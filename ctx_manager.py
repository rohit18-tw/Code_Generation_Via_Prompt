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

        # Use simplified context management - let Claude handle intelligence
        self.use_ai = False
        self.encoding = tiktoken.get_encoding("cl100k_base")

        # Context memory and learning
        self.pattern_memory: Dict[str, Any] = {}
        self.interface_registry: Dict[str, ContextItem] = {}
        self.dependency_graph: Dict[str, Set[str]] = defaultdict(set)

        # Context optimization settings
        self.compression_ratio = self.config.CTX_COMPRESSION_RATIO
        self.max_context_tokens = 8000  # Default context limit since CHUNK_TOKEN_LIMIT was removed

        self.logger.log_workflow("🎯 CTX Manager initialized - relying on Claude's natural intelligence")

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
        # Use minimal context building - Claude's intelligence is sufficient
        return self._build_minimal_context(target_file, development_plan, existing_files)


    def _build_minimal_context(self, target_file: Dict[str, Any],
                              development_plan: Dict[str, Any],
                              existing_files: Dict[str, str] = None) -> GenerationContext:
        """Build minimal but effective context - Claude handles the complexity."""
        self.logger.log_workflow(f"🎯 Building minimal context for: {target_file['path']}")

        # Only extract essential dependency content
        dependencies = []
        for dep_path in target_file.get("dependencies", []):
            if existing_files and dep_path in existing_files:
                # Just include the raw content - Claude will understand it
                content = existing_files[dep_path][:1000]  # First 1000 chars max
                dependencies.append(ContextItem(
                    content=content,
                    source_file=dep_path,
                    item_type="dependency",
                    relevance_score=1.0,
                    tokens=len(content) // 4  # Rough token estimate
                ))

        # Minimal project context - just language and framework
        project = development_plan.get("project", {})
        minimal_project_context = {
            "language": project.get("language", "unknown"),
            "framework": project.get("framework", "unknown")
        }

        context = GenerationContext(
            target_file=target_file,
            project_context=minimal_project_context,
            dependency_context=dependencies,
            pattern_context={},  # Empty - Claude knows patterns
            total_tokens=sum(item.tokens for item in dependencies),
            compression_ratio=1.0
        )

        self.logger.log_workflow(
            f"📊 Minimal context built: {len(dependencies)} dependencies, "
            f"{context.total_tokens} tokens"
        )

        return context

    def get_context_summary(self, context: GenerationContext) -> str:
        """Get a simple summary of the minimal context."""
        return f"📊 Minimal context: {len(context.dependency_context)} dependencies, {context.total_tokens} tokens"

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
