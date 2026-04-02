# 🎨 Frontend Angular - Guia Completo de Validação Facial

## 📋 Estrutura do Projeto Angular

```
src/app/
├── features/
│   ├── face-validation/
│   │   ├── components/
│   │   │   ├── face-capture/
│   │   │   └── face-registration/
│   │   ├── services/
│   │   │   └── face-validation.service.ts
│   │   └── guards/
│   │       └── face-validation.guard.ts
│   └── todos/
│       ├── components/
│       │   ├── todo-list/
│       │   └── todo-create/
│       └── services/
│           └── todo.service.ts
└── core/
    └── interceptors/
        └── face-token.interceptor.ts
```

---

## 📦 Dependências Necessárias

```bash
npm install face-api.js
npm install @types/face-api.js --save-dev
```

---

## 🔧 1. Serviço de Validação Facial

**face-validation.service.ts**

```typescript
import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';
import { tap } from 'rxjs/operators';
import * as faceapi from 'face-api.js';

export interface FaceVerificationResponse {
  success: boolean;
  token: string;
  expiresAt: string;
  message: string;
}

export interface FaceValidationStatus {
  hasValidToken: boolean;
  expiresAt?: string;
}

@Injectable({
  providedIn: 'root'
})
export class FaceValidationService {
  private apiUrl = 'http://localhost:7171/api/v1';
  private faceTokenSubject = new BehaviorSubject<string | null>(null);
  public faceToken$ = this.faceTokenSubject.asObservable();

  private modelsLoaded = false;

  constructor(private http: HttpClient) {}

  /**
   * Carrega os modelos do face-api.js
   * Deve ser chamado uma vez na inicialização do app
   */
  async loadFaceApiModels(): Promise<void> {
    if (this.modelsLoaded) return;

    const MODEL_URL = '/assets/models'; // Baixe modelos de https://github.com/justadudewhohacks/face-api.js-models

    await Promise.all([
      faceapi.nets.tinyFaceDetector.loadFromUri(MODEL_URL),
      faceapi.nets.faceLandmark68Net.loadFromUri(MODEL_URL),
      faceapi.nets.faceRecognitionNet.loadFromUri(MODEL_URL)
    ]);

    this.modelsLoaded = true;
    console.log('Face API models loaded');
  }

  /**
   * Detecta rosto em uma imagem
   */
  async detectFace(imageElement: HTMLVideoElement | HTMLImageElement): Promise<boolean> {
    await this.loadFaceApiModels();

    const detection = await faceapi
      .detectSingleFace(imageElement, new faceapi.TinyFaceDetectorOptions())
      .withFaceLandmarks()
      .withFaceDescriptor();

    return !!detection;
  }

  /**
   * Captura imagem da webcam como base64
   */
  captureImageFromVideo(video: HTMLVideoElement): string {
    const canvas = document.createElement('canvas');
    canvas.width = video.videoWidth;
    canvas.height = video.videoHeight;
    const ctx = canvas.getContext('2d');
    ctx?.drawImage(video, 0, 0);
    return canvas.toDataURL('image/jpeg', 0.95);
  }

  /**
   * Registra o rosto do usuário no backend
   */
  registerFace(imageBase64: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/face/register`, {
      image: imageBase64
    });
  }

  /**
   * Verifica o rosto e obtém token de validação
   */
  verifyFaceAndGetToken(imageBase64: string): Observable<FaceVerificationResponse> {
    return this.http.post<FaceVerificationResponse>(`${this.apiUrl}/face/verify`, {
      image: imageBase64
    }).pipe(
      tap(response => {
        if (response.success && response.token) {
          this.setFaceToken(response.token);
        }
      })
    );
  }

  /**
   * Verifica status da validação facial
   */
  checkFaceValidationStatus(): Observable<FaceValidationStatus> {
    return this.http.get<FaceValidationStatus>(`${this.apiUrl}/face/status`);
  }

  /**
   * Remove rosto do usuário
   */
  deleteFace(): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/face`).pipe(
      tap(() => this.clearFaceToken())
    );
  }

  /**
   * Define o token de validação facial
   */
  setFaceToken(token: string): void {
    localStorage.setItem('face_token', token);
    this.faceTokenSubject.next(token);
  }

  /**
   * Obtém o token de validação facial
   */
  getFaceToken(): string | null {
    return localStorage.getItem('face_token');
  }

  /**
   * Remove o token de validação facial
   */
  clearFaceToken(): void {
    localStorage.removeItem('face_token');
    this.faceTokenSubject.next(null);
  }

  /**
   * Verifica se tem token válido
   */
  hasValidToken(): boolean {
    return !!this.getFaceToken();
  }
}
```

---

## 📷 2. Componente de Captura Facial

**face-capture.component.ts**

```typescript
import { Component, OnInit, OnDestroy, Output, EventEmitter, ViewChild, ElementRef } from '@angular/core';
import { FaceValidationService } from '../../services/face-validation.service';

@Component({
  selector: 'app-face-capture',
  templateUrl: './face-capture.component.html',
  styleUrls: ['./face-capture.component.scss']
})
export class FaceCaptureComponent implements OnInit, OnDestroy {
  @ViewChild('video') videoElement!: ElementRef<HTMLVideoElement>;
  @Output() faceCaptured = new EventEmitter<string>();
  @Output() captureError = new EventEmitter<string>();

  isLoading = false;
  isCameraReady = false;
  faceDetected = false;
  private stream: MediaStream | null = null;
  private detectionInterval: any;

  constructor(private faceValidationService: FaceValidationService) {}

  async ngOnInit() {
    await this.startCamera();
  }

  ngOnDestroy() {
    this.stopCamera();
  }

  async startCamera() {
    try {
      this.isLoading = true;
      this.stream = await navigator.mediaDevices.getUserMedia({
        video: { facingMode: 'user', width: 640, height: 480 }
      });

      if (this.videoElement) {
        this.videoElement.nativeElement.srcObject = this.stream;
        await this.videoElement.nativeElement.play();
        this.isCameraReady = true;
        this.startFaceDetection();
      }
    } catch (error) {
      console.error('Erro ao acessar câmera:', error);
      this.captureError.emit('Erro ao acessar a câmera. Verifique as permissões.');
    } finally {
      this.isLoading = false;
    }
  }

  startFaceDetection() {
    // Detecta rosto a cada segundo
    this.detectionInterval = setInterval(async () => {
      if (this.videoElement && this.isCameraReady) {
        const video = this.videoElement.nativeElement;
        this.faceDetected = await this.faceValidationService.detectFace(video);
      }
    }, 1000);
  }

  async capture() {
    if (!this.videoElement || !this.isCameraReady) {
      this.captureError.emit('Câmera não está pronta');
      return;
    }

    const video = this.videoElement.nativeElement;

    // Verifica se há rosto detectado
    const hasFace = await this.faceValidationService.detectFace(video);

    if (!hasFace) {
      this.captureError.emit('Nenhum rosto detectado. Posicione seu rosto na câmera.');
      return;
    }

    // Captura imagem
    const imageBase64 = this.faceValidationService.captureImageFromVideo(video);
    this.faceCaptured.emit(imageBase64);
  }

  stopCamera() {
    if (this.detectionInterval) {
      clearInterval(this.detectionInterval);
    }

    if (this.stream) {
      this.stream.getTracks().forEach(track => track.stop());
      this.stream = null;
    }

    this.isCameraReady = false;
    this.faceDetected = false;
  }
}
```

**face-capture.component.html**

```html
<div class="face-capture-container">
  <div class="camera-wrapper">
    <video #video autoplay playsinline class="camera-view"></video>

    <div class="overlay">
      <div class="face-frame" [class.detected]="faceDetected"></div>
    </div>

    <div class="status-indicator" *ngIf="isCameraReady">
      <span class="badge" [class.badge-success]="faceDetected" [class.badge-warning]="!faceDetected">
        {{ faceDetected ? '✓ Rosto Detectado' : '⚠ Posicione seu rosto' }}
      </span>
    </div>
  </div>

  <div class="actions">
    <button
      class="btn btn-primary btn-lg"
      (click)="capture()"
      [disabled]="!isCameraReady || !faceDetected || isLoading">
      <span *ngIf="!isLoading">📸 Capturar</span>
      <span *ngIf="isLoading">⏳ Processando...</span>
    </button>
  </div>

  <div class="instructions">
    <p>✓ Olhe diretamente para a câmera</p>
    <p>✓ Mantenha o rosto bem iluminado</p>
    <p>✓ Evite óculos escuros ou máscaras</p>
  </div>
</div>
```

**face-capture.component.scss**

```scss
.face-capture-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1.5rem;
  padding: 2rem;
}

.camera-wrapper {
  position: relative;
  width: 640px;
  max-width: 100%;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.camera-view {
  width: 100%;
  height: auto;
  display: block;
}

.overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
}

.face-frame {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 300px;
  height: 350px;
  border: 3px solid #ffc107;
  border-radius: 50%;
  transition: border-color 0.3s;

  &.detected {
    border-color: #28a745;
    box-shadow: 0 0 20px rgba(40, 167, 69, 0.5);
  }
}

.status-indicator {
  position: absolute;
  top: 20px;
  right: 20px;
}

.badge {
  padding: 8px 16px;
  border-radius: 20px;
  font-weight: bold;
  font-size: 14px;

  &.badge-success {
    background: #28a745;
    color: white;
  }

  &.badge-warning {
    background: #ffc107;
    color: #000;
  }
}

.actions {
  display: flex;
  gap: 1rem;
}

.btn {
  padding: 12px 32px;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;

  &.btn-primary {
    background: #007bff;
    color: white;

    &:hover:not(:disabled) {
      background: #0056b3;
      transform: translateY(-2px);
    }

    &:disabled {
      background: #6c757d;
      cursor: not-allowed;
      opacity: 0.6;
    }
  }
}

.instructions {
  text-align: center;
  color: #666;
  font-size: 14px;

  p {
    margin: 4px 0;
  }
}
```

Continua...
