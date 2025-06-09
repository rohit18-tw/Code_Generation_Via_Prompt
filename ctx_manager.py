"""Context Management System (CTX) for intelligent code generation context optimization."""

import json
import re
from typing import Dict, List, Any, Optional, Set, Tuple
from dataclasses import dataclass, field
from pathlib import Path
import tiktoken
from collections import defaultdict

from config import Config
from response_logger import get_logger


@dataclass
class ContextItem:
    """Represents a single context item with metadata."""
    content: str
    source_file: str
    item_type: str  # 'interface', 'type', 'function', 'constant', 'import'
    relevance_score: float = 0.0
    dependencies: Set[str] = field(default_factory=set)
    tokens: int = 0


@dataclass
class GenerationContext:
    """Complete context for generating a specific file."""
    target_file: Dict[str, Any]
    project_context: Dict[str, Any]
    dependency_context: List[ContextItem]
    pattern_context: Dict[str, Any]
    total_tokens: int
    compression_ratio: float


class ContextManager:
    """Intelligent context management for code generation."""
    
    def __init__(self, logger=None):
        self.config = Config()
        self.logger = logger or get_logger()
        self.encoding = tiktoken.get_encoding("cl100k_base")
        
        # Context memory and learning
        self.pattern_memory: Dict[str, Any] = {}
        self.interface_registry: Dict[str, ContextItem] = {}
        self.dependency_graph: Dict[str, Set[str]] = defaultdict(set)
        
        # Context optimization settings
        self.compression_ratio = self.config.CTX_COMPRESSION_RATIO
        self.max_context_tokens = self.config.CHUNK_TOKEN_LIMIT
        
        self.logger.log_workflow("🧠 CTX Manager initialized with intelligent context optimization")

    def build_generation_context(self, target_file: Dict[str, Any], 
                                development_plan: Dict[str, Any],
                                existing_files: Dict[str, str] = None) -> GenerationContext:
        """
        Build optimized context for generating a specific file.
        
        Args:
            target_file: File metadata from development plan
            development_plan: Complete development plan
            existing_files: Already generated files {path: content}
            
        Returns:
            Optimized GenerationContext
        """
        self.logger.log_workflow(f"🎯 Building context for: {target_file['path']}")
        
        # Extract project context
        project_context = self._extract_project_context(development_plan)
        
        # Build dependency context
        dependency_context = self._build_dependency_context(
            target_file, development_plan, existing_files or {}
        )
        
        # Apply pattern learning
        pattern_context = self._apply_pattern_learning(target_file, development_plan)
        
        # Optimize context for token limits
        optimized_context = self._optimize_context_tokens(
            dependency_context, target_file, project_context
        )
        
        # Calculate total tokens
        total_tokens = self._calculate_total_tokens(optimized_context, project_context, pattern_context)
        
        context = GenerationContext(
            target_file=target_file,
            project_context=project_context,
            dependency_context=optimized_context,
            pattern_context=pattern_context,
            total_tokens=total_tokens,
            compression_ratio=len(optimized_context) / max(len(dependency_context), 1)
        )
        
        self.logger.log_workflow(
            f"📊 Context built: {len(optimized_context)} items, "
            f"{total_tokens} tokens, {context.compression_ratio:.2f} compression"
        )
        
        return context

    def _extract_project_context(self, development_plan: Dict[str, Any]) -> Dict[str, Any]:
        """Extract essential project context."""
        project = development_plan.get("project", {})
        code_gen = development_plan.get("code_generation", {})
        
        return {
            "language": project.get("language", "unknown"),
            "framework": project.get("framework", "unknown"),
            "type": project.get("type", "unknown"),
            "file_templates": code_gen.get("file_templates", {}),
            "validation_rules": code_gen.get("validation_rules", []),
            "dependencies": development_plan.get("dependencies", {})
        }

    def _build_dependency_context(self, target_file: Dict[str, Any], 
                                 development_plan: Dict[str, Any],
                                 existing_files: Dict[str, str]) -> List[ContextItem]:
        """Build context from file dependencies."""
        context_items = []
        dependencies = target_file.get("dependencies", [])
        
        # Process each dependency
        for dep in dependencies:
            # Find matching files in the development plan
            matching_files = self._find_matching_files(dep, development_plan)
            
            for file_info in matching_files:
                file_path = file_info["path"]
                
                # If file already exists, extract relevant context
                if file_path in existing_files:
                    items = self._extract_context_from_file(
                        existing_files[file_path], file_path, target_file
                    )
                    context_items.extend(items)
                else:
                    # Create placeholder context based on file metadata
                    items = self._create_placeholder_context(file_info, target_file)
                    context_items.extend(items)
        
        return context_items

    def _find_matching_files(self, dependency: str, development_plan: Dict[str, Any]) -> List[Dict[str, Any]]:
        """Find files that match a dependency pattern."""
        files = development_plan.get("structure", {}).get("files", [])
        matching_files = []
        
        for file_info in files:
            file_path = file_info["path"]
            
            # Direct path match
            if dependency in file_path:
                matching_files.append(file_info)
            # Package/module match
            elif dependency.replace("/", ".") in file_path.replace("/", "."):
                matching_files.append(file_info)
        
        return matching_files

    def _extract_context_from_file(self, file_content: str, file_path: str, 
                                  target_file: Dict[str, Any]) -> List[ContextItem]:
        """Extract relevant context items from existing file content."""
        context_items = []
        language = target_file.get("language", "unknown")
        
        if language == "go":
            context_items.extend(self._extract_go_context(file_content, file_path))
        elif language == "java":
            context_items.extend(self._extract_java_context(file_content, file_path))
        elif language == "python":
            context_items.extend(self._extract_python_context(file_content, file_path))
        else:
            # Generic extraction
            context_items.extend(self._extract_generic_context(file_content, file_path))
        
        # Calculate relevance scores
        for item in context_items:
            item.relevance_score = self._calculate_relevance_score(item, target_file)
            item.tokens = len(self.encoding.encode(item.content))
        
        return context_items

    def _extract_go_context(self, content: str, file_path: str) -> List[ContextItem]:
        """Extract Go-specific context items."""
        items = []
        
        # Extract interfaces
        interface_pattern = r'type\s+(\w+)\s+interface\s*\{([^}]+)\}'
        for match in re.finditer(interface_pattern, content, re.MULTILINE | re.DOTALL):
            items.append(ContextItem(
                content=match.group(0),
                source_file=file_path,
                item_type="interface"
            ))
        
        # Extract struct types
        struct_pattern = r'type\s+(\w+)\s+struct\s*\{([^}]+)\}'
        for match in re.finditer(struct_pattern, content, re.MULTILINE | re.DOTALL):
            items.append(ContextItem(
                content=match.group(0),
                source_file=file_path,
                item_type="type"
            ))
        
        # Extract constants
        const_pattern = r'const\s*\(([^)]+)\)|const\s+\w+\s*=\s*[^;\n]+'
        for match in re.finditer(const_pattern, content, re.MULTILINE | re.DOTALL):
            items.append(ContextItem(
                content=match.group(0),
                source_file=file_path,
                item_type="constant"
            ))
        
        return items

    def _extract_java_context(self, content: str, file_path: str) -> List[ContextItem]:
        """Extract Java-specific context items."""
        items = []
        
        # Extract interfaces
        interface_pattern = r'public\s+interface\s+(\w+)(?:\s+extends\s+[\w,\s]+)?\s*\{([^}]+)\}'
        for match in re.finditer(interface_pattern, content, re.MULTILINE | re.DOTALL):
            items.append(ContextItem(
                content=match.group(0),
                source_file=file_path,
                item_type="interface"
            ))
        
        # Extract class definitions (just signatures)
        class_pattern = r'public\s+class\s+(\w+)(?:\s+extends\s+\w+)?(?:\s+implements\s+[\w,\s]+)?\s*\{'
        for match in re.finditer(class_pattern, content):
            items.append(ContextItem(
                content=match.group(0),
                source_file=file_path,
                item_type="type"
            ))
        
        return items

    def _extract_python_context(self, content: str, file_path: str) -> List[ContextItem]:
        """Extract Python-specific context items."""
        items = []
        
        # Extract class definitions
        class_pattern = r'class\s+(\w+)(?:\([^)]*\))?\s*:'
        for match in re.finditer(class_pattern, content):
            items.append(ContextItem(
                content=match.group(0),
                source_file=file_path,
                item_type="type"
            ))
        
        # Extract function signatures
        func_pattern = r'def\s+(\w+)\s*\([^)]*\)\s*(?:->\s*[^:]+)?\s*:'
        for match in re.finditer(func_pattern, content):
            items.append(ContextItem(
                content=match.group(0),
                source_file=file_path,
                item_type="function"
            ))
        
        return items

    def _extract_generic_context(self, content: str, file_path: str) -> List[ContextItem]:
        """Extract generic context items for unknown languages."""
        # For now, just return a summary
        lines = content.split('\n')
        summary_lines = lines[:5] + ['...'] + lines[-5:] if len(lines) > 10 else lines
        
        return [ContextItem(
            content='\n'.join(summary_lines),
            source_file=file_path,
            item_type="summary"
        )]

    def _create_placeholder_context(self, file_info: Dict[str, Any], 
                                   target_file: Dict[str, Any]) -> List[ContextItem]:
        """Create placeholder context for files that don't exist yet."""
        file_type = file_info.get("type", "unknown")
        purpose = file_info.get("purpose", "")
        
        placeholder_content = f"// {purpose}\n// Type: {file_type}\n// Path: {file_info['path']}"
        
        return [ContextItem(
            content=placeholder_content,
            source_file=file_info["path"],
            item_type="placeholder",
            relevance_score=0.3  # Lower relevance for placeholders
        )]

    def _calculate_relevance_score(self, item: ContextItem, target_file: Dict[str, Any]) -> float:
        """Calculate relevance score for a context item."""
        score = 0.0
        target_type = target_file.get("type", "")
        target_path = target_file.get("path", "")
        
        # Type-based scoring
        if item.item_type == "interface" and target_type in ["service", "repository"]:
            score += 0.8
        elif item.item_type == "type" and target_type in ["service", "controller"]:
            score += 0.7
        elif item.item_type == "constant":
            score += 0.5
        
        # Path-based scoring
        if item.source_file in target_file.get("dependencies", []):
            score += 0.6
        
        # Content-based scoring (simple keyword matching)
        target_keywords = target_path.lower().split("/")[-1].replace(".go", "").replace(".java", "").replace(".py", "")
        if target_keywords in item.content.lower():
            score += 0.4
        
        return min(score, 1.0)

    def _apply_pattern_learning(self, target_file: Dict[str, Any], 
                               development_plan: Dict[str, Any]) -> Dict[str, Any]:
        """Apply learned patterns to improve context."""
        file_type = target_file.get("type", "")
        language = development_plan.get("project", {}).get("language", "")
        
        # Get patterns for this file type and language
        pattern_key = f"{language}_{file_type}"
        patterns = self.pattern_memory.get(pattern_key, {})
        
        return {
            "naming_conventions": patterns.get("naming_conventions", {}),
            "error_handling": patterns.get("error_handling", {}),
            "logging_patterns": patterns.get("logging_patterns", {}),
            "validation_patterns": patterns.get("validation_patterns", {})
        }

    def _optimize_context_tokens(self, context_items: List[ContextItem], 
                                target_file: Dict[str, Any],
                                project_context: Dict[str, Any]) -> List[ContextItem]:
        """Optimize context to fit within token limits."""
        # Sort by relevance score (descending)
        sorted_items = sorted(context_items, key=lambda x: x.relevance_score, reverse=True)
        
        # Calculate available tokens (reserve space for project context and generation)
        reserved_tokens = 2000  # Reserve for project context and generation space
        available_tokens = self.max_context_tokens - reserved_tokens
        
        optimized_items = []
        current_tokens = 0
        
        for item in sorted_items:
            if current_tokens + item.tokens <= available_tokens:
                optimized_items.append(item)
                current_tokens += item.tokens
            else:
                # Try to compress the item
                compressed_item = self._compress_context_item(item, available_tokens - current_tokens)
                if compressed_item:
                    optimized_items.append(compressed_item)
                    current_tokens += compressed_item.tokens
                break
        
        return optimized_items

    def _compress_context_item(self, item: ContextItem, max_tokens: int) -> Optional[ContextItem]:
        """Compress a context item to fit within token limit."""
        if max_tokens < 50:  # Not worth compressing if too small
            return None
        
        # Simple compression: take first part of content
        lines = item.content.split('\n')
        compressed_lines = []
        current_tokens = 0
        
        for line in lines:
            line_tokens = len(self.encoding.encode(line))
            if current_tokens + line_tokens <= max_tokens:
                compressed_lines.append(line)
                current_tokens += line_tokens
            else:
                compressed_lines.append("// ... (truncated)")
                break
        
        if compressed_lines:
            return ContextItem(
                content='\n'.join(compressed_lines),
                source_file=item.source_file,
                item_type=item.item_type,
                relevance_score=item.relevance_score * 0.8,  # Reduce score for compressed content
                dependencies=item.dependencies,
                tokens=current_tokens
            )
        
        return None

    def _calculate_total_tokens(self, context_items: List[ContextItem], 
                               project_context: Dict[str, Any],
                               pattern_context: Dict[str, Any]) -> int:
        """Calculate total tokens for the complete context."""
        context_tokens = sum(item.tokens for item in context_items)
        project_tokens = len(self.encoding.encode(json.dumps(project_context)))
        pattern_tokens = len(self.encoding.encode(json.dumps(pattern_context)))
        
        return context_tokens + project_tokens + pattern_tokens

    def learn_from_generation(self, target_file: Dict[str, Any], 
                             generated_code: str, success: bool):
        """Learn patterns from successful generations."""
        if not success or not self.config.CTX_PATTERN_LEARNING:
            return
        
        file_type = target_file.get("type", "")
        language = target_file.get("language", "unknown")
        pattern_key = f"{language}_{file_type}"
        
        # Extract patterns from generated code
        patterns = self._extract_patterns_from_code(generated_code, language)
        
        # Update pattern memory
        if pattern_key not in self.pattern_memory:
            self.pattern_memory[pattern_key] = {}
        
        # Merge patterns
        for pattern_type, pattern_data in patterns.items():
            if pattern_type not in self.pattern_memory[pattern_key]:
                self.pattern_memory[pattern_key][pattern_type] = pattern_data
            else:
                # Simple merge strategy - could be more sophisticated
                self.pattern_memory[pattern_key][pattern_type].update(pattern_data)
        
        self.logger.log_workflow(f"📚 Learned patterns for {pattern_key}")

    def _extract_patterns_from_code(self, code: str, language: str) -> Dict[str, Any]:
        """Extract reusable patterns from generated code."""
        patterns = {
            "naming_conventions": {},
            "error_handling": {},
            "logging_patterns": {},
            "validation_patterns": {}
        }
        
        # This is a simplified pattern extraction
        # In a real implementation, this would be much more sophisticated
        
        if language == "go":
            # Extract Go naming patterns
            if "func New" in code:
                patterns["naming_conventions"]["constructor"] = "New{TypeName}"
            if "if err != nil" in code:
                patterns["error_handling"]["check"] = "if err != nil { return nil, err }"
        
        return patterns

    def get_context_summary(self, context: GenerationContext) -> str:
        """Get a human-readable summary of the context."""
        summary = f"""
🧠 **Context Summary for {context.target_file['path']}**

**Project**: {context.project_context['language']} {context.project_context['framework']}
**Target Type**: {context.target_file.get('type', 'unknown')}
**Dependencies**: {len(context.dependency_context)} items
**Total Tokens**: {context.total_tokens:,}
**Compression**: {context.compression_ratio:.1%}

**Context Items**:
"""
        
        for item in context.dependency_context[:5]:  # Show top 5
            summary += f"- {item.item_type} from {item.source_file} (score: {item.relevance_score:.2f})\n"
        
        if len(context.dependency_context) > 5:
            summary += f"- ... and {len(context.dependency_context) - 5} more items\n"
        
        return summary.strip()
