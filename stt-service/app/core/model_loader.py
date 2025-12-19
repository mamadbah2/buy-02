import torch
from transformers import Wav2Vec2ForCTC, AutoProcessor
from app.core.config import settings
import logging
import threading

logger = logging.getLogger(__name__)

class STTModel:
    _instance = None
    _lock = threading.Lock()

    def __new__(cls):
        if cls._instance is None:
            with cls._lock:
                if cls._instance is None:
                    cls._instance = super(STTModel, cls).__new__(cls)
                    cls._instance.initialized = False
        return cls._instance

    def load_model(self):
        if not self.initialized:
            logger.info(f"Loading model {settings.MODEL_ID}...")
            try:
                self.processor = AutoProcessor.from_pretrained(settings.MODEL_ID)
                self.model = Wav2Vec2ForCTC.from_pretrained(settings.MODEL_ID)
                
                # Move to GPU if available
                self.device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
                try:
                    self.model.to(self.device)
                except torch.OutOfMemoryError:
                    logger.warning("CUDA Out of Memory. Falling back to CPU.")
                    torch.cuda.empty_cache()
                    self.device = torch.device("cpu")
                    self.model.to(self.device)
                
                # Initialize adapters for supported languages to ensure they are cached/ready if needed
                # For MMS, we usually just set the target lang during inference
                
                self.initialized = True
                logger.info("Model loaded successfully.")
            except Exception as e:
                logger.error(f"Failed to load model: {e}")
                raise e

    def predict(self, audio_input, lang: str) -> str:
        """
        Thread-safe prediction method.
        """
        if not self.initialized:
            raise RuntimeError("Model not initialized. Call load_model() first.")

        # Use a lock to ensure thread safety when switching languages on the shared model/processor
        with self._lock:
            try:
                # Set language
                self.processor.tokenizer.set_target_lang(lang)
                self.model.load_adapter(lang)

                inputs = self.processor(audio_input, sampling_rate=settings.SAMPLE_RATE, return_tensors="pt")
                inputs = inputs.to(self.device)

                with torch.no_grad():
                    outputs = self.model(**inputs)

                ids = torch.argmax(outputs.logits, dim=-1)[0]
                transcription = self.processor.decode(ids)
                return transcription
            except Exception as e:
                logger.error(f"Prediction error: {e}")
                raise e

stt_model = STTModel()
