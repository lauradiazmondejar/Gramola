import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { UserService } from '../user'; // Asegúrate de que la ruta sea correcta

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './register.html',
  styleUrl: './register.css'
})
export class Register {

  // Nuevos campos
  bar?: string;
  email?: string;
  pwd1?: string;
  pwd2?: string;
  clientId?: string;
  clientSecret?: string;
  lat?: number;
  lon?: number;
  ubicacionMsg: string = '';
  address: string = '';

  registroOK: boolean = false;
  registroKO: boolean = false;

  constructor(private service: UserService) { }

  obtenerUbicacion() {
    if (navigator.geolocation) {
      this.ubicacionMsg = 'Obteniendo coordenadas...';
      navigator.geolocation.getCurrentPosition(
        (position) => {
          this.lat = position.coords.latitude;
          this.lon = position.coords.longitude;
          this.ubicacionMsg = `✅ Ubicación guardada: ${this.lat.toFixed(4)}, ${this.lon.toFixed(4)}`;
        },
        (error) => {
          console.error(error);
          this.ubicacionMsg = '❌ Error: No se pudo obtener la ubicación. Permite el acceso GPS.';
        }
      );
    } else {
      this.ubicacionMsg = 'Tu navegador no soporta geolocalización.';
    }
  }

  registrar() {
    this.registroOK = false;
    this.registroKO = false;

    // Validación básica
    if (!this.bar || !this.email || !this.pwd1 || !this.clientId || !this.clientSecret) {
      alert('Por favor, rellena todos los campos.');
      return;
    }

    if (this.pwd1 != this.pwd2) {
      alert('Las contraseñas no coinciden');
      return;
    }

    // Llamada al servicio con TODOS los datos
    this.service.register(this.bar!, this.email!, this.pwd1!, this.pwd2!, this.clientId!, this.clientSecret!, this.lat, this.lon)
      .subscribe({
        next: (response) => {
          console.log('Registro exitoso', response);
          this.registroOK = true;
          this.registroKO = false;
        },
        error: (error) => {
          console.error('Error en el registro', error);
          this.registroKO = true;
          this.registroOK = false;
        }
      });
  }

  async obtenerCoordenadasPorDireccion() {
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
}
