"""Plan generation module using Claude to analyze requirements and create development plans."""

from typing import Dict, Any, List
from langchain_anthropic import ChatAnthropic
from langchain.schema import HumanMessage, SystemMessage
import json

from config import Config
from response_logger import get_logger


class PlanGenerator:
    """Generates development plans using Claude based on processed input."""

    def __init__(self, logger=None):
        self.config = Config()
        self.llm = ChatAnthropic(
            anthropic_api_key=self.config.ANTHROPIC_API_KEY,
            model=self.config.DEFAULT_MODEL,
            max_tokens=self.config.MAX_TOKENS,
            temperature=self.config.TEMPERATURE
        )
        self.logger = logger or get_logger()

    def generate_plan(self, processed_input: Dict[str, Any]) -> Dict[str, Any]:
        """
        Generate a development plan based on processed input.

        Args:
            processed_input: Output from PromptProcessor

        Returns:
            Structured development plan
        """
        # Create the system prompt for plan generation
        system_prompt = self._create_system_prompt()

        # Create the human prompt with the processed input
        human_prompt = self._create_human_prompt(processed_input)

        # Handle image content if present
        if processed_input["type"] == "image":
            # For image inputs, we need to use the vision capabilities
            messages = [
                SystemMessage(content=system_prompt),
                HumanMessage(content=[
                    {"type": "text", "text": human_prompt},
                    {
                        "type": "image_url",
                        "image_url": {
                            "url": f"data:image/jpeg;base64,{processed_input['content']}"
                        }
                    }
                ])
            ]
        else:
            messages = [
                SystemMessage(content=system_prompt),
                HumanMessage(content=human_prompt)
            ]

        # Log the interaction
        self.logger.log_workflow("🎯 Sending plan generation request to Claude...")

        response = self.llm.invoke(messages)

        # Log the LLM interaction
        prompt_text = human_prompt if processed_input["type"] != "image" else f"{human_prompt}\n[Image content included]"
        response_file = self.logger.log_llm_interaction(
            step="plan_generation",
            prompt=prompt_text,
            response=response.content,
            model=self.config.DEFAULT_MODEL,
            metadata={
                "input_type": processed_input["type"],
                "source": processed_input.get("metadata", {}).get("source", "unknown"),
                "prompt_length": len(prompt_text),
                "response_length": len(response.content)
            }
        )

        self.logger.log_workflow(f"📝 Plan generation response saved to: {response_file}")

        # Check if response seems truncated
        response_text = response.content.strip()
        if not response_text.endswith('}') and not response_text.endswith(']'):
            self.logger.log_error("Response appears to be truncated - increase MAX_TOKENS in config", "plan_generation")

        # Parse the response into a structured plan
        try:
            plan_data = json.loads(response.content)
            structured_plan = self._validate_and_structure_plan(plan_data)
            self.logger.log_success("Plan successfully parsed and structured", "plan_generation")
            return structured_plan
        except json.JSONDecodeError as e:
            self.logger.log_error(f"JSON parsing failed: {str(e)}", "plan_generation")
            self.logger.log_workflow("⚠️ Failed to parse JSON, attempting text extraction...", "WARNING")
            # Fallback: try to extract JSON from the response
            fallback_plan = self._extract_plan_from_text(response.content)
            if "error" in fallback_plan:
                self.logger.log_error(f"Plan parsing failed: {fallback_plan['error']}", "plan_generation")
            return fallback_plan

    def _create_system_prompt(self) -> str:
        """Create the system prompt for plan generation."""

        base_prompt = """You are an expert software architect. Analyze the requirements and create a clean, focused development plan."""
        return base_prompt + """

**Output Format**: You MUST respond with a valid JSON object with this structure:

```json
{
    "project": {
        "title": "Project Title",
        "description": "Brief description",
        "language": "auto-detect from requirements (python|java|javascript|typescript|dart|go|csharp|php|ruby|swift|kotlin)",
        "type": "auto-detect from requirements (web_app|mobile_app|api|cli_tool|library|desktop_app|microservice)",
        "framework": "auto-detect based on language and requirements (spring-boot|react|express|fastapi|flutter|django|laravel|rails|dotnet|nextjs|vue|angular)"
    },
    "structure": {
        "directories": ["auto-generate based on language/framework conventions"],
        "files": [
            {
                "path": "auto-generate appropriate paths",
                "purpose": "Clear description of file purpose",
                "type": "auto-detect appropriate types based on language/framework",
                "priority": "high|medium|low",
                "dependencies": ["list of file dependencies"],
                "relevant_constraints": {
                    "technology_restrictions": ["extracted from requirements"],
                    "architectural_requirements": ["extracted from requirements"],
                    "business_logic_requirements": ["extracted from requirements"]
                }
            }
        ]
    },
    "code_generation": {
        "order": ["auto-determine optimal generation order based on dependencies"],
        "file_templates": {
            "auto-generate": "templates appropriate for the detected language/framework"
        },
        "implementation_strategy": "file_by_file|batch|incremental",
        "validation_rules": ["language-appropriate validation rules"]
    },
    "dependencies": {
        "runtime": ["language/framework appropriate dependencies"],
        "development": ["language/framework appropriate dev tools"],
        "build": ["appropriate build tools for the language/framework"]
    },
    "steps": [
        {
            "step": 1,
            "title": "Descriptive step title",
            "files": ["files for this step"],
            "file_types": ["types for this step"],
            "estimated_time": "realistic time estimate",
            "validation": ["appropriate validation for this step"]
        }
    ],
    "constraints": {
        "technology_restrictions": ["extracted from requirements"],
        "architectural_requirements": ["extracted from requirements"],
        "business_logic_requirements": ["extracted from requirements"],
        "security_requirements": ["extracted from requirements"],
        "testing_requirements": ["extracted from requirements"],
        "performance_requirements": ["extracted from requirements"],
        "coding_standards": ["language/framework appropriate standards"],
        "best_practices": ["language/framework appropriate practices"]
    }
}
```

**CRITICAL INSTRUCTIONS**:
1. **Auto-detect everything**: Analyze the requirements to determine the appropriate language, framework, file types, and structure
2. **Language-specific file types**: Use appropriate file types for the detected language:
   - Java: main|config|entity|service|controller|repository|test
   - Python: main|config|model|service|view|util|test
   - JavaScript/TypeScript: main|component|page|hook|service|util|test
   - Dart/Flutter: main|widget|screen|model|service|provider|util|test
   - Go: main|handler|service|model|middleware|util|test
   - C#: main|controller|service|model|repository|config|test
3. **Framework-appropriate structure**: Generate directory structures that follow the framework's conventions
4. **Language-appropriate dependencies**: Use the correct package managers and dependencies for the detected language
5. **Realistic generation order**: Order files based on actual dependency requirements
6. **Extract ALL constraints**: Pull every constraint, requirement, and restriction from the user's input

Focus on essential information only. Generate complete code without skipping implementations."""

    def _create_human_prompt(self, processed_input: Dict[str, Any]) -> str:
        """Create the human prompt with processed input data."""
        input_type = processed_input["type"]
        content = processed_input["content"]
        metadata = processed_input.get("metadata", {})

        prompt = f"""Please analyze the following requirements and create a detailed development plan.

**Input Type**: {input_type}
**Source**: {metadata.get('source', 'direct input')}

**Requirements/Content**:
{content}

"""



        # Add additional context for specific input types
        if input_type == "folder":
            files_info = processed_input.get("files", [])
            prompt += f"\n**Additional Context**:\n"
            prompt += f"- Total files processed: {len(files_info)}\n"
            prompt += f"- File types found: {', '.join(set(f['metadata'].get('extension', 'unknown') for f in files_info))}\n"

        elif input_type == "image":
            prompt += f"\n**Image Details**:\n"
            prompt += f"- Format: {metadata.get('format', 'unknown')}\n"
            prompt += f"- Dimensions: {metadata.get('dimensions', 'unknown')}\n"

        prompt += "\nPlease provide a comprehensive development plan in the specified JSON format."
        prompt += "\n\nCRITICAL REQUIREMENTS:"
        prompt += "\n1. Extract ALL constraints from requirements into a global 'constraints' section"
        prompt += "\n2. For EVERY file in the structure, add a 'relevant_constraints' field with ONLY applicable constraints"
        prompt += "\n3. Apply intelligent filtering:"
        prompt += "\n   - Mock services: NO database/PostgreSQL constraints, YES mock-specific constraints"
        prompt += "\n   - Main services: YES database constraints, NO mock-specific constraints"
        prompt += "\n   - Test files: YES testing constraints, NO production constraints"
        prompt += "\n4. Technology restrictions (No Lombok, No Mockito) apply to ALL relevant files"
        prompt += "\n5. Return ONLY valid JSON"
        prompt += "\n\nEXAMPLE: {\"path\": \"MockService.java\", \"relevant_constraints\": {\"technology\": [\"No Lombok\"], \"mock_specific\": [\"Use in-memory storage\"]}}"

        return prompt

    def _validate_and_structure_plan(self, plan_data: Dict[str, Any]) -> Dict[str, Any]:
        """Validate and ensure the plan has all required fields with quality scoring."""
        required_sections = [
            "project", "structure", "code_generation", "dependencies", "steps", "constraints"
        ]

        # Ensure all required sections exist
        for section in required_sections:
            if section not in plan_data:
                plan_data[section] = {}

        # Enhanced constraint validation
        plan_data["constraints"] = self._validate_constraints_section(plan_data.get("constraints", {}))

        # Validate project structure
        validation_results = self._validate_project_structure(plan_data)

        # Calculate plan quality score
        quality_score = self._calculate_plan_quality_score(plan_data, validation_results)

        # Add enhanced metadata
        plan_data["metadata"] = {
            "generated_by": "claude_plan_generator",
            "total_steps": len(plan_data.get("steps", [])),
            "total_files": len(plan_data.get("structure", {}).get("files", [])),
            "quality_score": quality_score,
            "validation_results": validation_results,
            "completeness_score": self._calculate_completeness_score(plan_data),
            "constraint_coverage": self._calculate_constraint_coverage(plan_data)
        }

        return plan_data

    def _validate_constraints_section(self, constraints: Dict[str, Any]) -> Dict[str, Any]:
        """Enhanced constraint validation with categorization."""
        validated_constraints = {
            "technology_restrictions": constraints.get("technology_restrictions", []),
            "architectural_requirements": constraints.get("architectural_requirements", []),
            "business_logic_requirements": constraints.get("business_logic_requirements", []),
            "security_requirements": constraints.get("security_requirements", []),
            "testing_requirements": constraints.get("testing_requirements", []),
            "performance_requirements": constraints.get("performance_requirements", []),
            "coding_standards": constraints.get("coding_standards", []),
            "best_practices": constraints.get("best_practices", []),
            "other_constraints": constraints.get("other_constraints", [])
        }

        # Validate constraint format and content
        for category, constraint_list in validated_constraints.items():
            if not isinstance(constraint_list, list):
                validated_constraints[category] = []
                self.logger.log_workflow(f"⚠️ Fixed invalid constraint format for {category}")

        return validated_constraints

    def _validate_project_structure(self, plan_data: Dict[str, Any]) -> Dict[str, Any]:
        """Validate project structure and dependencies."""
        validation_results = {
            "structure_issues": [],
            "dependency_issues": [],
            "constraint_issues": [],
            "completeness_issues": []
        }

        structure = plan_data.get("structure", {})
        files = structure.get("files", [])

        # Check for missing essential files
        project_type = plan_data.get("project", {}).get("type", "")
        language = plan_data.get("project", {}).get("language", "")

        essential_files = self._get_essential_files(project_type, language)
        existing_file_types = {f.get("type", "") for f in files}

        for essential_type in essential_files:
            if essential_type not in existing_file_types:
                validation_results["completeness_issues"].append(f"Missing essential file type: {essential_type}")

        # Validate file dependencies
        for file_info in files:
            file_deps = file_info.get("dependencies", [])
            for dep in file_deps:
                if not any(dep in f.get("path", "") for f in files):
                    validation_results["dependency_issues"].append(f"Unresolved dependency: {dep} for {file_info.get('path', '')}")

        # Validate constraint application
        for file_info in files:
            relevant_constraints = file_info.get("relevant_constraints", {})
            if not relevant_constraints:
                validation_results["constraint_issues"].append(f"No constraints applied to: {file_info.get('path', '')}")

        return validation_results

    def _get_essential_files(self, project_type: str, language: str) -> List[str]:
        """Get essential file types for project type and language."""
        essential_files = {
            "web_app": {
                "java": ["config", "entity", "repository", "service", "controller", "test"],
                "python": ["config", "model", "service", "controller", "test"],
                "javascript": ["config", "model", "service", "controller", "test"]
            },
            "api": {
                "java": ["config", "entity", "repository", "service", "controller", "test"],
                "go": ["config", "model", "service", "handler", "test"],
                "python": ["config", "model", "service", "router", "test"]
            },
            "cli_tool": {
                "java": ["main", "service", "test"],
                "go": ["main", "service", "test"],
                "python": ["main", "service", "test"]
            }
        }

        return essential_files.get(project_type, {}).get(language, ["main", "test"])

    def _calculate_plan_quality_score(self, plan_data: Dict[str, Any], validation_results: Dict[str, Any]) -> float:
        """Calculate overall plan quality score (0.0 to 1.0)."""
        score = 1.0

        # Deduct for validation issues
        total_issues = sum(len(issues) for issues in validation_results.values())
        if total_issues > 0:
            score -= min(0.3, total_issues * 0.05)  # Max 30% deduction for issues

        # Check completeness
        structure = plan_data.get("structure", {})
        files = structure.get("files", [])

        if not files:
            score -= 0.4  # Major deduction for no files
        elif len(files) < 3:
            score -= 0.2  # Minor deduction for too few files

        # Check constraint coverage
        constraints = plan_data.get("constraints", {})
        constraint_count = sum(len(v) if isinstance(v, list) else 0 for v in constraints.values())
        if constraint_count == 0:
            score -= 0.2  # Deduction for no constraints

        # Check dependency mapping
        files_with_deps = sum(1 for f in files if f.get("dependencies"))
        if files_with_deps == 0 and len(files) > 1:
            score -= 0.1  # Minor deduction for no dependencies in multi-file project

        return max(0.0, min(1.0, score))

    def _calculate_completeness_score(self, plan_data: Dict[str, Any]) -> float:
        """Calculate plan completeness score."""
        required_fields = {
            "project": ["title", "language", "type", "framework"],
            "structure": ["directories", "files"],
            "code_generation": ["order", "file_templates"],
            "dependencies": ["runtime"],
            "constraints": []
        }

        total_fields = 0
        present_fields = 0

        for section, fields in required_fields.items():
            section_data = plan_data.get(section, {})
            total_fields += len(fields) if fields else 1

            if not fields:  # Just check if section exists
                present_fields += 1 if section_data else 0
            else:
                for field in fields:
                    if field in section_data and section_data[field]:
                        present_fields += 1

        return present_fields / total_fields if total_fields > 0 else 0.0

    def _calculate_constraint_coverage(self, plan_data: Dict[str, Any]) -> float:
        """Calculate how well constraints are applied to files."""
        files = plan_data.get("structure", {}).get("files", [])
        if not files:
            return 0.0

        files_with_constraints = sum(1 for f in files if f.get("relevant_constraints"))
        return files_with_constraints / len(files)

    def _extract_plan_from_text(self, text: str) -> Dict[str, Any]:
        """Fallback method to extract plan from text response."""
        # Try to find JSON in the text
        import re
        json_match = re.search(r'\{.*\}', text, re.DOTALL)

        if json_match:
            try:
                plan_data = json.loads(json_match.group())
                return self._validate_and_structure_plan(plan_data)
            except json.JSONDecodeError:
                pass

        # If no valid JSON found, create a basic plan structure
        return {
            "project": {
                "title": "Generated Project",
                "description": "Project based on provided requirements",
                "language": "python",
                "type": "other",
                "framework": "unknown"
            },
            "structure": {"directories": [], "files": []},
            "code_generation": {
                "order": ["main"],
                "file_templates": {},
                "implementation_strategy": "file_by_file",
                "validation_rules": ["syntax_check"]
            },
            "dependencies": {"runtime": [], "development": [], "build": []},
            "steps": [],
            "error": "Could not parse structured plan from response"
        }

    def format_plan_summary(self, plan: Dict[str, Any]) -> str:
        """Format the plan into a readable summary."""
        project = plan.get("project", {})
        steps = plan.get("steps", [])
        code_gen = plan.get("code_generation", {})

        summary = f"""
🎯 **Enhanced Project Plan Generated**

**Title**: {project.get('title', 'Unknown Project')}
**Language**: {project.get('language', 'Not specified')}
**Framework**: {project.get('framework', 'Not specified')}
**Type**: {project.get('type', 'Unknown')}

**Code Generation Strategy**: {code_gen.get('implementation_strategy', 'file_by_file')}
**Generation Order**: {' → '.join(code_gen.get('order', []))}
**Implementation Steps**: {len(steps)} steps planned
**Files to Create**: {plan.get('metadata', {}).get('total_files', 'Unknown')} files

Ready for enhanced code generation!
"""
        return summary.strip()

    def generate_tree_structure(self, plan: Dict[str, Any]) -> str:
        """Generate a tree-like visualization of the project structure."""
        from pathlib import Path

        structure = plan.get("structure", {})
        directories = structure.get("directories", [])
        files = structure.get("files", [])

        # Create a tree structure
        tree = {}

        # Add all directories
        for dir_path in directories:
            parts = dir_path.split("/")
            current = tree
            for part in parts:
                if part not in current:
                    current[part] = {}
                current = current[part]

        # Add all files
        for file_info in files:
            file_path = file_info["path"]
            parts = file_path.split("/")
            current = tree

            # Navigate to the directory
            for part in parts[:-1]:
                if part not in current:
                    current[part] = {}
                current = current[part]

            # Add the file (use None to indicate it's a file)
            current[parts[-1]] = None

        # Generate tree string
        project_name = plan.get("project", {}).get("title", "project").lower().replace(" ", "-")

        def build_tree_string(node, prefix="", is_last=True):
            lines = []
            items = sorted(node.items()) if isinstance(node, dict) else []

            for i, (name, child) in enumerate(items):
                is_last_item = i == len(items) - 1

                # Choose the appropriate tree characters
                if prefix == "":  # Root level
                    current_prefix = "├── " if not is_last_item else "└── "
                    next_prefix = "│   " if not is_last_item else "    "
                else:
                    current_prefix = prefix + ("├── " if not is_last_item else "└── ")
                    next_prefix = prefix + ("│   " if not is_last_item else "    ")

                if child is None:  # It's a file
                    lines.append(current_prefix + name)
                else:  # It's a directory
                    lines.append(current_prefix + name + "/")
                    if child:  # If directory has contents
                        lines.extend(build_tree_string(child, next_prefix, is_last_item))

            return lines

        tree_lines = [f"{project_name}/"]
        tree_lines.extend(build_tree_string(tree))

        return "\n".join(tree_lines)

    def analyze_code_generation_plan(self, plan: Dict[str, Any]) -> Dict[str, Any]:
        """Analyze the enhanced plan structure for code generation insights."""
        code_gen = plan.get("code_generation", {})
        structure = plan.get("structure", {})
        files = structure.get("files", [])

        # Group files by type
        files_by_type = {}
        files_by_priority = {"high": [], "medium": [], "low": []}

        for file_info in files:
            file_type = file_info.get("type", "unknown")
            priority = file_info.get("priority", "medium")

            if file_type not in files_by_type:
                files_by_type[file_type] = []
            files_by_type[file_type].append(file_info)
            files_by_priority[priority].append(file_info)

        # Calculate generation order based on dependencies
        generation_order = code_gen.get("order", [])
        ordered_files = []

        for file_type in generation_order:
            if file_type in files_by_type:
                ordered_files.extend(files_by_type[file_type])

        # Add any remaining files not in the order
        for file_type, file_list in files_by_type.items():
            if file_type not in generation_order:
                ordered_files.extend(file_list)

        return {
            "total_files": len(files),
            "files_by_type": files_by_type,
            "files_by_priority": files_by_priority,
            "generation_order": generation_order,
            "ordered_files": ordered_files,
            "implementation_strategy": code_gen.get("implementation_strategy", "file_by_file"),
            "validation_rules": code_gen.get("validation_rules", []),
            "file_templates": code_gen.get("file_templates", {})
        }
