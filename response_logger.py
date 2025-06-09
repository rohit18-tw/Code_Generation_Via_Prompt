"""Response logging system for capturing LLM interactions and workflow logs."""

import json
import os
from datetime import datetime
from pathlib import Path
from typing import Dict, Any, List, Optional
import uuid


class ResponseLogger:
    """Handles logging of LLM responses and workflow interactions."""

    def __init__(self, base_dir: str = "Response", session_id: str = None, reuse_session: bool = True):
        self.base_dir = Path(base_dir)
        self.base_dir.mkdir(exist_ok=True)

        # Session management with reuse capability
        if session_id:
            self.session_id = session_id
        elif reuse_session:
            self.session_id = self._get_or_create_session()
        else:
            self.session_id = self._create_new_session()

        self.session_dir = self.base_dir / self.session_id
        self.session_dir.mkdir(exist_ok=True)

        # Initialize log files
        self.workflow_log_file = self.session_dir / "workflow_log.txt"
        self.interactions_log_file = self.session_dir / "llm_interactions.json"

        # Initialize data structures
        self.workflow_logs: List[str] = []
        self.llm_interactions: List[Dict[str, Any]] = []

        # Log session start
        self.log_workflow(f"🚀 Session started: {self.session_id}")
        self.log_workflow(f"📁 Session directory: {self.session_dir}")

    def _create_new_session(self) -> str:
        """Create a new session ID."""
        return datetime.now().strftime("%Y%m%d_%H%M%S") + "_" + str(uuid.uuid4())[:8]

    def _get_or_create_session(self) -> str:
        """Get existing session or create new one based on recency."""
        # Look for recent sessions (within last 10 minutes)
        current_time = datetime.now()
        recent_threshold = 10 * 60  # 10 minutes in seconds

        if self.base_dir.exists():
            for session_dir in self.base_dir.iterdir():
                if session_dir.is_dir() and len(session_dir.name) > 15:
                    try:
                        # Parse session timestamp
                        timestamp_str = session_dir.name[:15]  # YYYYMMDD_HHMMSS
                        session_time = datetime.strptime(timestamp_str, "%Y%m%d_%H%M%S")

                        # Check if session is recent
                        time_diff = (current_time - session_time).total_seconds()
                        if time_diff < recent_threshold:
                            # Check if session has actual content (not just initialization)
                            workflow_log = session_dir / "workflow_log.txt"
                            if workflow_log.exists():
                                with open(workflow_log, 'r') as f:
                                    content = f.read()
                                    # If session has substantial content, reuse it
                                    if len(content.split('\n')) > 5:
                                        return session_dir.name
                    except (ValueError, OSError):
                        continue

        # No suitable session found, create new one
        return self._create_new_session()

    def cleanup_empty_sessions(self) -> int:
        """Clean up empty or incomplete sessions."""
        cleaned_count = 0

        if not self.base_dir.exists():
            return cleaned_count

        for session_dir in self.base_dir.iterdir():
            if session_dir.is_dir() and len(session_dir.name) > 15:
                try:
                    # Check if session is empty or has minimal content
                    workflow_log = session_dir / "workflow_log.txt"
                    llm_interactions = session_dir / "llm_interactions.json"

                    is_empty = True

                    # Check workflow log
                    if workflow_log.exists():
                        with open(workflow_log, 'r') as f:
                            lines = f.readlines()
                            # If more than just session start messages, keep it
                            if len(lines) > 3:
                                is_empty = False

                    # Check LLM interactions
                    if llm_interactions.exists():
                        with open(llm_interactions, 'r') as f:
                            data = json.load(f)
                            if data:  # Has actual interactions
                                is_empty = False

                    # Remove empty sessions older than 1 hour
                    if is_empty:
                        try:
                            timestamp_str = session_dir.name[:15]
                            session_time = datetime.strptime(timestamp_str, "%Y%m%d_%H%M%S")
                            age_hours = (datetime.now() - session_time).total_seconds() / 3600

                            if age_hours > 1:  # Older than 1 hour
                                import shutil
                                shutil.rmtree(session_dir)
                                cleaned_count += 1
                        except (ValueError, OSError):
                            pass

                except (OSError, json.JSONDecodeError):
                    continue

        return cleaned_count

    def log_workflow(self, message: str, level: str = "INFO") -> None:
        """Log workflow events and steps."""
        timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        log_entry = f"[{timestamp}] [{level}] {message}"

        # Add to memory
        self.workflow_logs.append(log_entry)

        # Write to file immediately
        with open(self.workflow_log_file, 'a', encoding='utf-8') as f:
            f.write(log_entry + "\n")

        # Also print to console
        print(log_entry)

    def log_llm_interaction(self,
                           step: str,
                           prompt: str,
                           response: str,
                           model: str = "claude-3-5-sonnet",
                           metadata: Optional[Dict[str, Any]] = None) -> str:
        """
        Log an LLM interaction and save the response to a separate file.

        Args:
            step: The workflow step (e.g., "plan_generation", "code_generation")
            prompt: The prompt sent to the LLM
            response: The response from the LLM
            model: The model used
            metadata: Additional metadata about the interaction

        Returns:
            The filename where the response was saved
        """
        timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        interaction_id = f"{step}_{len(self.llm_interactions) + 1:03d}"

        # Create interaction record
        interaction = {
            "id": interaction_id,
            "timestamp": timestamp,
            "step": step,
            "model": model,
            "prompt": prompt,
            "response": response,
            "metadata": metadata or {},
            "response_file": f"{interaction_id}_response.txt"
        }

        # Save response to separate file
        response_file = self.session_dir / interaction["response_file"]
        with open(response_file, 'w', encoding='utf-8') as f:
            f.write(f"# LLM Response - {step}\n")
            f.write(f"# Timestamp: {timestamp}\n")
            f.write(f"# Model: {model}\n")
            f.write(f"# Interaction ID: {interaction_id}\n")
            f.write("# " + "="*50 + "\n\n")
            f.write(response)

        # Add to interactions log
        self.llm_interactions.append(interaction)

        # Save interactions log
        with open(self.interactions_log_file, 'w', encoding='utf-8') as f:
            json.dump(self.llm_interactions, f, indent=2, ensure_ascii=False)

        # Log the interaction in workflow log
        self.log_workflow(f"💬 LLM Interaction: {step} -> {interaction['response_file']}")

        return interaction["response_file"]

    def log_error(self, error: str, step: str = "unknown") -> None:
        """Log an error that occurred during the workflow."""
        self.log_workflow(f"❌ ERROR in {step}: {error}", level="ERROR")

    def log_success(self, message: str, step: str = "unknown") -> None:
        """Log a successful operation."""
        self.log_workflow(f"✅ SUCCESS in {step}: {message}", level="SUCCESS")



    def save_generated_code(self, code: str, filename: str, language: str = "python") -> str:
        """Save generated code to a file."""
        # Create code directory if it doesn't exist
        code_dir = self.session_dir / "generated_code"
        code_dir.mkdir(exist_ok=True)

        # Save the code
        code_file = code_dir / filename
        with open(code_file, 'w', encoding='utf-8') as f:
            f.write(f"# Generated Code - {language}\n")
            f.write(f"# Generated at: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")
            f.write(f"# Session: {self.session_id}\n")
            f.write("# " + "="*50 + "\n\n")
            f.write(code)

        self.log_workflow(f"💻 Code saved: {code_file.relative_to(self.session_dir)}")
        return str(code_file)






# Global logger instance
_logger_instance = None

def get_logger(reuse_session: bool = True) -> ResponseLogger:
    """Get the global logger instance with session reuse."""
    global _logger_instance
    if _logger_instance is None:
        _logger_instance = ResponseLogger(reuse_session=reuse_session)
        # Clean up old empty sessions
        cleaned = _logger_instance.cleanup_empty_sessions()
        if cleaned > 0:
            _logger_instance.log_workflow(f"🧹 Cleaned up {cleaned} empty sessions")
    return _logger_instance

def init_logger(base_dir: str = "Response", force_new_session: bool = False) -> ResponseLogger:
    """Initialize a new logger instance."""
    global _logger_instance
    _logger_instance = ResponseLogger(base_dir, reuse_session=not force_new_session)

    # Clean up old empty sessions
    cleaned = _logger_instance.cleanup_empty_sessions()
    if cleaned > 0:
        _logger_instance.log_workflow(f"🧹 Cleaned up {cleaned} empty sessions")

    return _logger_instance

def cleanup_sessions() -> int:
    """Clean up empty sessions manually."""
    temp_logger = ResponseLogger(reuse_session=False)
    return temp_logger.cleanup_empty_sessions()
