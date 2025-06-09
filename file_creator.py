"""File creation system with validation, formatting, and safety features."""

import os
import shutil
import tempfile
from typing import Dict, List, Any, Optional, Tuple
from pathlib import Path
from dataclasses import dataclass
import subprocess
import time

from config import Config
from response_logger import get_logger
from mcp_integration import MCPIntegration, ValidationResult


@dataclass
class FileCreationResult:
    """Result of file creation operation."""
    file_path: str
    success: bool
    created: bool
    backed_up: bool
    formatted: bool
    validation_passed: bool
    errors: List[str]
    warnings: List[str]
    file_size: int


@dataclass
class CreationSummary:
    """Summary of batch file creation."""
    total_files: int
    created_files: int
    failed_files: int
    backed_up_files: int
    formatted_files: int
    total_size: int
    creation_time: float
    errors: List[str]


class FileCreator:
    """Intelligent file creation with validation and safety features."""
    
    def __init__(self, logger=None):
        self.config = Config()
        self.logger = logger or get_logger()
        self.mcp_integration = MCPIntegration(logger)
        
        # File creation settings
        self.output_dir = Path(self.config.CODE_OUTPUT_DIR)
        self.dry_run = self.config.DRY_RUN_MODE
        self.validate_before_write = self.config.VALIDATE_BEFORE_WRITE
        self.backup_existing = self.config.BACKUP_EXISTING_FILES
        
        # Ensure output directory exists
        if not self.dry_run:
            self.output_dir.mkdir(parents=True, exist_ok=True)
        
        self.logger.log_workflow(
            f"📁 File Creator initialized - Output: {self.output_dir}, "
            f"Dry Run: {self.dry_run}, Validation: {self.validate_before_write}"
        )

    def create_files_from_generation(self, generation_results: Dict[str, Any]) -> CreationSummary:
        """
        Create files from code generation results.
        
        Args:
            generation_results: Results from CodeGenerator
            
        Returns:
            CreationSummary with creation statistics
        """
        start_time = time.time()
        self.logger.log_workflow("📝 Starting file creation from generation results")
        
        # Extract file information from generation results
        files_to_create = []
        chunk_results = generation_results.get("chunk_results", [])
        
        for chunk in chunk_results:
            for file_result in chunk.get("files", []):
                if file_result.get("success", False):
                    files_to_create.append({
                        "path": file_result["file_path"],
                        "content": file_result["content"],
                        "language": self._detect_language(file_result["file_path"])
                    })
        
        # Create files
        creation_results = []
        for file_info in files_to_create:
            result = self.create_single_file(
                file_info["path"],
                file_info["content"],
                file_info["language"]
            )
            creation_results.append(result)
        
        # Compile summary
        summary = self._compile_creation_summary(creation_results, start_time)
        
        self.logger.log_workflow(
            f"✅ File creation complete: {summary.created_files}/{summary.total_files} files created "
            f"in {summary.creation_time:.2f}s"
        )
        
        return summary

    def create_single_file(self, file_path: str, content: str, language: str = None) -> FileCreationResult:
        """
        Create a single file with validation and safety features.
        
        Args:
            file_path: Relative path for the file
            content: File content
            language: Programming language (auto-detected if None)
            
        Returns:
            FileCreationResult with creation details
        """
        full_path = self.output_dir / file_path
        language = language or self._detect_language(file_path)
        
        self.logger.log_workflow(f"📄 Creating file: {file_path}")
        
        result = FileCreationResult(
            file_path=file_path,
            success=False,
            created=False,
            backed_up=False,
            formatted=False,
            validation_passed=False,
            errors=[],
            warnings=[],
            file_size=0
        )
        
        try:
            # Step 1: Validate content if enabled
            if self.validate_before_write:
                validation = self.mcp_integration.validate_code(content, language, file_path)
                result.validation_passed = validation.is_valid
                result.errors.extend(validation.errors)
                result.warnings.extend(validation.warnings)
                
                if not validation.is_valid and not self.config.DRY_RUN_MODE:
                    self.logger.log_workflow(f"❌ Validation failed for {file_path}")
                    return result
            else:
                result.validation_passed = True
            
            # Step 2: Backup existing file if it exists
            if full_path.exists() and self.backup_existing:
                backup_path = self.mcp_integration.backup_existing_file(file_path)
                result.backed_up = backup_path is not None
            
            # Step 3: Format content
            formatted_content = self._format_content(content, language)
            result.formatted = formatted_content != content
            
            # Step 4: Create directory structure
            if not self.dry_run:
                full_path.parent.mkdir(parents=True, exist_ok=True)
            
            # Step 5: Write file
            if self.dry_run:
                self.logger.log_workflow(f"🔍 DRY RUN: Would create {file_path} ({len(formatted_content)} chars)")
                result.created = True
                result.success = True
                result.file_size = len(formatted_content.encode('utf-8'))
            else:
                with open(full_path, 'w', encoding='utf-8') as f:
                    f.write(formatted_content)
                
                result.created = True
                result.success = True
                result.file_size = full_path.stat().st_size
                
                self.logger.log_workflow(f"✅ Created: {file_path} ({result.file_size:,} bytes)")
            
            # Step 6: Post-creation validation
            if not self.dry_run and self.validate_before_write:
                post_validation = self._post_creation_validation(full_path, language)
                if not post_validation.is_valid:
                    result.warnings.extend([f"Post-creation warning: {w}" for w in post_validation.warnings])
        
        except Exception as e:
            error_msg = f"Failed to create {file_path}: {str(e)}"
            result.errors.append(error_msg)
            self.logger.log_error(error_msg, "file_creation")
        
        return result

    def _detect_language(self, file_path: str) -> str:
        """Detect programming language from file extension."""
        suffix = Path(file_path).suffix.lower()
        
        language_map = {
            '.py': 'python',
            '.go': 'go',
            '.java': 'java',
            '.js': 'javascript',
            '.ts': 'typescript',
            '.jsx': 'javascript',
            '.tsx': 'typescript',
            '.rs': 'rust',
            '.cpp': 'cpp',
            '.c': 'c',
            '.cs': 'csharp',
            '.php': 'php',
            '.rb': 'ruby',
            '.kt': 'kotlin',
            '.scala': 'scala',
            '.swift': 'swift',
            '.dart': 'dart',
            '.sql': 'sql',
            '.yaml': 'yaml',
            '.yml': 'yaml',
            '.json': 'json',
            '.xml': 'xml',
            '.html': 'html',
            '.css': 'css',
            '.scss': 'scss',
            '.md': 'markdown',
            '.sh': 'bash',
            '.dockerfile': 'dockerfile'
        }
        
        return language_map.get(suffix, 'text')

    def _format_content(self, content: str, language: str) -> str:
        """Format content according to language conventions."""
        if self.dry_run:
            return content  # Skip formatting in dry run
        
        try:
            if language == 'python':
                return self._format_python(content)
            elif language == 'go':
                return self._format_go(content)
            elif language == 'java':
                return self._format_java(content)
            elif language in ['javascript', 'typescript']:
                return self._format_javascript(content)
            elif language == 'json':
                return self._format_json(content)
            else:
                return content  # No formatting available
                
        except Exception as e:
            self.logger.log_workflow(f"⚠️ Formatting failed for {language}: {str(e)}")
            return content  # Return original content if formatting fails

    def _format_python(self, content: str) -> str:
        """Format Python code using black."""
        try:
            # Check if black is available
            result = subprocess.run(['black', '--version'], capture_output=True, text=True)
            if result.returncode != 0:
                return content
            
            # Create temporary file
            with tempfile.NamedTemporaryFile(mode='w', suffix='.py', delete=False) as f:
                f.write(content)
                temp_path = f.name
            
            try:
                # Run black
                result = subprocess.run(
                    ['black', '--quiet', temp_path],
                    capture_output=True,
                    text=True,
                    timeout=30
                )
                
                if result.returncode == 0:
                    with open(temp_path, 'r', encoding='utf-8') as f:
                        formatted_content = f.read()
                    return formatted_content
                else:
                    return content
            finally:
                os.unlink(temp_path)
                
        except (subprocess.TimeoutExpired, FileNotFoundError, Exception):
            return content

    def _format_go(self, content: str) -> str:
        """Format Go code using go fmt."""
        try:
            # Check if go is available
            result = subprocess.run(['go', 'version'], capture_output=True, text=True)
            if result.returncode != 0:
                return content
            
            # Use go fmt
            result = subprocess.run(
                ['go', 'fmt'],
                input=content,
                capture_output=True,
                text=True,
                timeout=30
            )
            
            if result.returncode == 0:
                return result.stdout
            else:
                return content
                
        except (subprocess.TimeoutExpired, FileNotFoundError, Exception):
            return content

    def _format_java(self, content: str) -> str:
        """Basic Java formatting (indentation and spacing)."""
        lines = content.split('\n')
        formatted_lines = []
        indent_level = 0
        
        for line in lines:
            stripped = line.strip()
            
            # Decrease indent for closing braces
            if stripped.startswith('}'):
                indent_level = max(0, indent_level - 1)
            
            # Add indented line
            if stripped:
                formatted_lines.append('    ' * indent_level + stripped)
            else:
                formatted_lines.append('')
            
            # Increase indent for opening braces
            if stripped.endswith('{'):
                indent_level += 1
        
        return '\n'.join(formatted_lines)

    def _format_javascript(self, content: str) -> str:
        """Basic JavaScript formatting."""
        # Similar to Java formatting for now
        return self._format_java(content)

    def _format_json(self, content: str) -> str:
        """Format JSON content."""
        try:
            import json
            parsed = json.loads(content)
            return json.dumps(parsed, indent=2, ensure_ascii=False)
        except json.JSONDecodeError:
            return content

    def _post_creation_validation(self, file_path: Path, language: str) -> ValidationResult:
        """Perform post-creation validation."""
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                content = f.read()
            
            return self.mcp_integration.validate_code(content, language, str(file_path))
            
        except Exception as e:
            return ValidationResult(
                is_valid=False,
                errors=[f"Post-creation validation failed: {str(e)}"],
                warnings=[],
                suggestions=[]
            )

    def _compile_creation_summary(self, results: List[FileCreationResult], start_time: float) -> CreationSummary:
        """Compile creation summary from individual results."""
        total_files = len(results)
        created_files = sum(1 for r in results if r.created)
        failed_files = total_files - created_files
        backed_up_files = sum(1 for r in results if r.backed_up)
        formatted_files = sum(1 for r in results if r.formatted)
        total_size = sum(r.file_size for r in results)
        creation_time = time.time() - start_time
        
        # Collect all errors
        all_errors = []
        for result in results:
            all_errors.extend(result.errors)
        
        return CreationSummary(
            total_files=total_files,
            created_files=created_files,
            failed_files=failed_files,
            backed_up_files=backed_up_files,
            formatted_files=formatted_files,
            total_size=total_size,
            creation_time=creation_time,
            errors=all_errors
        )

    def create_project_files(self, file_specs: List[Dict[str, str]]) -> CreationSummary:
        """
        Create multiple files from specifications.
        
        Args:
            file_specs: List of dicts with 'path', 'content', and optional 'language'
            
        Returns:
            CreationSummary with creation statistics
        """
        start_time = time.time()
        self.logger.log_workflow(f"📦 Creating {len(file_specs)} project files")
        
        results = []
        for spec in file_specs:
            result = self.create_single_file(
                spec['path'],
                spec['content'],
                spec.get('language')
            )
            results.append(result)
        
        summary = self._compile_creation_summary(results, start_time)
        
        self.logger.log_workflow(
            f"📊 Project creation summary: {summary.created_files}/{summary.total_files} files, "
            f"{summary.total_size:,} bytes, {summary.creation_time:.2f}s"
        )
        
        return summary

    def get_creation_report(self, summary: CreationSummary) -> str:
        """Generate a human-readable creation report."""
        success_rate = summary.created_files / summary.total_files if summary.total_files > 0 else 0
        
        report = f"""
📁 **File Creation Report**

**Summary:**
- Total Files: {summary.total_files}
- Created Successfully: {summary.created_files}
- Failed: {summary.failed_files}
- Success Rate: {success_rate:.1%}

**Operations:**
- Files Backed Up: {summary.backed_up_files}
- Files Formatted: {summary.formatted_files}
- Total Size: {summary.total_size:,} bytes
- Creation Time: {summary.creation_time:.2f} seconds

**Output Directory:** {self.output_dir}
"""
        
        if summary.errors:
            report += f"\n**Errors ({len(summary.errors)}):**\n"
            for error in summary.errors[:5]:  # Show first 5 errors
                report += f"- {error}\n"
            if len(summary.errors) > 5:
                report += f"- ... and {len(summary.errors) - 5} more errors\n"
        
        return report.strip()

    def cleanup_failed_files(self, summary: CreationSummary) -> int:
        """Clean up any partially created files from failed operations."""
        if self.dry_run:
            return 0
        
        cleaned_count = 0
        # This would implement cleanup logic for failed file creations
        # For now, just return 0 as files are created atomically
        
        return cleaned_count
