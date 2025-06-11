"""Model Context Protocol (MCP) Integration for enhanced code generation capabilities."""

import json
import os
import subprocess
import tempfile
from typing import Dict, List, Any, Optional, Tuple
from pathlib import Path
from dataclasses import dataclass
import ast
import shutil

from config import Config
from response_logger import get_logger


@dataclass
class MCPTool:
    """Represents an MCP tool with its capabilities."""
    name: str
    description: str
    parameters: Dict[str, Any]
    enabled: bool = True


@dataclass
class FileSystemInfo:
    """Information about the file system structure."""
    directories: List[str]
    files: List[str]
    file_types: Dict[str, int]
    total_size: int
    last_modified: str


@dataclass
class ValidationResult:
    """Result of code validation."""
    is_valid: bool
    errors: List[str]
    warnings: List[str]
    suggestions: List[str]


class MCPIntegration:
    """MCP integration for file system access and tool orchestration."""

    def __init__(self, logger=None):
        self.config = Config()
        self.logger = logger or get_logger()
        self.output_dir = Path(self.config.CODE_OUTPUT_DIR)

        # Available MCP tools
        self.tools = {
            "file_reader": MCPTool(
                name="file_reader",
                description="Read and analyze existing files",
                parameters={"path": "string", "analysis_type": "string"}
            ),
            "file_writer": MCPTool(
                name="file_writer",
                description="Write files with validation",
                parameters={"path": "string", "content": "string", "validate": "boolean"}
            ),
            "directory_creator": MCPTool(
                name="directory_creator",
                description="Create directory structures",
                parameters={"paths": "array", "recursive": "boolean"}
            ),
            "code_validator": MCPTool(
                name="code_validator",
                description="Validate code syntax and style",
                parameters={"language": "string", "content": "string", "rules": "array"}
            ),
            "dependency_analyzer": MCPTool(
                name="dependency_analyzer",
                description="Analyze file dependencies",
                parameters={"files": "array", "language": "string"}
            )
        }

        self.logger.log_workflow("🔗 MCP Integration initialized with file system tools")

    def scan_existing_codebase(self, base_path: str = None) -> FileSystemInfo:
        """
        Scan existing codebase to understand current structure.

        Args:
            base_path: Base directory to scan (defaults to output directory)

        Returns:
            FileSystemInfo with current codebase structure
        """
        scan_path = Path(base_path) if base_path else self.output_dir

        if not scan_path.exists():
            self.logger.log_workflow(f"📁 Creating output directory: {scan_path}")
            scan_path.mkdir(parents=True, exist_ok=True)
            return FileSystemInfo([], [], {}, 0, "")

        self.logger.log_workflow(f"🔍 Scanning codebase at: {scan_path}")

        directories = []
        files = []
        file_types = {}
        total_size = 0

        for item in scan_path.rglob("*"):
            if item.is_dir():
                directories.append(str(item.relative_to(scan_path)))
            elif item.is_file():
                rel_path = str(item.relative_to(scan_path))
                files.append(rel_path)

                # Track file types
                suffix = item.suffix.lower()
                file_types[suffix] = file_types.get(suffix, 0) + 1

                # Track size
                try:
                    total_size += item.stat().st_size
                except OSError:
                    pass

        info = FileSystemInfo(
            directories=sorted(directories),
            files=sorted(files),
            file_types=file_types,
            total_size=total_size,
            last_modified=str(scan_path.stat().st_mtime) if scan_path.exists() else ""
        )

        self.logger.log_workflow(
            f"📊 Scanned: {len(directories)} dirs, {len(files)} files, "
            f"{total_size:,} bytes"
        )

        return info

    def read_existing_files(self, file_paths: List[str]) -> Dict[str, str]:
        """
        Read content of existing files.

        Args:
            file_paths: List of file paths to read

        Returns:
            Dictionary mapping file paths to their content
        """
        file_contents = {}

        for file_path in file_paths:
            full_path = self.output_dir / file_path

            if full_path.exists() and full_path.is_file():
                try:
                    with open(full_path, 'r', encoding='utf-8') as f:
                        content = f.read()
                        file_contents[file_path] = content
                        self.logger.log_workflow(f"📖 Read file: {file_path}")
                except Exception as e:
                    self.logger.log_error(f"Failed to read {file_path}: {str(e)}", "mcp_file_read")
            else:
                self.logger.log_workflow(f"⚠️ File not found: {file_path}")

        return file_contents

    def analyze_dependencies(self, files: Dict[str, str], language: str = None) -> Dict[str, List[str]]:
        """
        Analyze dependencies between files using universal patterns.

        Args:
            files: Dictionary of file paths to content
            language: Programming language (optional, not used in universal approach)

        Returns:
            Dictionary mapping file paths to their dependencies
        """
        dependencies = {}

        for file_path, content in files.items():
            deps = self._analyze_universal_dependencies(content, file_path)
            dependencies[file_path] = deps
            self.logger.log_workflow(f"🔗 {file_path}: {len(deps)} dependencies")

        return dependencies

    def _analyze_universal_dependencies(self, content: str, file_path: str) -> List[str]:
        """Universal dependency analysis - works for any language/framework."""
        dependencies = []
        lines = content.split('\n')

        # Universal import/dependency patterns
        import_patterns = [
            # Programming languages
            r'import\s+([^;\n]+)',           # Java, Go, TypeScript, etc.
            r'from\s+([^\s]+)\s+import',     # Python, JavaScript
            r'#include\s*[<"]([^>"]+)[>"]',  # C/C++
            r'require\s*\(\s*[\'"]([^\'"]+)[\'"]\s*\)',  # Node.js
            r'use\s+([^;\n]+)',              # Rust, PHP
            r'using\s+([^;\n]+)',            # C#

            # Frontend frameworks
            r'import.*from\s+[\'"]([^\'"]+)[\'"]',  # React, Vue, Angular
            r'@import\s+[\'"]([^\'"]+)[\'"]',        # CSS/SCSS

            # Configuration files
            r'extends:\s*[\'"]?([^\'"]+)[\'"]?',     # YAML configs
            r'"extends":\s*[\'"]([^\'"]+)[\'"]',     # JSON configs

            # Infrastructure
            r'FROM\s+([^\s]+)',              # Dockerfile
            r'image:\s*([^\s]+)',            # Docker Compose, K8s
            r'source\s*=\s*[\'"]([^\'"]+)[\'"]',  # Terraform
        ]

        import re
        for line in lines[:50]:  # Check first 50 lines for imports
            line = line.strip()
            if not line or line.startswith('#') or line.startswith('//'):
                continue

            for pattern in import_patterns:
                matches = re.findall(pattern, line, re.IGNORECASE)
                for match in matches:
                    # Clean up the dependency
                    dep = match.strip().strip('"\'').strip()
                    if dep and dep not in dependencies:
                        dependencies.append(dep)

        # File path-based dependencies (relative imports)
        relative_deps = self._extract_relative_dependencies(content, file_path)
        dependencies.extend(relative_deps)

        return dependencies[:20]  # Limit to top 20 dependencies

    def _extract_relative_dependencies(self, content: str, file_path: str) -> List[str]:
        """Extract relative file dependencies from content."""
        dependencies = []

        # Look for relative file references
        relative_patterns = [
            r'[\'"]\.\/([^\'"\s]+)[\'"]',     # ./relative/path
            r'[\'"]\.\.\/([^\'"\s]+)[\'"]',   # ../relative/path
            r'[\'"]([^\/\'"]+\.(?:js|ts|jsx|tsx|vue|dart|java|py|go|rs|cs))[\'"]',  # filename.ext
        ]

        import re
        for pattern in relative_patterns:
            matches = re.findall(pattern, content, re.IGNORECASE)
            for match in matches:
                if match and match not in dependencies:
                    dependencies.append(match)

        return dependencies[:10]  # Limit relative dependencies

    def validate_code(self, content: str, language: str, file_path: str = None) -> ValidationResult:
        """
        Universal code validation - works for any language/framework.

        Args:
            content: Code content to validate
            language: Programming language (used for context, not hardcoded rules)
            file_path: Optional file path for context

        Returns:
            ValidationResult with validation details
        """
        try:
            result = self._validate_universal_code(content, language, file_path)

            self.logger.log_workflow(
                f"✅ Validation: {'PASS' if result.is_valid else 'FAIL'} "
                f"({len(result.errors)} errors, {len(result.warnings)} warnings)"
            )

            return result

        except Exception as e:
            self.logger.log_error(f"Validation failed: {str(e)}", "mcp_validation")
            return ValidationResult(False, [str(e)], [], [])

    def _validate_universal_code(self, content: str, language: str, file_path: str = None) -> ValidationResult:
        """Universal code validation - works for any language/framework."""
        errors = []
        warnings = []
        suggestions = []

        # Universal basic checks
        if not content.strip():
            errors.append("File is empty")
            return ValidationResult(False, errors, warnings, suggestions)

        # File extension and content consistency
        if file_path:
            filename = file_path.split('/')[-1]
            errors.extend(self._validate_file_consistency(content, filename, language))

        # Universal syntax checks
        syntax_issues = self._validate_universal_syntax(content, language)
        errors.extend(syntax_issues.get('errors', []))
        warnings.extend(syntax_issues.get('warnings', []))
        suggestions.extend(syntax_issues.get('suggestions', []))

        # Universal style checks
        style_issues = self._validate_universal_style(content)
        warnings.extend(style_issues.get('warnings', []))
        suggestions.extend(style_issues.get('suggestions', []))

        return ValidationResult(len(errors) == 0, errors, warnings, suggestions)

    def _validate_file_consistency(self, content: str, filename: str, language: str) -> List[str]:
        """Validate file name and content consistency."""
        errors = []

        # Extract base name without extension
        base_name = filename.split('.')[0]

        # Universal naming consistency checks
        if any(ext in filename for ext in ['.java', '.kt', '.cs', '.swift']):
            # Class-based languages - check if class name matches filename
            class_patterns = [
                r'public\s+class\s+(\w+)',
                r'class\s+(\w+)',
                r'interface\s+(\w+)',
                r'struct\s+(\w+)'
            ]

            import re
            for pattern in class_patterns:
                matches = re.findall(pattern, content, re.IGNORECASE)
                for match in matches:
                    if match.lower() != base_name.lower():
                        errors.append(f"Class/interface name '{match}' should match filename '{base_name}'")
                        break

        elif filename.endswith('.go'):
            # Go files should have package declaration
            if not content.strip().startswith('package '):
                errors.append("Go files must start with a package declaration")

        elif any(ext in filename for ext in ['.jsx', '.tsx', '.vue']):
            # Component files should export a component
            if 'export' not in content and 'module.exports' not in content:
                errors.append("Component files should export a component")

        return errors

    def _validate_universal_syntax(self, content: str, language: str) -> Dict[str, List[str]]:
        """Universal syntax validation."""
        issues = {'errors': [], 'warnings': [], 'suggestions': []}

        # Bracket/brace matching (universal)
        bracket_pairs = {'(': ')', '[': ']', '{': '}'}
        stack = []

        for i, char in enumerate(content):
            if char in bracket_pairs:
                stack.append((char, i))
            elif char in bracket_pairs.values():
                if not stack:
                    issues['errors'].append(f"Unmatched closing bracket '{char}' at position {i}")
                else:
                    open_char, _ = stack.pop()
                    if bracket_pairs[open_char] != char:
                        issues['errors'].append(f"Mismatched brackets: '{open_char}' and '{char}' at position {i}")

        if stack:
            for open_char, pos in stack:
                issues['errors'].append(f"Unmatched opening bracket '{open_char}' at position {pos}")

        # String quote matching (universal)
        quote_chars = ['"', "'", '`']
        for quote in quote_chars:
            if content.count(quote) % 2 != 0:
                issues['warnings'].append(f"Unmatched {quote} quotes detected")

        return issues

    def _validate_universal_style(self, content: str) -> Dict[str, List[str]]:
        """Universal style validation."""
        issues = {'warnings': [], 'suggestions': []}

        lines = content.split('\n')

        for i, line in enumerate(lines, 1):
            # Line length check (universal)
            if len(line) > 120:
                issues['warnings'].append(f"Line {i}: Line too long ({len(line)} > 120 characters)")

            # Trailing whitespace (universal)
            if line.endswith(' ') or line.endswith('\t'):
                issues['suggestions'].append(f"Line {i}: Trailing whitespace")

            # Mixed tabs and spaces (universal)
            if '\t' in line and '    ' in line:
                issues['warnings'].append(f"Line {i}: Mixed tabs and spaces")

            # Empty lines at end of file
            if i == len(lines) and not line.strip():
                issues['suggestions'].append("Remove empty lines at end of file")

        return issues

    def create_directory_structure(self, directories: List[str]) -> bool:
        """
        Create directory structure.

        Args:
            directories: List of directory paths to create

        Returns:
            True if successful, False otherwise
        """
        try:
            for directory in directories:
                dir_path = self.output_dir / directory
                dir_path.mkdir(parents=True, exist_ok=True)
                self.logger.log_workflow(f"📁 Created directory: {directory}")

            return True

        except Exception as e:
            self.logger.log_error(f"Failed to create directories: {str(e)}", "mcp_directory_creation")
            return False

    def backup_existing_file(self, file_path: str) -> Optional[str]:
        """
        Create backup of existing file.

        Args:
            file_path: Path to file to backup

        Returns:
            Backup file path if successful, None otherwise
        """
        if not self.config.BACKUP_EXISTING_FILES:
            return None

        full_path = self.output_dir / file_path
        if not full_path.exists():
            return None

        try:
            backup_path = full_path.with_suffix(full_path.suffix + '.backup')
            shutil.copy2(full_path, backup_path)
            self.logger.log_workflow(f"💾 Backed up: {file_path} -> {backup_path.name}")
            return str(backup_path)

        except Exception as e:
            self.logger.log_error(f"Failed to backup {file_path}: {str(e)}", "mcp_backup")
            return None

    def get_file_system_context(self, target_file: Dict[str, Any]) -> Dict[str, Any]:
        """
        Get file system context for a target file.

        Args:
            target_file: Target file metadata

        Returns:
            File system context information
        """
        file_path = target_file.get("path", "")
        file_dir = str(Path(file_path).parent) if file_path else ""

        # Scan current state
        fs_info = self.scan_existing_codebase()

        # Find related files
        related_files = []
        for existing_file in fs_info.files:
            if file_dir and existing_file.startswith(file_dir):
                related_files.append(existing_file)

        # Read related files
        related_content = self.read_existing_files(related_files[:5])  # Limit to 5 files

        return {
            "existing_structure": {
                "directories": fs_info.directories,
                "files": fs_info.files[:20],  # Limit for context
                "file_types": fs_info.file_types
            },
            "related_files": related_content,
            "target_directory": file_dir,
            "directory_exists": file_dir in fs_info.directories if file_dir else True
        }

    def get_available_tools(self) -> List[Dict[str, Any]]:
        """Get list of available MCP tools."""
        return [
            {
                "name": tool.name,
                "description": tool.description,
                "parameters": tool.parameters,
                "enabled": tool.enabled
            }
            for tool in self.tools.values()
        ]

    def execute_tool(self, tool_name: str, parameters: Dict[str, Any]) -> Dict[str, Any]:
        """
        Execute an MCP tool.

        Args:
            tool_name: Name of the tool to execute
            parameters: Tool parameters

        Returns:
            Tool execution result
        """
        if tool_name not in self.tools:
            return {"success": False, "error": f"Unknown tool: {tool_name}"}

        tool = self.tools[tool_name]
        if not tool.enabled:
            return {"success": False, "error": f"Tool disabled: {tool_name}"}

        try:
            if tool_name == "file_reader":
                return self._execute_file_reader(parameters)
            elif tool_name == "file_writer":
                return self._execute_file_writer(parameters)
            elif tool_name == "directory_creator":
                return self._execute_directory_creator(parameters)
            elif tool_name == "code_validator":
                return self._execute_code_validator(parameters)
            elif tool_name == "dependency_analyzer":
                return self._execute_dependency_analyzer(parameters)
            else:
                return {"success": False, "error": f"Tool not implemented: {tool_name}"}

        except Exception as e:
            self.logger.log_error(f"Tool execution failed: {str(e)}", f"mcp_tool_{tool_name}")
            return {"success": False, "error": str(e)}

    def _execute_file_reader(self, params: Dict[str, Any]) -> Dict[str, Any]:
        """Execute file reader tool."""
        path = params.get("path", "")
        files = self.read_existing_files([path])
        return {
            "success": True,
            "content": files.get(path, ""),
            "exists": path in files
        }

    def _execute_file_writer(self, params: Dict[str, Any]) -> Dict[str, Any]:
        """Execute file writer tool."""
        path = params.get("path", "")
        content = params.get("content", "")
        validate = params.get("validate", True)

        if validate:
            # This would be implemented in the file creator
            pass

        return {"success": True, "message": f"File write prepared: {path}"}

    def _execute_directory_creator(self, params: Dict[str, Any]) -> Dict[str, Any]:
        """Execute directory creator tool."""
        paths = params.get("paths", [])
        success = self.create_directory_structure(paths)
        return {"success": success, "created": paths if success else []}

    def _execute_code_validator(self, params: Dict[str, Any]) -> Dict[str, Any]:
        """Execute code validator tool."""
        language = params.get("language", "")
        content = params.get("content", "")

        result = self.validate_code(content, language)
        return {
            "success": result.is_valid,
            "errors": result.errors,
            "warnings": result.warnings,
            "suggestions": result.suggestions
        }

    def _execute_dependency_analyzer(self, params: Dict[str, Any]) -> Dict[str, Any]:
        """Execute dependency analyzer tool."""
        files = params.get("files", {})
        language = params.get("language", "")

        dependencies = self.analyze_dependencies(files, language)
        return {"success": True, "dependencies": dependencies}
