"""Configuration settings for the LangGraph agentic workflow."""

import os
from typing import Optional
from dotenv import load_dotenv

# Load environment variables
load_dotenv()

class Config:
    """Configuration class for the agentic workflow."""

    # API Keys
    ANTHROPIC_API_KEY: str = os.getenv("ANTHROPIC_API_KEY", "")
    GITHUB_TOKEN: Optional[str] = os.getenv("GITHUB_TOKEN")

    # Model Configuration
    DEFAULT_MODEL: str = os.getenv("DEFAULT_MODEL", "claude-3-5-sonnet-20241022")
    MAX_TOKENS: int = 16000  # Increased to 16000 for enhanced planning structure
    TEMPERATURE: float = 0.1



    # File Processing
    SUPPORTED_TEXT_EXTENSIONS = {".txt", ".md", ".py", ".js", ".ts", ".html", ".css", ".json", ".yaml", ".yml"}
    SUPPORTED_DOC_EXTENSIONS = {".pdf", ".docx", ".doc"}
    SUPPORTED_IMAGE_EXTENSIONS = {".png", ".jpg", ".jpeg", ".gif", ".bmp", ".webp"}

    # Validation
    ENABLE_STATIC_ANALYSIS = True
    ENABLE_TYPE_CHECKING = True
    ENABLE_LINTING = True

    # Code Generation Configuration
    ENABLE_CODE_GENERATION = True
    CODE_OUTPUT_DIR = os.getenv("CODE_OUTPUT_DIR", "./generated_code")
    MAX_FILES_PER_CHUNK = int(os.getenv("MAX_FILES_PER_CHUNK", "6"))
    CHUNK_TOKEN_LIMIT = int(os.getenv("CHUNK_TOKEN_LIMIT", "12000"))



    # CTX Configuration
    ENABLE_CTX = True
    CTX_COMPRESSION_RATIO = float(os.getenv("CTX_COMPRESSION_RATIO", "0.4"))  # Compress context to 40% of original
    CTX_MEMORY_ENABLED = True
    CTX_PATTERN_LEARNING = True

    # MCP Configuration
    ENABLE_MCP = True
    MCP_TOOLS_ENABLED = True
    MCP_FILE_SYSTEM_ACCESS = True
    MCP_VALIDATION_ENABLED = True

    # File Creation Configuration
    CREATE_DIRECTORY_STRUCTURE = True
    VALIDATE_BEFORE_WRITE = True
    BACKUP_EXISTING_FILES = False  # Disabled: No backup files will be created
    DRY_RUN_MODE = bool(os.getenv("DRY_RUN_MODE", "False").lower() == "true")

    @classmethod
    def validate(cls) -> bool:
        """Validate that required configuration is present."""
        if not cls.ANTHROPIC_API_KEY:
            raise ValueError("ANTHROPIC_API_KEY is required. Please set it in your .env file.")

        # Validate code generation settings
        if cls.ENABLE_CODE_GENERATION:
            if cls.MAX_FILES_PER_CHUNK < 1 or cls.MAX_FILES_PER_CHUNK > 20:
                raise ValueError("MAX_FILES_PER_CHUNK must be between 1 and 20")
            if cls.CHUNK_TOKEN_LIMIT < 1000 or cls.CHUNK_TOKEN_LIMIT > cls.MAX_TOKENS:
                raise ValueError(f"CHUNK_TOKEN_LIMIT must be between 1000 and {cls.MAX_TOKENS}")

        return True

# Validate configuration on import
Config.validate()
