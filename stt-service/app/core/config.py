from pydantic_settings import BaseSettings

class Settings(BaseSettings):
    PROJECT_NAME: str = "STT Service"
    VERSION: str = "1.0.0"
    MODEL_ID: str = "facebook/mms-1b-all"
    # Supported languages: Wolof (wol), Pular (fuf)
    SUPPORTED_LANGUAGES: list[str] = ["wol", "fuf"]
    SAMPLE_RATE: int = 16000

    class Config:
        env_file = ".env"

settings = Settings()
