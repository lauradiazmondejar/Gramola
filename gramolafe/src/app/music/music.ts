import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SpotiService } from '../spoti';
import { HttpClient } from '@angular/common/http';
import { PaymentService } from '../payment';

declare let Stripe: any;

@Component({
  selector: 'app-music',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './music.html',
  styleUrl: './music.css'
})
export class Music implements OnInit {

  devices: any[] = [];
  playlists: any[] = [];
  tracks: any[] = [];
  currentDevice: any;
  searchTerm = '';
  errorMsg = '';
  successMsg = '';

  stripe = Stripe('pk_test_51SIV2CRfAGkgoJHtjzPD344TigvazTauIQXxhm98Tk78mAuc7H79dD9XWvSO8cIfKNG8DS5MvEw5ldw6LhfUuEsg00QDV18Afz'); // <--- ¡PON TU CLAVE PÚBLICA!
  elements: any;
  card: any;
  clientSecret: string = '';
  pagandoCancion: boolean = false; // Para mostrar/ocultar el modal de pago
  cancionSeleccionada: any = null; // La canción que se quiere comprar
  precio: number = 50; // Precio en céntimos (0.50€)

  constructor(
    private spotiService: SpotiService,
    private http: HttpClient, // <--- Inyectamos HttpClient
    private paymentService: PaymentService
  ) {}

  ngOnInit(): void {
    this.cargarDispositivos();
    this.cargarPlaylists();
  }

  cargarDispositivos() {
    this.spotiService.getDevices().subscribe({
      next: (data) => {
        this.devices = data.devices || [];
        this.currentDevice = this.devices.find(d => d.is_active);
      },
      error: (err) => {
        console.error('Error cargando dispositivos', err);
        this.errorMsg = 'No se pudieron cargar los dispositivos. Revisa que Spotify este abierto.';
      }
    });
  }

  cargarPlaylists() {
    this.spotiService.getPlaylists().subscribe({
      next: (data) => {
        this.playlists = data.items || [];
      },
      error: (err) => {
        console.error('Error cargando playlists', err);
      }
    });
  }

  buscar() {
    if (!this.searchTerm) return;

    this.spotiService.search(this.searchTerm).subscribe({
      next: (data) => {
        this.tracks = data.tracks.items;
        this.errorMsg = '';
      },
      error: (err) => {
        console.error(err);
        this.errorMsg = 'Error al buscar canciones.';
      }
    });
  }

  add(track: any) {
    if (!this.currentDevice) {
      this.errorMsg = 'No hay ningun dispositivo activo. Dale al Play en Spotify primero.';
      return;
    }

    this.spotiService.addToQueue(track.uri, this.currentDevice.id).subscribe({
      next: () => {
        this.successMsg = `"${track.name}" anadida a la cola.`;
        this.guardarEnHistorial(track);
        setTimeout(() => this.successMsg = '', 3000);
      },
      error: (err) => {
        console.error(err);
        // Mostrar el mensaje real de Spotify para saber la causa (premium, sin dispositivo activo, etc.)
        const apiMsg = err?.error?.error?.message || err?.error?.message || '';
        if (err.status === 404) {
          this.errorMsg = 'No hay dispositivo activo en Spotify. Dale al Play en algún dispositivo y reintenta.';
        } else if (err.status === 403) {
          this.errorMsg = 'Spotify respondió 403 (Premium requerido o permiso denegado). Inicia sesión con la cuenta Premium y repite el login.';
        } else {
          this.errorMsg = apiMsg || 'No se pudo anadir. Revisa Spotify y vuelve a intentar.';
        }
      }
    });
  }

  // --- LÓGICA DE PAGO ---

  // 1. El usuario hace clic en "Poner" -> Preparamos el cobro
  solicitarCancion(track: any) {
    this.cancionSeleccionada = track;
    this.pagandoCancion = true; // Mostramos el modal
    this.errorMsg = '';

    // Pedimos al backend el "intento de pago" por el precio de la canción
    this.paymentService.prepay(this.precio).subscribe({
      next: (response: any) => {
        const data = JSON.parse(response);
        this.clientSecret = data.client_secret;
        
        // Montamos el formulario de tarjeta (con un pequeño retardo para que el DIV exista)
        setTimeout(() => this.montarFormularioStripe(), 100);
      },
      error: (err) => {
        console.error(err);
        this.errorMsg = 'Error iniciando el pago';
        this.pagandoCancion = false;
      }
    });
  }

  montarFormularioStripe() {
    this.elements = this.stripe.elements();
    const style = {
      base: { 
        color: '#32325d', fontFamily: 'Arial, sans-serif', fontSmoothing: 'antialiased', fontSize: '16px', 
        '::placeholder': { color: '#aab7c4' } 
      }
    };
    this.card = this.elements.create('card', {style: style});
    this.card.mount('#card-element-song'); // Ojo al ID nuevo
  }

  // 2. El usuario hace clic en "Pagar y Poner" -> Confirmamos con Stripe
  confirmarPago() {
    this.stripe.confirmCardPayment(this.clientSecret, {
      payment_method: { card: this.card }
    }).then((result: any) => {
      if (result.error) {
        this.errorMsg = result.error.message;
      } else {
        if (result.paymentIntent.status === 'succeeded') {
          // 3. ¡Pago OK! -> Ejecutamos la lógica de añadir la canción
          this.ejecutarPedido();
        }
      }
    });
  }

  cancelarPago() {
    this.pagandoCancion = false;
    this.cancionSeleccionada = null;
    if(this.card) this.card.destroy(); // Limpiamos el formulario
  }

  // 4. Lógica final (lo que antes hacías directamente)
  ejecutarPedido() {
    // Ocultamos el pago
    this.pagandoCancion = false;
    
    if (!this.currentDevice) {
      this.errorMsg = 'No hay dispositivo activo.';
      return;
    }

    // Añadimos a Spotify
    this.spotiService.addToQueue(this.cancionSeleccionada.uri, this.currentDevice.id).subscribe({
      next: () => {
        this.successMsg = `¡"${this.cancionSeleccionada.name}" pagada y en cola!`;
        
        // Guardamos en BD
        this.guardarEnHistorial(this.cancionSeleccionada);
        
        this.cancionSeleccionada = null;
        setTimeout(() => this.successMsg = '', 4000);
      },
      error: (err) => {
        this.errorMsg = 'Se cobró pero falló Spotify. Contacta con el camarero.';
      }
    });
  }

  guardarEnHistorial(track: any) {
    const clientId = sessionStorage.getItem('clientId'); // Recuperamos quién es el bar
    
    const body = {
        title: track.name,
        artist: track.artists[0].name,
        uri: track.uri,
        clientId: clientId
    };

    this.http.post('http://127.0.0.1:8080/music/add', body).subscribe({
        next: () => console.log('Canción guardada en BD'),
        error: (e) => console.error('Error guardando en BD', e)
    });
}
}
