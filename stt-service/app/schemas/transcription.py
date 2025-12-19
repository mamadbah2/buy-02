from pydantic import BaseModel, Field

class TranscriptionRequest(BaseModel):
    language: str = Field(..., description="Language code (wol, fuf)", pattern="^(wol|fuf)$")

class TranscriptionResponse(BaseModel):
    transcription: str
    language: str
    duration: float
