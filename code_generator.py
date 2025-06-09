"""Core code generation system using CTX and MCP for intelligent file generation."""

import json
from typing import Dict, List, Any, Optional, Tuple
from dataclasses import dataclass
from pathlib import Path

from langchain_anthropic import ChatAnthropic
from langchain.schema import HumanMessage, SystemMessage

from config import Config
from response_logger import get_logger
from ctx_manager import ContextManager, GenerationContext
from mcp_integration import MCPIntegration


@dataclass
class GenerationChunk:
    """Represents a chunk of files to generate together."""
    chunk_id: str
    files: List[Dict[str, Any]]
    dependencies: List[str]
    estimated_tokens: int
    priority: int


@dataclass
class GenerationResult:
    """Result of code generation for a single file."""
    file_path: str
    content: str
    success: bool
    errors: List[str]
    warnings: List[str]
    tokens_used: int
    generation_time: float


class CodeGenerator:
    """Intelligent code generator using CTX and MCP."""

    def __init__(self, logger=None):
        self.config = Config()
        self.logger = logger or get_logger()

        # Initialize LLM
        self.llm = ChatAnthropic(
            anthropic_api_key=self.config.ANTHROPIC_API_KEY,
            model=self.config.DEFAULT_MODEL,
            max_tokens=self.config.MAX_TOKENS,
            temperature=self.config.TEMPERATURE
        )

        # Initialize CTX and MCP
        self.ctx_manager = ContextManager(logger)
        self.mcp_integration = MCPIntegration(logger)

        # Initialize file creator for real-time file creation
        from file_creator import FileCreator
        self.file_creator = FileCreator(logger)

        # Generation state
        self.generated_files: Dict[str, str] = {}
        self.generation_history: List[GenerationResult] = []

        self.logger.log_workflow("🚀 Code Generator initialized with CTX and MCP integration")

    def generate_code_from_plan(self, development_plan: Dict[str, Any]) -> Dict[str, Any]:
        """
        Generate complete codebase from development plan.

        Args:
            development_plan: Enhanced development plan from planning phase

        Returns:
            Generation results and statistics
        """
        self.logger.log_workflow("🎯 Starting code generation from enhanced plan")

        # Create output directory structure
        self._setup_output_structure(development_plan)

        # Create generation chunks
        chunks = self._create_generation_chunks(development_plan)

        # Generate code chunk by chunk
        chunk_results = []
        for chunk in chunks:
            result = self._generate_chunk(chunk, development_plan)
            chunk_results.append(result)

            # Update generated files for next chunk context
            for file_result in result.get("files", []):
                if file_result["success"]:
                    self.generated_files[file_result["file_path"]] = file_result["content"]

        # Compile final results
        final_result = self._compile_generation_results(chunk_results, development_plan)

        self.logger.log_workflow(
            f"✅ Code generation complete: {final_result['total_files']} files, "
            f"{final_result['success_rate']:.1%} success rate"
        )

        return final_result

    def _setup_output_structure(self, development_plan: Dict[str, Any]):
        """Setup output directory structure."""
        structure = development_plan.get("structure", {})
        directories = structure.get("directories", [])

        if directories:
            self.mcp_integration.create_directory_structure(directories)

        self.logger.log_workflow(f"📁 Created {len(directories)} directories")

    def _create_generation_chunks(self, development_plan: Dict[str, Any]) -> List[GenerationChunk]:
        """Create intelligent generation chunks based on dependencies and complexity."""
        files = development_plan.get("structure", {}).get("files", [])
        code_gen = development_plan.get("code_generation", {})
        generation_order = code_gen.get("order", [])

        chunks = []
        chunk_id = 1

        # Group files by type according to generation order
        files_by_type = {}
        for file_info in files:
            file_type = file_info.get("type", "unknown")
            if file_type not in files_by_type:
                files_by_type[file_type] = []
            files_by_type[file_type].append(file_info)

        # Create chunks following the generation order
        for file_type in generation_order:
            if file_type in files_by_type:
                type_files = files_by_type[file_type]

                # Split large types into multiple chunks
                while type_files:
                    chunk_files = []
                    estimated_tokens = 0

                    # Add files to chunk until we hit limits
                    while (type_files and
                           len(chunk_files) < self.config.MAX_FILES_PER_CHUNK and
                           estimated_tokens < self.config.CHUNK_TOKEN_LIMIT * 0.7):  # Leave room for context

                        file_info = type_files.pop(0)
                        chunk_files.append(file_info)

                        # Estimate tokens for this file (rough estimate)
                        estimated_tokens += self._estimate_file_tokens(file_info, development_plan)

                    if chunk_files:
                        # Determine chunk dependencies
                        chunk_deps = set()
                        for file_info in chunk_files:
                            chunk_deps.update(file_info.get("dependencies", []))

                        chunk = GenerationChunk(
                            chunk_id=f"chunk_{chunk_id:02d}_{file_type}",
                            files=chunk_files,
                            dependencies=list(chunk_deps),
                            estimated_tokens=estimated_tokens,
                            priority=generation_order.index(file_type) if file_type in generation_order else 999
                        )
                        chunks.append(chunk)
                        chunk_id += 1

        # Add any remaining files not in the generation order
        remaining_files = []
        for file_type, type_files in files_by_type.items():
            if file_type not in generation_order:
                remaining_files.extend(type_files)

        if remaining_files:
            chunk = GenerationChunk(
                chunk_id=f"chunk_{chunk_id:02d}_remaining",
                files=remaining_files,
                dependencies=[],
                estimated_tokens=sum(self._estimate_file_tokens(f, development_plan) for f in remaining_files),
                priority=1000
            )
            chunks.append(chunk)

        self.logger.log_workflow(f"📦 Created {len(chunks)} generation chunks")
        return chunks

    def _estimate_file_tokens(self, file_info: Dict[str, Any], development_plan: Dict[str, Any]) -> int:
        """Estimate tokens needed to generate a file."""
        file_type = file_info.get("type", "unknown")
        language = development_plan.get("project", {}).get("language", "unknown")

        # Base estimates by file type and language
        base_estimates = {
            "config": 500,
            "entity": 800,
            "service": 1500,
            "controller": 1200,
            "repository": 1000,
            "test": 1000,
            "main": 600,
            "utility": 700
        }

        # Language multipliers
        language_multipliers = {
            "java": 1.3,  # Java tends to be more verbose
            "go": 1.0,
            "python": 0.8,
            "javascript": 0.9
        }

        base_tokens = base_estimates.get(file_type, 800)
        multiplier = language_multipliers.get(language, 1.0)

        return int(base_tokens * multiplier)

    def _generate_chunk(self, chunk: GenerationChunk, development_plan: Dict[str, Any]) -> Dict[str, Any]:
        """Generate code for a specific chunk."""
        self.logger.log_workflow(f"🔨 Generating chunk: {chunk.chunk_id} ({len(chunk.files)} files)")

        chunk_results = {
            "chunk_id": chunk.chunk_id,
            "files": [],
            "success_count": 0,
            "total_files": len(chunk.files),
            "errors": []
        }

        # Generate each file in the chunk
        for file_info in chunk.files:
            try:
                result = self._generate_single_file(file_info, development_plan, chunk)
                chunk_results["files"].append(result.__dict__)

                if result.success:
                    chunk_results["success_count"] += 1
                    # Add to generated files for next file's context
                    self.generated_files[file_info.get('path', '')] = result.content
                    # Learn from successful generation
                    self.ctx_manager.learn_from_generation(file_info, result.content, True)

                    # 🔄 Update MCP context with newly created file
                    self.logger.log_workflow(f"🔄 Updated context with: {file_info.get('path', '')}")
                else:
                    chunk_results["errors"].extend(result.errors)

            except Exception as e:
                error_msg = f"Failed to generate {file_info.get('path', 'unknown')}: {str(e)}"
                self.logger.log_error(error_msg, "code_generation")
                chunk_results["errors"].append(error_msg)

        success_rate = chunk_results["success_count"] / chunk_results["total_files"]
        self.logger.log_workflow(
            f"✅ Chunk {chunk.chunk_id} complete: {success_rate:.1%} success rate"
        )

        return chunk_results

    def _generate_single_file(self, file_info: Dict[str, Any],
                             development_plan: Dict[str, Any],
                             chunk: GenerationChunk) -> GenerationResult:
        """Generate code for a single file."""
        import time
        start_time = time.time()

        file_path = file_info.get("path", "")
        self.logger.log_workflow(f"📝 Generating: {file_path}")

        try:
            # Build optimized context using CTX
            context = self.ctx_manager.build_generation_context(
                file_info, development_plan, self.generated_files
            )

            # Get MCP file system context (real-time scan)
            fs_context = self.mcp_integration.get_file_system_context(file_info)

            # 🔍 MCP: Read any existing related files for enhanced context
            related_files = fs_context.get("related_files", {})
            if related_files:
                self.logger.log_workflow(f"🔗 MCP found {len(related_files)} related files for context")

            # Create generation prompt
            prompt = self._create_generation_prompt(context, fs_context, chunk)

            # Generate code
            response = self.llm.invoke([
                SystemMessage(content=self._create_system_prompt(development_plan)),
                HumanMessage(content=prompt)
            ])

            generated_code = response.content.strip()

            # Extract code from markdown if present
            generated_code = self._extract_code_from_response(generated_code)

            # Validate generated code
            language = development_plan.get("project", {}).get("language", "unknown")
            validation = self.mcp_integration.validate_code(generated_code, language, file_path)

            # Log the generation
            response_file = self.logger.log_llm_interaction(
                step=f"code_generation_{chunk.chunk_id}",
                prompt=prompt,
                response=generated_code,
                model=self.config.DEFAULT_MODEL,
                metadata={
                    "file_path": file_path,
                    "file_type": file_info.get("type", "unknown"),
                    "context_tokens": context.total_tokens,
                    "validation_passed": validation.is_valid
                }
            )

            generation_time = time.time() - start_time

            # 🚀 REAL-TIME FILE CREATION: Create file immediately after generation
            if validation.is_valid:
                self.logger.log_workflow(f"📁 Creating file immediately: {file_path}")
                creation_result = self.file_creator.create_single_file(
                    file_path, generated_code, language
                )

                if creation_result.success:
                    self.logger.log_workflow(f"✅ File created: {file_path} ({creation_result.file_size:,} bytes)")
                    # Update MCP's understanding of the codebase
                    self.mcp_integration.scan_existing_codebase()
                else:
                    self.logger.log_workflow(f"❌ File creation failed: {file_path}")
                    for error in creation_result.errors:
                        self.logger.log_workflow(f"   Error: {error}")
            else:
                self.logger.log_workflow(f"⚠️ Skipping file creation due to validation errors: {file_path}")

            return GenerationResult(
                file_path=file_path,
                content=generated_code,
                success=validation.is_valid,
                errors=validation.errors,
                warnings=validation.warnings,
                tokens_used=len(self.ctx_manager.encoding.encode(generated_code)),
                generation_time=generation_time
            )

        except Exception as e:
            generation_time = time.time() - start_time
            error_msg = f"Generation failed: {str(e)}"
            self.logger.log_error(error_msg, "single_file_generation")

            return GenerationResult(
                file_path=file_path,
                content="",
                success=False,
                errors=[error_msg],
                warnings=[],
                tokens_used=0,
                generation_time=generation_time
            )

    def _create_system_prompt(self, development_plan: Dict[str, Any]) -> str:
        """Create system prompt for code generation."""
        project = development_plan.get("project", {})
        constraints = development_plan.get("constraints", {})
        language = project.get("language", "unknown")
        framework = project.get("framework", "unknown")

        prompt = f"""You are an expert {language} developer specializing in {framework} applications.

Generate complete, production-ready code based on the provided context and requirements.

**Key Requirements:**
- Write complete implementations, no placeholders or TODOs
- Follow {language} best practices and conventions
- Include proper error handling and logging
- Add comprehensive comments and documentation
- Ensure type safety and validation
- Follow the architectural patterns specified
- Include all necessary imports and dependencies"""

        # Add constraints if they exist
        if constraints:
            prompt += "\n\n**CRITICAL CONSTRAINTS - MUST FOLLOW:**"

            tech_restrictions = constraints.get("technology_restrictions", [])
            if tech_restrictions:
                prompt += "\n- Technology Restrictions:"
                for restriction in tech_restrictions:
                    prompt += f"\n  • {restriction}"

            coding_standards = constraints.get("coding_standards", [])
            if coding_standards:
                prompt += "\n- Coding Standards:"
                for standard in coding_standards:
                    prompt += f"\n  • {standard}"

            testing_requirements = constraints.get("testing_requirements", [])
            if testing_requirements:
                prompt += "\n- Testing Requirements:"
                for requirement in testing_requirements:
                    prompt += f"\n  • {requirement}"

            best_practices = constraints.get("best_practices", [])
            if best_practices:
                prompt += "\n- Best Practices:"
                for practice in best_practices:
                    prompt += f"\n  • {practice}"

            other_constraints = constraints.get("other_constraints", [])
            if other_constraints:
                prompt += "\n- Other Constraints:"
                for constraint in other_constraints:
                    prompt += f"\n  • {constraint}"

        prompt += """

**Code Quality Standards:**
- Clean, readable, and maintainable code
- Proper separation of concerns
- Consistent naming conventions
- Comprehensive error handling
- Security best practices
- Performance considerations

**Output Format:**
Provide ONLY the complete code for the requested file. Do not include explanations, markdown formatting, or additional text.
"""
        return prompt

    def _create_generation_prompt(self, context: GenerationContext,
                                 fs_context: Dict[str, Any],
                                 chunk: GenerationChunk) -> str:
        """Create the generation prompt with optimized context."""
        file_info = context.target_file
        project_context = context.project_context

        prompt = f"""Generate complete code for: {file_info['path']}

**File Details:**
- Purpose: {file_info.get('purpose', 'Not specified')}
- Type: {file_info.get('type', 'unknown')}
- Priority: {file_info.get('priority', 'medium')}
- Language: {project_context['language']}
- Framework: {project_context['framework']}

**Project Context:**
- Project Type: {project_context['type']}
- Dependencies: {', '.join(project_context.get('dependencies', {}).get('runtime', [])[:5])}

**Dependency Context:**
"""

        # Add optimized dependency context
        for item in context.dependency_context:
            prompt += f"\n// From {item.source_file} ({item.item_type}):\n{item.content}\n"

        # Add file system context
        if fs_context.get("related_files"):
            prompt += "\n**Related Files in Project:**\n"
            for rel_path, rel_content in list(fs_context["related_files"].items())[:3]:
                # Include just the signatures/interfaces
                lines = rel_content.split('\n')[:10]
                prompt += f"\n// {rel_path} (excerpt):\n" + '\n'.join(lines) + "\n"

        # Add pattern context
        if context.pattern_context:
            prompt += "\n**Patterns to Follow:**\n"
            for pattern_type, patterns in context.pattern_context.items():
                if patterns:
                    prompt += f"- {pattern_type}: {patterns}\n"

        # Add specific requirements
        prompt += f"""

**Specific Requirements:**
- Implement all methods and functionality completely
- Follow the file template: {project_context.get('file_templates', {}).get(file_info.get('type', ''), 'Standard implementation')}
- Include proper error handling and validation
- Add comprehensive logging with PII masking where applicable
- Ensure thread safety and performance optimization
- Include all necessary imports and dependencies

Generate the complete, production-ready code now:"""

        return prompt

    def _extract_code_from_response(self, response: str) -> str:
        """Extract code content from markdown-wrapped response."""
        import re

        # Look for code blocks with language specification
        code_block_pattern = r'```(?:python|go|java|javascript|typescript|json|yaml|sql|bash|sh|dockerfile)?\s*\n(.*?)\n```'
        matches = re.findall(code_block_pattern, response, re.DOTALL | re.IGNORECASE)

        if matches:
            # If we found code blocks, return the first one
            return matches[0].strip()

        # If no code blocks found, check if the entire response looks like code
        lines = response.split('\n')

        # Remove common non-code prefixes
        cleaned_lines = []
        for line in lines:
            # Skip lines that look like markdown or comments about the response
            if (line.strip().startswith('#') and
                any(keyword in line.lower() for keyword in ['llm response', 'timestamp', 'model', 'interaction'])):
                continue
            if line.strip().startswith('```'):
                continue
            cleaned_lines.append(line)

        # If we removed some lines, return the cleaned version
        if len(cleaned_lines) < len(lines):
            return '\n'.join(cleaned_lines).strip()

        # Otherwise return the original response
        return response.strip()

    def _compile_generation_results(self, chunk_results: List[Dict[str, Any]],
                                   development_plan: Dict[str, Any]) -> Dict[str, Any]:
        """Compile final generation results."""
        total_files = sum(chunk["total_files"] for chunk in chunk_results)
        total_success = sum(chunk["success_count"] for chunk in chunk_results)
        all_errors = []

        for chunk in chunk_results:
            all_errors.extend(chunk.get("errors", []))

        # Calculate statistics
        success_rate = total_success / total_files if total_files > 0 else 0
        total_tokens = sum(result.tokens_used for result in self.generation_history)

        return {
            "success": success_rate > 0.8,  # Consider successful if >80% files generated
            "total_files": total_files,
            "successful_files": total_success,
            "failed_files": total_files - total_success,
            "success_rate": success_rate,
            "total_tokens_used": total_tokens,
            "chunks_generated": len(chunk_results),
            "errors": all_errors,
            "generated_files": list(self.generated_files.keys()),
            "output_directory": str(self.mcp_integration.output_dir),
            "chunk_results": chunk_results
        }
