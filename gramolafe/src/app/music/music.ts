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
  queue: any[] = [];
  nowPlaying: any;
  tracks: any[] = [];
  currentDevice: any;
  searchTerm = '';
  errorMsg = '';
  successMsg = '';
  userSignature: string = '';
  currentPlaylistName?: string;
  currentPlaylistTracks: any[] = [];
  isPlaying = false;
  simulatePlayback = true; // fallback sin Premium
  backendBase = 'http://127.0.0.1:8080';

  stripe = Stripe('pk_test_51SIV2CRfAGkgoJHtjzPD344TigvazTauIQXxhm98Tk78mAuc7H79dD9XWvSO8cIfKNG8DS5MvEw5ldw6LhfUuEsg00QDV18Afz');
  elements: any;
  card: any;
  clientSecret: string = '';
  internalTransactionId: string = '';
  pagandoCancion: boolean = false;
  cancionSeleccionada: any = null;
  songPriceCents?: number;

  constructor(
    private spotiService: SpotiService,
    private http: HttpClient,
    private paymentService: PaymentService
  ) {}

  ngOnInit(): void {
    // Carga datos iniciales de Spotify y precios al entrar en la vista
    this.userSignature = sessionStorage.getItem('signature') || '';
    this.cargarDispositivos();
    this.cargarPlaylists();
    this.cargarCola();
    this.cargarPlaybackActual();
    this.cargarPrecioCancion();
  }

  logout() {
    sessionStorage.clear();
    window.location.href = '/login';
  }

  cargarDispositivos() {
    // Obtiene los dispositivos Spotify disponibles para reproducir
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
    // Lista las playlists del usuario para mostrarlas
    this.spotiService.getPlaylists().subscribe({
      next: (data) => {
        this.playlists = data.items || [];
      },
      error: (err) => {
        console.error('Error cargando playlists', err);
      }
    });
  }

  cargarCola() {
    // Recupera la cola y la cancion en reproduccion; simula si no hay permiso
    this.spotiService.getQueue().subscribe({
      next: (data) => {
        this.nowPlaying = data.currently_playing;
        this.queue = data.queue || [];
      },
      error: (err) => {
        console.error('No se pudo cargar la cola', err);
        if (this.simulatePlayback) {
          this.cargarColaLocal();
        }
      }
    });
  }

  cargarPlaybackActual() {
    // Consulta lo que se esta reproduciendo y la playlist asociada
    this.spotiService.getCurrentPlayback().subscribe({
      next: (data) => {
        this.isPlaying = !!data?.is_playing;
        if (data?.item) {
          this.nowPlaying = data.item;
        }
        const ctx = data?.context;
        if (ctx?.type === 'playlist' && ctx.uri) {
          const playlistId = this.extraerPlaylistId(ctx.uri);
          if (playlistId) {
            this.cargarPlaylistActual(playlistId);
          }
        }
      },
      error: (err) => {
        console.error('No se pudo obtener el estado de reproduccion', err);
      }
    });
  }

  private extraerPlaylistId(uri: string): string | undefined {
    const parts = uri.split(':');
    return parts.length === 3 ? parts[2] : undefined;
  }

  cargarPlaylistActual(playlistId: string) {
    this.spotiService.getPlaylist(playlistId).subscribe({
      next: (playlist) => {
        this.currentPlaylistName = playlist?.name;
        this.currentPlaylistTracks = (playlist?.tracks?.items || []).map((it: any) => it.track);
      },
      error: (err) => {
        console.error('No se pudo cargar la playlist actual', err);
      }
    });
  }

  reproducirPlaylist(list: any) {
    // Lanza la reproduccion de una playlist en el dispositivo activo
    if (!this.currentDevice) {
      this.errorMsg = 'No hay ningun dispositivo activo.';
      return;
    }
    this.errorMsg = '';
    this.spotiService.startPlaylist(list.uri, this.currentDevice.id).subscribe({
      next: () => {
        this.currentPlaylistName = list.name;
        this.currentPlaylistTracks = [];
        this.isPlaying = true;
        setTimeout(() => {
          this.cargarPlaybackActual();
          this.cargarCola();
        }, 400);
      },
      error: (err) => {
        console.error(err);
        this.errorMsg = 'Spotify no permitio iniciar la playlist (verifica cuenta Premium y permisos).';
      }
    });
  }

  reanudarReproduccion() {
    // Reanuda la reproduccion en Spotify si estaba pausada
    if (!this.currentDevice) {
      this.errorMsg = 'No hay dispositivo para reproducir.';
      return;
    }
    this.spotiService.resumePlayback(this.currentDevice.id).subscribe({
      next: () => {
        this.isPlaying = true;
        this.errorMsg = '';
        this.cargarPlaybackActual();
      },
      error: (err) => {
        console.error(err);
        this.errorMsg = 'No se pudo reanudar la reproduccion en Spotify.';
      }
    });
  }

  cargarPrecioCancion() {
    // Consulta al backend el precio de una cancion individual
    this.paymentService.getPrice('song').subscribe({
      next: (price: any) => {
        this.songPriceCents = price.amount;
      },
      error: (err) => {
        console.error('No se pudo obtener el precio de cancion', err);
      }
    });
  }

  buscar() {
    // Usa la API de Spotify para buscar canciones por texto
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
    // Valida ubicacion/pago y encola la cancion en Spotify
    if (!this.currentDevice) {
      this.errorMsg = 'No hay ningun dispositivo activo. Dale al Play en Spotify primero.';
      return;
    }

    this.errorMsg = '';

    this.guardarEnHistorial(
      track,
      () => {
        this.spotiService.addToQueue(track.uri, this.currentDevice.id).subscribe({
          next: () => {
            this.successMsg = `"${track.name}" anadida a la cola.`;
            setTimeout(() => this.successMsg = '', 3000);
            this.cargarCola();
          },
          error: (err) => {
            console.error(err);
            const apiMsg = err?.error?.error?.message || err?.error?.message || '';
            if (err.status === 404) {
              this.errorMsg = 'No hay dispositivo activo en Spotify. Dale al Play en algun dispositivo y reintenta.';
              if (this.simulatePlayback) {
                this.simularColaLocal(track);
              }
            } else if (err.status === 403) {
              this.errorMsg = 'Spotify respondio 403 (Premium requerido o permiso denegado). Inicia sesion con la cuenta Premium y repite el login.';
              if (this.simulatePlayback) {
                this.simularColaLocal(track);
              }
            } else {
              this.errorMsg = apiMsg || 'No se pudo anadir. Revisa Spotify y vuelve a intentar.';
            }
          }
        });
      },
      (err) => {
        console.error('Error validando ubicacion en backend', err);
        if (err?.status === 403) {
          this.errorMsg = 'Estas demasiado lejos del bar. La cancion no se envia a Spotify.';
        } else if (err?.status === 400) {
          this.errorMsg = 'Este bar requiere que compartas ubicacion. Activa tu GPS.';
        } else {
          this.errorMsg = 'No se pudo validar la ubicacion. Intentalo de nuevo.';
        }
      }
    );
  }

  solicitarCancion(track: any) {
    // Dispara el flujo de pago por cancion individual
    this.cancionSeleccionada = track;
    this.pagandoCancion = true;
    this.errorMsg = '';

    const email = sessionStorage.getItem('email') || '';
    const barName = sessionStorage.getItem('bar') || '';
    this.paymentService.prepay('song', email, barName, 'song').subscribe({
      next: (response: any) => {
        const data = JSON.parse(response);
        this.clientSecret = data.client_secret;
        this.internalTransactionId = data.id;
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
    // Prepara el card element de Stripe para el pago de canciones
    this.elements = this.stripe.elements();
    const style = {
      base: {
        color: '#32325d', fontFamily: 'Arial, sans-serif', fontSmoothing: 'antialiased', fontSize: '16px',
        '::placeholder': { color: '#aab7c4' }
      }
    };
    this.card = this.elements.create('card', { style: style });
    this.card.mount('#card-element-song');
  }

  confirmarPago() {
    // Confirma el pago de cancion en Stripe y luego en backend
    if (!this.clientSecret) {
      this.errorMsg = 'No hay intento de pago activo';
      return;
    }
    // Evitamos doble submit
    const payButton = document.querySelector('.btn-pay') as HTMLButtonElement | null;
    if (payButton) payButton.disabled = true;

    this.stripe.confirmCardPayment(this.clientSecret, {
      payment_method: { card: this.card }
    }).then((result: any) => {
      if (result.error) {
        this.errorMsg = result.error.message;
        if (payButton) payButton.disabled = false;
      } else {
        if (result.paymentIntent.status === 'succeeded') {
          // Confirmamos en backend antes de añadir a Spotify
          this.paymentService.confirm(result, this.internalTransactionId, null).subscribe({
            next: () => this.ejecutarPedido(),
            error: (err) => {
              console.error(err);
              this.errorMsg = 'Pago ok en Stripe, pero el servidor no lo validó.';
              if (payButton) payButton.disabled = false;
            }
          });
        }
      }
    });
  }

  cancelarPago() {
    // Sale del modal de pago y limpia el formulario de tarjeta
    this.pagandoCancion = false;
    this.cancionSeleccionada = null;
    if (this.card) this.card.destroy();
  }

  ejecutarPedido() {
    // Tras pagar, valida ubicacion y encola la cancion solicitada
    this.pagandoCancion = false;
    
    if (!this.currentDevice) {
      this.errorMsg = 'No hay dispositivo activo.';
      return;
    }

    this.errorMsg = '';

    this.guardarEnHistorial(
      this.cancionSeleccionada,
      () => {
            this.spotiService.addToQueue(this.cancionSeleccionada.uri, this.currentDevice.id).subscribe({
              next: () => {
                this.successMsg = `"${this.cancionSeleccionada.name}" pagada y en cola!`;
                this.cancionSeleccionada = null;
                setTimeout(() => this.successMsg = '', 4000);
                this.cargarCola();
              },
              error: (err) => {
                this.errorMsg = 'Se cobro pero fallo Spotify. Contacta con el camarero.';
                if (this.simulatePlayback && this.cancionSeleccionada) {
                  this.simularColaLocal(this.cancionSeleccionada);
                }
              }
            });
          },
          (err) => {
            if (err?.status === 403) {
          this.errorMsg = 'Estas demasiado lejos del bar. No se anade a la cola.';
        } else if (err?.status === 400) {
          this.errorMsg = 'Este bar requiere que compartas ubicacion.';
        } else {
          this.errorMsg = 'No se pudo validar la ubicacion. Reintenta.';
        }
        this.cancionSeleccionada = null;
      }
    );
  }

  guardarEnHistorial(track: any, onOk?: () => void, onError?: (err: any) => void) {
    // Envia la peticion de cancion al backend incluyendo ubicacion si esta disponible
    const clientId = sessionStorage.getItem('clientId');
    const email = sessionStorage.getItem('email');
    const barName = sessionStorage.getItem('bar');

    if (!clientId || !email) {
        this.errorMsg = 'Sesion invalida. Vuelve a iniciar sesion.';
        if (onError) { onError({ status: 400 }); }
        return;
    }
    
    const enviarAlBackend = (lat?: number, lon?: number) => {
        const body = {
            title: track.name,
            artist: track.artists[0].name,
            uri: track.uri,
            clientId: clientId,
            email: email,
            bar: barName,
            lat: lat,
            lon: lon
        };

        this.http.post('http://127.0.0.1:8080/music/add', body).subscribe({
            next: () => {
                console.log('Cancion validada en backend.');
                if (onOk) { onOk(); }
            },
            error: (e) => {
                console.error('Error guardando en BD', e);
                if (onError) { onError(e); }
                if (e.status === 403) {
                    alert('Estas demasiado lejos del bar.');
                } else if (e.status === 400) {
                    alert('Hace falta tu ubicacion para pedir musica en este bar.');
                }
            }
        });
    };

    if (navigator.geolocation) {
        navigator.geolocation.getCurrentPosition(
            (pos) => {
                enviarAlBackend(pos.coords.latitude, pos.coords.longitude);
            },
            () => {
                console.warn('No se pudo geolocalizar al cliente, enviando sin datos...');
                enviarAlBackend();
            }
        );
    } else {
        enviarAlBackend();
    }
  }

  private cargarColaLocal() {
    // Carga la cola desde el backend para simular reproduccion sin Premium
    const email = sessionStorage.getItem('email');
    if (!email) { return; }
    this.http.get(`${this.backendBase}/music/queue?email=${encodeURIComponent(email)}`).subscribe({
      next: (songs: any) => {
        // Normalizamos al formato usado en la vista (name + artists[0].name)
        const mapped = (songs as any[]).map((s: any) => ({
          name: s.title,
          artists: [{ name: s.artist }],
          uri: s.uri
        }));
        this.queue = mapped;
        this.nowPlaying = mapped[0] || null;
        this.successMsg = this.successMsg || 'Reproduccion simulada en cola local.';
      },
      error: (err) => {
        console.error('No se pudo cargar la cola local', err);
      }
    });
  }

  private simularColaLocal(track: any) {
    // Cuando falta Premium, a�ade la cancion a la cola local de manera simulada
    this.cargarColaLocal();
    this.successMsg = `"${track.name}" añadida en modo simulado (sin Premium).`;
    setTimeout(() => this.successMsg = '', 4000);
  }
}


















