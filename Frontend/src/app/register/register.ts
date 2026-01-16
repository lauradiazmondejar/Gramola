import { Component, ViewChild, ElementRef, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { UserService } from '../user';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './register.html',
  styleUrl: './register.css'
})
// Registro de bar: datos de cuenta, Spotify, geolocalizacion y firma.
export class Register implements AfterViewInit {
  bar?: string;
  email?: string;
  pwd1?: string;
  pwd2?: string;
  clientId?: string;
  clientSecret?: string;
  lat?: number;
  lon?: number;
  ubicacionMsg = '';
  address = '';
  errorMsg?: string;
  showPwd1 = false;
  showPwd2 = false;
  showClientSecret = false;

  registroOK = false;
  registroKO = false;

  @ViewChild('canvasFirma') canvasRef!: ElementRef;
  private cx!: CanvasRenderingContext2D;
  private isDrawing = false;

  constructor(private service: UserService) {}

  obtenerUbicacion() {
    // Pide coordenadas al navegador para asociarlas al bar.
    if (navigator.geolocation) {
      this.ubicacionMsg = 'Obteniendo coordenadas...';
      navigator.geolocation.getCurrentPosition(
        (position) => {
          this.lat = position.coords.latitude;
          this.lon = position.coords.longitude;
          this.ubicacionMsg = `Ok. Ubicación guardada: ${this.lat.toFixed(4)}, ${this.lon.toFixed(4)}`;
        },
        (error) => {
          console.error(error);
          this.ubicacionMsg = 'Error: No se pudo obtener la ubicación. Permite el acceso GPS.';
        }
      );
    } else {
      this.ubicacionMsg = 'Tu navegador no soporta geolocalización.';
    }
  }

  registrar() {
    // Valida los campos y llama al backend para crear el usuario.
    this.registroOK = false;
    this.registroKO = false;
    this.errorMsg = undefined;

    const signatureImage = this.canvasRef.nativeElement.toDataURL();

    if (!this.bar || !this.email || !this.pwd1 || !this.clientId || !this.clientSecret) {
      this.errorMsg = 'Por favor, rellena todos los campos.';
      return;
    }

    if (this.pwd1 !== this.pwd2) {
      this.errorMsg = 'Las contraseñas no coinciden';
      return;
    }

    this.service.register(
      this.bar!,
      this.email!,
      this.pwd1!,
      this.pwd2!,
      this.clientId!,
      this.clientSecret!,
      this.lat,
      this.lon,
      signatureImage
    ).subscribe({
      next: (response) => {
        console.log('Registro exitoso', response);
        this.registroOK = true;
        this.registroKO = false;
      },
      error: (error) => {
        console.error('Error en el registro', error);
        this.errorMsg = 'Error en el registro. Revisa los datos o intenta de nuevo.';
        this.registroKO = true;
        this.registroOK = false;
      }
    });
  }

  async obtenerCoordenadasPorDireccion() {
    // Consulta Nominatim para traducir la direccion en coordenadas.
    const query = this.address?.trim();
    if (!query) {
      this.ubicacionMsg = 'Por favor, escribe una dirección primero.';
      return;
    }

    this.ubicacionMsg = 'Buscando dirección en el mapa...';

    try {
      const url = `https://nominatim.openstreetmap.org/search?format=json&limit=1&q=${encodeURIComponent(query)}`;
      const resp = await fetch(url, { headers: { 'Accept-Language': 'es' } });

      if (!resp.ok) {
        throw new Error(`HTTP ${resp.status}`);
      }

      const data = await resp.json();
      if (data && data.length > 0) {
        this.lat = parseFloat(data[0].lat);
        this.lon = parseFloat(data[0].lon);
        this.ubicacionMsg = `Dirección encontrada: ${this.lat.toFixed(4)}, ${this.lon.toFixed(4)}`;
      } else {
        this.ubicacionMsg = 'No se encontró esa dirección. Prueba con calle, número y ciudad.';
        this.lat = undefined;
        this.lon = undefined;
      }
    } catch (error) {
      console.error('Error geocodificando dirección', error);
      this.ubicacionMsg = 'Error al conectar con el servicio de mapas. Reintenta en unos segundos.';
      this.lat = undefined;
      this.lon = undefined;
    }
  }

  ngAfterViewInit(): void {
    // Prepara el canvas para capturar la firma con el raton.
    const canvasEl: HTMLCanvasElement = this.canvasRef.nativeElement;
    this.cx = canvasEl.getContext('2d')!;

    this.cx.lineWidth = 3;
    this.cx.lineCap = 'round';
    this.cx.strokeStyle = '#000';

    canvasEl.addEventListener('mousedown', this.startDrawing.bind(this));
    canvasEl.addEventListener('mousemove', this.draw.bind(this));
    canvasEl.addEventListener('mouseup', this.stopDrawing.bind(this));
    canvasEl.addEventListener('mouseleave', this.stopDrawing.bind(this));
  }

  startDrawing(e: MouseEvent) {
    // Inicia el trazo en el canvas cuando se pulsa el raton.
    this.isDrawing = true;
    const rect = this.canvasRef.nativeElement.getBoundingClientRect();
    this.cx.beginPath();
    this.cx.moveTo(e.clientX - rect.left, e.clientY - rect.top);
  }

  draw(e: MouseEvent) {
    // Traza lineas siguiendo el puntero mientras se arrastra.
    if (!this.isDrawing) return;
    const rect = this.canvasRef.nativeElement.getBoundingClientRect();
    this.cx.lineTo(e.clientX - rect.left, e.clientY - rect.top);
    this.cx.stroke();
  }

  stopDrawing() {
    // Cierra el trazo cuando se levanta el raton o se sale del canvas.
    if (this.isDrawing) {
      this.cx.closePath();
      this.isDrawing = false;
    }
  }

  limpiarFirma() {
    // Borra la firma actual del canvas.
    const canvas = this.canvasRef.nativeElement;
    this.cx.clearRect(0, 0, canvas.width, canvas.height);
  }
}








