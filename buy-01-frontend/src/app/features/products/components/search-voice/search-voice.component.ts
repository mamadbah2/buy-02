import { Component, EventEmitter, Output, inject, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ProductService } from '../../services/product.service';
import { Subject, takeUntil } from 'rxjs';

@Component({
  selector: 'app-search-voice',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './search-voice.component.html',
  styleUrls: ['./search-voice.component.css']
})
export class SearchVoiceComponent implements OnDestroy {
  @Output() search = new EventEmitter<string>();

  isRecording = false;
  isProcessing = false;
  mediaRecorder: MediaRecorder | null = null;
  audioChunks: Blob[] = [];
  
  private productService = inject(ProductService);
  private destroy$ = new Subject<void>();

  async startRecording() {
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      this.mediaRecorder = new MediaRecorder(stream);
      this.audioChunks = [];

      this.mediaRecorder.ondataavailable = (event) => {
        this.audioChunks.push(event.data);
      };

      this.mediaRecorder.onstop = () => {
        const audioBlob = new Blob(this.audioChunks, { type: 'audio/wav' });
        this.sendAudio(audioBlob);
        stream.getTracks().forEach(track => track.stop());
      };

      this.mediaRecorder.start();
      this.isRecording = true;
    } catch (error) {
      console.error('Error accessing microphone:', error);
      alert('Could not access microphone. Please allow microphone access.');
    }
  }

  stopRecording() {
    if (this.mediaRecorder && this.isRecording) {
      this.mediaRecorder.stop();
      this.isRecording = false;
      this.isProcessing = true;
    }
  }

  sendAudio(blob: Blob) {
    this.productService.transcribeAudio(blob)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          this.isProcessing = false;
          console.log('Transcription response:', response);
          if (response.transcription) {
            this.search.emit(response.transcription);
          }
        },
        error: (error) => {
          console.error('Transcription error:', error);
          this.isProcessing = false;
          alert('Failed to transcribe audio.');
        }
      });
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
