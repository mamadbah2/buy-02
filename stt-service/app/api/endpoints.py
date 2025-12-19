from fastapi import APIRouter, UploadFile, File, Form, HTTPException
from app.schemas.transcription import TranscriptionResponse
from app.services.inference_service import InferenceService
from typing import Annotated
import logging

logger = logging.getLogger(__name__)

router = APIRouter()

@router.post("/transcribe", response_model=TranscriptionResponse)
async def transcribe_audio(
    file: Annotated[UploadFile, File(description="Audio file (.wav)")],
    language: Annotated[str, Form(description="Language code (wol, fuf)", pattern="^(wol|fuf)$")]
):
    """
    Transcribe an audio file to text.
    """
    logger.info(f"Received transcription request. File: {file.filename}, Content-Type: {file.content_type}, Language: {language}")

    if not file.filename.endswith(('.wav', '.WAV')):
         logger.warning(f"Rejected file extension: {file.filename}")
         raise HTTPException(status_code=400, detail="Only .wav files are supported")
         
    return await InferenceService.transcribe(file, language)
