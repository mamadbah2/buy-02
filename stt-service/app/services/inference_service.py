from fastapi import UploadFile, HTTPException
from app.core.model_loader import stt_model
from app.services.audio_processor import AudioProcessor
from app.schemas.transcription import TranscriptionResponse
from app.core.config import settings

class InferenceService:
    @staticmethod
    async def transcribe(file: UploadFile, language: str) -> TranscriptionResponse:
        if language not in settings.SUPPORTED_LANGUAGES:
            raise HTTPException(status_code=400, detail=f"Language '{language}' not supported. Supported: {settings.SUPPORTED_LANGUAGES}")

        # Process audio
        audio_input, duration = AudioProcessor.process_audio(file)

        # Run inference
        try:
            text = stt_model.predict(audio_input, language)
        except Exception as e:
            raise HTTPException(status_code=500, detail=f"Inference failed: {str(e)}")

        return TranscriptionResponse(
            transcription=text,
            language=language,
            duration=duration
        )
