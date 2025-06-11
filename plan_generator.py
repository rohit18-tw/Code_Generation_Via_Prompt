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

        # Create messages for LLM
        messages = [
            SystemMessage(content=system_prompt),
            HumanMessage(content=human_prompt)
        ]

        # Log the interaction
        self.logger.log_workflow("🎯 Sending plan generation request to Claude...")

        response = self.llm.invoke(messages)

        # Log the LLM interaction
        response_file = self.logger.log_llm_interaction(
            step="plan_generation",
            prompt=human_prompt,
            response=response.content,
            model=self.config.DEFAULT_MODEL,
            metadata={
                "input_type": processed_input["type"],
                "source": processed_input.get("metadata", {}).get("source", "unknown"),
                "prompt_length": len(human_prompt),
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
                "path": "file path",
                "purpose": "file purpose",
                "priority": "high|medium|low",
                "dependencies": ["dependencies"]
            }
        ]
    },
    "code_generation": {
        "order": ["generation order"],
        "file_templates": {
            "type": "template"
        },
        "implementation_strategy": "file_by_file|batch|incremental",
        "validation_rules": ["validation rules"]
    },
    "dependencies": {
        "runtime": ["dependencies"],
        "development": ["dev tools"],
        "build": ["build tools"]
    }
}
```

**CRITICAL INSTRUCTIONS**:
1. **Auto-detect everything**: Analyze the requirements to determine the appropriate language, framework, file types, and structure
2. **Use language/framework conventions**: Apply your knowledge of software architecture to determine the most appropriate file types, directory structures, and naming conventions for the detected language and framework
3. **Framework-appropriate structure**: Generate directory structures that follow industry best practices and framework conventions
4. **Language-appropriate dependencies**: Use the correct package managers, dependencies, and build tools for the detected language/framework ecosystem
5. **Realistic generation order**: Order files based on actual dependency requirements and compilation/build dependencies
6. **Apply architectural patterns**: Use appropriate design patterns and architectural styles for the detected technology stack

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

        prompt += "\nPlease provide a comprehensive development plan in the specified JSON format."
        prompt += "\n\nCRITICAL REQUIREMENTS:"
        prompt += "\n1. **Extract all requirements**: Identify every requirement, preference, and guideline from the provided content"
        prompt += "\n2. **Use your expertise**: Apply software engineering best practices and architectural knowledge to determine appropriate structure"
        prompt += "\n3. **Be comprehensive**: Consider both explicit requirements and implied best practices for the chosen technology stack"
        prompt += "\n4. Return ONLY valid JSON"

        return prompt

    def _validate_and_structure_plan(self, plan_data: Dict[str, Any]) -> Dict[str, Any]:
        """Minimal validation - only check basic completeness and let Claude's structure drive the rest."""

        # Basic completeness checks
        validation_issues = []

        # Check if plan has some basic structure
        if not plan_data:
            validation_issues.append("Plan is empty")
            return self._create_fallback_plan("Empty plan data")

        # Check for some form of file structure (flexible field names)
        files_found = False
        structure_keys = ["structure", "files", "components", "modules"]  # Common possible names
        for key in structure_keys:
            if key in plan_data:
                structure_data = plan_data[key]
                if isinstance(structure_data, dict):
                    # Look for files in various possible field names
                    file_keys = ["files", "components", "modules", "items"]
                    for file_key in file_keys:
                        if file_key in structure_data and structure_data[file_key]:
                            files_found = True
                            break
                elif isinstance(structure_data, list) and structure_data:
                    files_found = True
                break

        if not files_found:
            validation_issues.append("No files or components found in plan")

        # Add minimal metadata (no complex scoring)
        plan_data["metadata"] = {
            "generated_by": "claude_plan_generator",
            "validation_approach": "minimal",
            "validation_issues": validation_issues,
            "has_files": files_found
        }

        # Log validation results
        if validation_issues:
            for issue in validation_issues:
                self.logger.log_workflow(f"⚠️ Validation issue: {issue}")
        else:
            self.logger.log_workflow("✅ Plan passed minimal validation")

        return plan_data

    def _create_fallback_plan(self, error_reason: str) -> Dict[str, Any]:
        """Create a minimal fallback plan when validation fails."""
        return {
            "error": error_reason,
            "fallback": True,
            "metadata": {
                "generated_by": "claude_plan_generator",
                "validation_approach": "minimal",
                "validation_issues": [error_reason],
                "has_files": False
            }
        }

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

        # If no valid JSON found, return minimal fallback
        return self._create_fallback_plan("Could not parse structured plan from response")

    def format_plan_summary(self, plan: Dict[str, Any]) -> str:
        """Format the plan into a readable summary - adaptive to any plan structure."""

        # Handle fallback plans
        if plan.get("fallback") or plan.get("error"):
            return f"⚠️ **Plan Generation Issue**: {plan.get('error', 'Unknown error')}"

        # Try to extract project info from various possible structures
        project = plan.get("project", plan.get("application", plan.get("software", {})))

        # Try to find files in various possible locations
        files = []
        structure = plan.get("structure", plan.get("files", plan.get("components", {})))
        if isinstance(structure, dict):
            files = structure.get("files", structure.get("components", structure.get("modules", [])))
        elif isinstance(structure, list):
            files = structure

        # Try to find code generation info
        code_gen = plan.get("code_generation", plan.get("implementation", plan.get("build", {})))

        summary = f"""
🎯 **Project Plan Generated**

**Title**: {project.get('title', project.get('name', 'Generated Project'))}
**Language**: {project.get('language', project.get('tech', 'Not specified'))}
**Framework**: {project.get('framework', project.get('stack', 'Not specified'))}
**Type**: {project.get('type', project.get('category', 'Unknown'))}

**Files to Create**: {len(files)} files
**Strategy**: {code_gen.get('implementation_strategy', code_gen.get('approach', 'Adaptive'))}
**Generation Order**: {' → '.join(code_gen.get('order', [])[:5])}{'...' if len(code_gen.get('order', [])) > 5 else ''}

Ready for code generation!
"""
        return summary.strip()

    def generate_tree_structure(self, plan: Dict[str, Any]) -> str:
        """Generate a tree-like visualization of the project structure - adaptive to any plan format."""
        from pathlib import Path

        # Handle fallback plans
        if plan.get("fallback") or plan.get("error"):
            return f"⚠️ No structure available: {plan.get('error', 'Plan generation failed')}"

        # Try to find structure in various possible locations
        structure = plan.get("structure", plan.get("files", plan.get("components", {})))

        directories = []
        files = []

        if isinstance(structure, dict):
            directories = structure.get("directories", structure.get("folders", []))
            files = structure.get("files", structure.get("components", structure.get("modules", [])))
        elif isinstance(structure, list):
            files = structure

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

        # Add all files - handle different possible file formats
        for file_info in files:
            # Try to extract file path from various possible field names
            file_path = None
            if isinstance(file_info, dict):
                file_path = file_info.get("path", file_info.get("file", file_info.get("name", file_info.get("location"))))
            elif isinstance(file_info, str):
                file_path = file_info

            if not file_path:
                continue  # Skip files without identifiable paths

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
        """Analyze the plan structure for code generation insights - adaptive to any format."""

        # Handle fallback plans
        if plan.get("fallback") or plan.get("error"):
            return {
                "total_files": 0,
                "files_by_type": {},
                "files_by_priority": {"high": [], "medium": [], "low": []},
                "generation_order": [],
                "ordered_files": [],
                "implementation_strategy": "unknown",
                "validation_rules": [],
                "file_templates": {},
                "error": plan.get("error", "Plan generation failed")
            }

        # Try to find code generation info in various locations
        code_gen = plan.get("code_generation", plan.get("implementation", plan.get("build", {})))

        # Try to find files in various locations
        structure = plan.get("structure", plan.get("files", plan.get("components", {})))
        files = []
        if isinstance(structure, dict):
            files = structure.get("files", structure.get("components", structure.get("modules", [])))
        elif isinstance(structure, list):
            files = structure

        # Group files by type - handle different file formats
        files_by_type = {}
        files_by_priority = {"high": [], "medium": [], "low": []}

        for file_info in files:
            # Handle both dict and string file formats
            if isinstance(file_info, dict):
                file_type = file_info.get("type", file_info.get("category", file_info.get("kind", "unknown")))
                priority = file_info.get("priority", file_info.get("importance", "medium"))
            else:
                file_type = "unknown"
                priority = "medium"

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
