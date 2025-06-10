"""
AI-Driven Context Manager
Replaces regex/tree-sitter with intelligent AI-based context extraction.
"""

import json
from typing import Dict, List, Any, Optional
from dataclasses import dataclass
from pathlib import Path
import tiktoken
from collections import defaultdict

from langchain_anthropic import ChatAnthropic
from config import Config
from response_logger import get_logger

@dataclass
class AIContextItem:
    """Context item extracted by AI analysis."""
    content: str
    source_file: str
    item_type: str  # interface, class, function, pattern, etc.
    name: str
    relevance_score: float
    reasoning: str  # Why AI thinks this is relevant
    tokens: int = 0
    language: str = "unknown"

@dataclass
class TechStack:
    """Technology stack suggested by AI."""
    language: str
    framework: str
    database: str
    architecture: str
    reasoning: str
    confidence: float

@dataclass
class AIGenerationContext:
    """Complete context for AI-driven code generation."""
    user_intent: Dict[str, Any]
    tech_stack: TechStack
    relevant_context: List[AIContextItem]
    total_tokens: int
    context_summary: str

class AIContextManager:
    """AI-powered context manager that understands intent and extracts relevant code."""

    def __init__(self, logger=None):
        self.config = Config()
        self.logger = logger or get_logger()
        self.llm = ChatAnthropic(
            anthropic_api_key=self.config.ANTHROPIC_API_KEY,
            model=self.config.DEFAULT_MODEL,
            max_tokens=self.config.MAX_TOKENS,
            temperature=0.1  # Low temperature for consistent analysis
        )
        self.encoding = tiktoken.get_encoding("cl100k_base")

        self.logger.log_workflow("🧠 AI Context Manager initialized - No language-specific code needed!")

    def build_smart_context(self, user_prompt: str, existing_files: Dict[str, Any] = None) -> AIGenerationContext:
        """
        Build intelligent context using AI analysis instead of regex/tree-sitter.

        Args:
            user_prompt: What the user wants to build
            existing_files: Existing codebase files {path: content}

        Returns:
            AI-optimized generation context
        """
        self.logger.log_workflow(f"🎯 Building AI context for: {user_prompt}")

        # Step 1: Analyze user intent
        intent_data = self.analyze_user_intent(user_prompt)

        # Step 2: Suggest or detect tech stack
        tech_stack = self.suggest_tech_stack(intent_data, existing_files)

        # Step 3: Extract relevant context using AI
        relevant_context = []
        if existing_files:
            for file_path, file_content in existing_files.items():
                if isinstance(file_content, dict):
                    file_content = file_content.get("content", "")

                if file_content and len(file_content.strip()) > 0:
                    context_items = self.extract_relevant_context(
                        file_content, file_path, intent_data, tech_stack
                    )
                    relevant_context.extend(context_items)

        # Step 4: Optimize context for token limits
        optimized_context = self._optimize_context_tokens(relevant_context)

        # Step 5: Generate context summary
        context_summary = self._generate_context_summary(intent_data, tech_stack, optimized_context)

        total_tokens = sum(item.tokens for item in optimized_context)

        ai_context = AIGenerationContext(
            user_intent=intent_data,
            tech_stack=tech_stack,
            relevant_context=optimized_context,
            total_tokens=total_tokens,
            context_summary=context_summary
        )

        self.logger.log_workflow(
            f"✅ AI context built: {len(optimized_context)} items, "
            f"{total_tokens} tokens, {tech_stack.language} + {tech_stack.framework}"
        )

        return ai_context

    def analyze_user_intent(self, user_prompt: str) -> Dict[str, Any]:
        """Analyze user prompt to understand intent, domain, and requirements."""

        analysis_prompt = f"""
Analyze this user request and extract key information:

User Request: "{user_prompt}"

Extract and return as JSON:
{{
    "intent": "api|web_app|cli_tool|data_processing|mobile_app|desktop_app",
    "domain": "user_management|e_commerce|blog|cms|etc",
    "complexity": "simple|medium|complex",
    "key_features": ["feature1", "feature2"],
    "tech_stack_specified": true/false,
    "specified_stack": {{
        "language": "if mentioned",
        "framework": "if mentioned",
        "database": "if mentioned"
    }},
    "important_concepts": ["authentication", "crud", "api", "etc"]
}}

Focus on understanding WHAT the user wants to build and HOW complex it is.
"""

        try:
            response = self.llm.invoke(analysis_prompt)
            intent_data = json.loads(response.content)

            self.logger.log_workflow(f"🎯 Intent analyzed: {intent_data.get('intent')} for {intent_data.get('domain')}")
            return intent_data

        except Exception as e:
            self.logger.log_error(f"Intent analysis failed: {e}", "ai_context")
            # Fallback to basic analysis
            return {
                "intent": "api",
                "domain": "general",
                "complexity": "medium",
                "key_features": [],
                "tech_stack_specified": False,
                "specified_stack": {},
                "important_concepts": []
            }

    def suggest_tech_stack(self, intent_data: Dict[str, Any], existing_files: Dict[str, Any] = None) -> TechStack:
        """AI suggests the best technology stack based on requirements."""

        if intent_data.get("tech_stack_specified", False):
            # User specified stack, use it
            specified = intent_data.get("specified_stack", {})
            return TechStack(
                language=specified.get("language", "python"),
                framework=specified.get("framework", "fastapi"),
                database=specified.get("database", "postgresql"),
                architecture="layered",
                reasoning="User specified stack",
                confidence=1.0
            )

        # Analyze existing codebase if available
        existing_context = ""
        if existing_files:
            existing_context = f"\nExisting codebase files: {list(existing_files.keys())[:5]}"

        stack_prompt = f"""
Suggest the best technology stack for this project:

Intent: {intent_data.get('intent')}
Domain: {intent_data.get('domain')}
Complexity: {intent_data.get('complexity')}
Key Features: {intent_data.get('key_features')}
Important Concepts: {intent_data.get('important_concepts')}
{existing_context}

Consider:
1. Development speed and simplicity
2. Community support and documentation
3. Performance requirements
4. Team productivity (assume small team)
5. Existing codebase patterns (if any)

Return as JSON:
{{
    "language": "python|java|go|javascript|typescript|rust",
    "framework": "fastapi|spring-boot|gin|express|nestjs|axum",
    "database": "postgresql|mysql|mongodb|sqlite",
    "architecture": "layered|hexagonal|microservices|monolith",
    "reasoning": "Why this stack is best for the requirements",
    "confidence": 0.0-1.0
}}

Prioritize proven, productive stacks over trendy ones.
"""

        try:
            response = self.llm.invoke(stack_prompt)
            stack_data = json.loads(response.content)

            tech_stack = TechStack(
                language=stack_data.get("language", "python"),
                framework=stack_data.get("framework", "fastapi"),
                database=stack_data.get("database", "postgresql"),
                architecture=stack_data.get("architecture", "layered"),
                reasoning=stack_data.get("reasoning", "AI suggested stack"),
                confidence=stack_data.get("confidence", 0.8)
            )

            self.logger.log_workflow(f"🛠️ Suggested stack: {tech_stack.language} + {tech_stack.framework}")
            return tech_stack

        except Exception as e:
            self.logger.log_error(f"Stack suggestion failed: {e}", "ai_context")
            # Fallback to safe defaults
            return TechStack(
                language="python",
                framework="fastapi",
                database="postgresql",
                architecture="layered",
                reasoning="Fallback to proven stack",
                confidence=0.6
            )

    def extract_relevant_context(self, file_content: str, file_path: str,
                                intent_data: Dict[str, Any], tech_stack: TechStack) -> List[AIContextItem]:
        """AI extracts relevant context from code based on user intent."""

        # Determine what's important based on intent
        focus_areas = self._get_focus_areas_for_intent(intent_data.get("intent", "api"))

        context_prompt = f"""
Analyze this code file and extract the most relevant parts for building: {intent_data.get('domain')} {intent_data.get('intent')}

Target Stack: {tech_stack.language} + {tech_stack.framework}
Key Features Needed: {intent_data.get('key_features')}
Important Concepts: {intent_data.get('important_concepts')}

File: {file_path}
```
{file_content[:3000]}  # Limit content to avoid token overflow
```

Focus on extracting:
{focus_areas}

For each relevant code construct, return JSON array:
[
    {{
        "name": "construct_name",
        "type": "interface|class|function|pattern|constant|config",
        "content": "the actual code snippet",
        "relevance_score": 0.0-1.0,
        "reasoning": "why this is relevant for the user's goal"
    }}
]

Only include constructs that are actually useful for: {intent_data.get('domain')} {intent_data.get('intent')}
Prioritize interfaces, data models, and patterns over implementation details.
"""

        try:
            response = self.llm.invoke(context_prompt)
            context_data = json.loads(response.content)

            items = []
            for item_data in context_data:
                item = AIContextItem(
                    content=item_data.get("content", ""),
                    source_file=file_path,
                    item_type=item_data.get("type", "unknown"),
                    name=item_data.get("name", "Unknown"),
                    relevance_score=item_data.get("relevance_score", 0.5),
                    reasoning=item_data.get("reasoning", "AI analysis"),
                    tokens=len(self.encoding.encode(item_data.get("content", ""))),
                    language=tech_stack.language
                )
                items.append(item)

            # Sort by relevance score
            items.sort(key=lambda x: x.relevance_score, reverse=True)

            self.logger.log_workflow(f"🔍 Extracted {len(items)} relevant items from {file_path}")
            return items

        except Exception as e:
            self.logger.log_error(f"Context extraction failed for {file_path}: {e}", "ai_context")
            return []

    def _get_focus_areas_for_intent(self, intent: str) -> str:
        """Get focus areas based on user intent."""
        focus_map = {
            "api": """
- REST endpoint interfaces and contracts
- Data models and DTOs
- Service layer patterns
- Error handling approaches
- Authentication/authorization patterns
- Database entity definitions
""",
            "web_app": """
- Component structures and patterns
- State management approaches
- Routing configurations
- UI component interfaces
- Data fetching patterns
- Form handling approaches
""",
            "cli_tool": """
- Command line argument parsing
- Command pattern implementations
- Configuration handling
- Output formatting approaches
- Error handling for CLI
- Main entry point patterns
""",
            "data_processing": """
- Data pipeline patterns
- ETL/ELT approaches
- Data validation schemas
- Batch processing patterns
- Stream processing interfaces
- Data transformation functions
""",
            "mobile_app": """
- Screen/view components
- Navigation patterns
- State management
- API integration patterns
- Local storage approaches
- Platform-specific interfaces
""",
            "desktop_app": """
- Window/view management
- Event handling patterns
- Menu and toolbar structures
- File handling approaches
- Settings/configuration management
- Platform integration patterns
"""
        }

        return focus_map.get(intent, focus_map["api"])  # Default to API focus

    def _optimize_context_tokens(self, context_items: List[AIContextItem]) -> List[AIContextItem]:
        """Optimize context to fit within token limits using AI relevance scores."""
        # Sort by relevance score (highest first)
        sorted_items = sorted(context_items, key=lambda x: x.relevance_score, reverse=True)

        optimized_items = []
        current_tokens = 0

        for item in sorted_items:
            if current_tokens + item.tokens <= self.max_context_tokens:
                optimized_items.append(item)
                current_tokens += item.tokens
            else:
                # For high-relevance items, try to include a summary
                if item.relevance_score > 0.8:
                    summary_item = self._create_summary_item(item)
                    if current_tokens + summary_item.tokens <= self.max_context_tokens:
                        optimized_items.append(summary_item)
                        current_tokens += summary_item.tokens
                break

        return optimized_items

    def _create_summary_item(self, item: AIContextItem) -> AIContextItem:
        """Create a summarized version of a context item."""
        lines = item.content.split('\n')
        if len(lines) <= 3:
            return item  # Already short

        # Take first few lines and add truncation notice
        summary_lines = lines[:2] + ['    // ... (truncated for space)']
        summary_content = '\n'.join(summary_lines)

        return AIContextItem(
            content=summary_content,
            source_file=item.source_file,
            item_type=item.item_type,
            name=item.name,
            relevance_score=item.relevance_score * 0.9,  # Slight penalty for truncation
            reasoning=f"{item.reasoning} (summarized)",
            tokens=len(self.encoding.encode(summary_content)),
            language=item.language
        )

    def _generate_context_summary(self, intent_data: Dict[str, Any],
                                 tech_stack: TechStack,
                                 context_items: List[AIContextItem]) -> str:
        """Generate a human-readable summary of the context."""

        # Group items by type
        items_by_type = defaultdict(list)
        for item in context_items:
            items_by_type[item.item_type].append(item)

        summary = f"""
🧠 **AI Context Summary**

**User Intent**: {intent_data.get('intent')} for {intent_data.get('domain')}
**Complexity**: {intent_data.get('complexity')}
**Tech Stack**: {tech_stack.language} + {tech_stack.framework} + {tech_stack.database}
**Stack Confidence**: {tech_stack.confidence:.1%}

**Relevant Context Found**:
"""

        for item_type, items in items_by_type.items():
            avg_relevance = sum(item.relevance_score for item in items) / len(items)
            summary += f"- {item_type.title()}: {len(items)} items (avg relevance: {avg_relevance:.2f})\n"

        summary += f"\n**Total Context**: {len(context_items)} items, {sum(item.tokens for item in context_items)} tokens"

        if tech_stack.reasoning:
            summary += f"\n**Stack Reasoning**: {tech_stack.reasoning}"

        return summary.strip()

    def get_generation_prompt_context(self, ai_context: AIGenerationContext) -> str:
        """Convert AI context into a prompt-ready format for code generation."""

        context_prompt = f"""
# Context for Code Generation

## User Requirements
- **Intent**: {ai_context.user_intent.get('intent')}
- **Domain**: {ai_context.user_intent.get('domain')}
- **Key Features**: {ai_context.user_intent.get('key_features')}
- **Complexity**: {ai_context.user_intent.get('complexity')}

## Technology Stack
- **Language**: {ai_context.tech_stack.language}
- **Framework**: {ai_context.tech_stack.framework}
- **Database**: {ai_context.tech_stack.database}
- **Architecture**: {ai_context.tech_stack.architecture}

## Relevant Code Patterns
"""

        # Add relevant context items
        for item in ai_context.relevant_context[:10]:  # Limit to top 10 items
            context_prompt += f"""
### {item.name} ({item.item_type})
**Relevance**: {item.relevance_score:.2f} - {item.reasoning}
**Source**: {item.source_file}

```{item.language}
{item.content}
```
"""

        return context_prompt
