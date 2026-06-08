import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SpotiService } from '../spoti';
import { MusicService } from '../music';
import { PaymentService } from '../payment';
import { UserService } from '../user';
import { environment } from '../../environments/environment';

declare let Stripe: any;

@Component({
  selector: 'app-music',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './music.html',
  styleUrl: './music.css'
})
// Vista principal de gramola: Spotify, cola y pagos por cancion.
export class Music implements OnInit {

  devices: any[] = [];
  playlists: any[] = [];
  catalogoBar: any[] = [];
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
  simulatePlayback = true; // Fallback sin Premium.
  stripe = Stripe(environment.stripePublicKey);
  elements: any;
  card: any;
  clientSecret: string = '';
  internalTransactionId: string = '';
  pagandoCancion: boolean = false;
  cancionSeleccionada: any = null;
  songPriceCents?: number;
  confirmandoPlaylist = false;
  playlistObjetivo: any = null;
  playlistPassword = '';
  playlistError = '';
  verificandoPlaylist = false;
  confirmandoCatalogo = false;
  trackParaCatalogo: any = null;
  catalogoPassword = '';
  catalogoError = '';
  verificandoCatalogo = false;
  mostrandoModalUbicacion = false;
  tituloModalUbicacion = 'Ubicacion requerida';
  mensajeModalUbicacion = '';

  constructor(
    private spotiService: SpotiService,
    private musicService: MusicService,
    private paymentService: PaymentService,
    private userService: UserService
  ) {}

  ngOnInit(): void {
    // Carga datos iniciales: catalogo del bar desde el backend, Spotify y precios
    this.userSignature = sessionStorage.getItem('signature') || '';
    this.cargarCatalogoBar();
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

  cargarCatalogoBar() {
    // Carga la lista de canciones del bar desde el backend
    const email = sessionStorage.getItem('email');
    if (!email) return;
    this.musicService.getQueue(email).subscribe({
      next: (songs: any[]) => {
        this.catalogoBar = songs.map((s: any) => ({
          name: s.title,
          artists: [{ name: s.artist }],
          uri: s.uri
        }));
      },
      error: (err: any) => console.error('No se pudo cargar el catálogo del bar', err)
    });
  }

  agregarAlCatalogo(track: any) {
    // Abre el modal de verificación; si el propietario confirma, la cancion se añade gratis
    this.trackParaCatalogo = track;
    this.catalogoPassword = '';
    this.catalogoError = '';
    this.confirmandoCatalogo = true;
  }

  confirmarAgregarAlCatalogo() {
    if (!this.catalogoPassword) {
      this.catalogoError = 'Introduce la contraseña del bar.';
      return;
    }
    const email = sessionStorage.getItem('email');
    const clientId = sessionStorage.getItem('clientId');
    if (!email || !clientId) {
      this.catalogoError = 'Sesión inválida. Vuelve a iniciar sesión.';
      return;
    }
    this.verificandoCatalogo = true;
    this.catalogoError = '';
    const body = {
      title: this.trackParaCatalogo.name,
      artist: this.trackParaCatalogo.artists[0].name,
      uri: this.trackParaCatalogo.uri,
      email: email,
      clientId: clientId,
      password: this.catalogoPassword
    };
    this.musicService.addSongFree(body).subscribe({
      next: () => {
        this.verificandoCatalogo = false;
        this.confirmandoCatalogo = false;
        this.successMsg = `"${this.trackParaCatalogo.name}" añadida al catálogo del bar.`;
        this.trackParaCatalogo = null;
        this.catalogoPassword = '';
        setTimeout(() => this.successMsg = '', 3000);
        this.cargarCatalogoBar();
      },
      error: (err: any) => {
        this.verificandoCatalogo = false;
        this.catalogoError = err?.error?.message || 'Contraseña incorrecta o error al añadir.';
      }
    });
  }

  cancelarAgregarAlCatalogo() {
    this.confirmandoCatalogo = false;
    this.trackParaCatalogo = null;
    this.catalogoPassword = '';
    this.catalogoError = '';
    this.verificandoCatalogo = false;
  }

  cargarDispositivos() {
    // Obtiene los dispositivos Spotify disponibles para reproducir.
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
    // Lista las playlists del usuario para mostrarlas.
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
    // Recupera la cola y la cancion en reproduccion; simula si no hay permiso.
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
    // Consulta lo que se esta reproduciendo y la playlist asociada.
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
    // Lanza la reproduccion de una playlist en el dispositivo activo.
    if (!this.currentDevice) {
      this.errorMsg = 'No hay ningun dispositivo activo.';
      return;
    }

    const email = sessionStorage.getItem('email');
    if (!email) {
      this.errorMsg = 'Sesion invalida. Vuelve a iniciar sesion.';
      return;
    }

    this.errorMsg = '';
    this.playlistObjetivo = list;
    this.playlistPassword = '';
    this.playlistError = '';
    this.confirmandoPlaylist = true;
  }

  reproducirCatalogo() {
    if (!this.currentDevice) {
      this.errorMsg = 'No hay ningún dispositivo activo.';
      return;
    }
    if (this.catalogoBar.length === 0) {
      this.errorMsg = 'El catálogo está vacío. Añade canciones primero.';
      return;
    }
    // Reutiliza el modal de verificación de playlists marcando que es el catalogo
    this.playlistObjetivo = { name: 'Catálogo del bar', esCatalogo: true };
    this.playlistPassword = '';
    this.playlistError = '';
    this.confirmandoPlaylist = true;
  }

  confirmarReproduccionPlaylist() {
    if (!this.playlistObjetivo) {
      this.playlistError = 'No hay playlist seleccionada.';
      return;
    }

    const email = sessionStorage.getItem('email');
    if (!email) {
      this.playlistError = 'Sesion invalida. Vuelve a iniciar sesion.';
      return;
    }

    if (!this.playlistPassword) {
      this.playlistError = 'Introduce la contraseña del bar.';
      return;
    }

    this.playlistError = '';
    this.verificandoPlaylist = true;
    this.userService.verifyPassword(email, this.playlistPassword).subscribe({
      next: () => {
        const list = this.playlistObjetivo;
        this.verificandoPlaylist = false;
        this.confirmandoPlaylist = false;
        this.playlistObjetivo = null;
        this.playlistPassword = '';
        if (list?.esCatalogo) {
          this.iniciarReproduccionCatalogo();
        } else {
          this.iniciarReproduccionPlaylist(list);
        }
      },
      error: (err) => {
        console.error(err);
        this.verificandoPlaylist = false;
        this.playlistError = 'Contraseña incorrecta o no autorizada.';
      }
    });
  }

  cancelarConfirmacionPlaylist() {
    this.confirmandoPlaylist = false;
    this.playlistObjetivo = null;
    this.playlistPassword = '';
    this.playlistError = '';
    this.verificandoPlaylist = false;
  }

  abrirModalUbicacion(mensaje: string, titulo: string = 'Ubicacion requerida') {
    this.tituloModalUbicacion = titulo;
    this.mensajeModalUbicacion = mensaje;
    this.mostrandoModalUbicacion = true;
  }

  cerrarModalUbicacion() {
    this.mostrandoModalUbicacion = false;
    this.mensajeModalUbicacion = '';
  }
  private iniciarReproduccionPlaylist(list: any) {
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

  private iniciarReproduccionCatalogo() {
    const uris = this.catalogoBar.map((s: any) => s.uri).filter(Boolean);
    if (uris.length === 0) {
      this.errorMsg = 'No hay canciones válidas en el catálogo para reproducir.';
      return;
    }
    this.spotiService.startTracks(uris, this.currentDevice.id).subscribe({
      next: () => {
        this.currentPlaylistName = 'Catálogo del bar';
        this.currentPlaylistTracks = [...this.catalogoBar];
        this.isPlaying = true;
        setTimeout(() => {
          this.cargarPlaybackActual();
          this.cargarCola();
        }, 400);
      },
      error: (err) => {
        console.error(err);
        this.errorMsg = 'Spotify no pudo reproducir el catálogo (verifica cuenta Premium y permisos).';
      }
    });
  }

  reanudarReproduccion() {
    // Reanuda la reproduccion en Spotify si estaba pausada.
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
    // Consulta el precio de cancion del bar
    const email = sessionStorage.getItem('email') || undefined;
    this.paymentService.getPrice('song', email).subscribe({
      next: (price: any) => {
        this.songPriceCents = price.amount;
      },
      error: (err) => {
        console.error('No se pudo obtener el precio de cancion', err);
      }
    });
  }

  buscar() {
    // Usa la API de Spotify para buscar canciones por texto.
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
    // Valida ubicacion/pago y encola la cancion en Spotify.
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
    // Dispara el flujo de pago por cancion individual.
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
    // Prepara el card element de Stripe para el pago de canciones.
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
    // Confirma el pago de cancion en Stripe y luego en backend.
    if (!this.clientSecret) {
      this.errorMsg = 'No hay intento de pago activo';
      return;
    }
    // Evitamos doble submit.
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
          // Confirmamos en backend antes de anadir a Spotify.
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
    // Sale del modal de pago y limpia el formulario de tarjeta.
    this.pagandoCancion = false;
    this.cancionSeleccionada = null;
    if (this.card) this.card.destroy();
  }

  ejecutarPedido() {
    // Tras pagar, valida ubicacion y encola la cancion solicitada.
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
    // Envia la peticion de cancion al backend incluyendo ubicacion si esta disponible.
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

        this.musicService.addSong(body).subscribe({
            next: () => {
                console.log('Cancion validada en backend.');
                if (onOk) { onOk(); }
            },
            error: (e: any) => {
                console.error('Error guardando en BD', e);
                if (onError) { onError(e); }
                if (e.status === 403) {
                    this.abrirModalUbicacion('Estas demasiado lejos del bar.');
                } else if (e.status === 400) {
                    this.abrirModalUbicacion('Hace falta tu ubicacion para pedir musica en este bar.');
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
    // Carga la cola desde el backend para simular reproduccion sin Premium.
    const email = sessionStorage.getItem('email');
    if (!email) { return; }
    this.musicService.getQueue(email).subscribe({
      next: (songs: any[]) => {
        // Normalizamos al formato usado en la vista (name + artists[0].name).
        const mapped = songs.map((s: any) => ({
          name: s.title,
          artists: [{ name: s.artist }],
          uri: s.uri
        }));
        this.queue = mapped;
        this.nowPlaying = mapped[0] || null;
        this.successMsg = this.successMsg || 'Reproduccion simulada en cola local.';
      },
      error: (err: any) => {
        console.error('No se pudo cargar la cola local', err);
      }
    });
  }

  private simularColaLocal(track: any) {
    // Cuando falta Premium, anade la cancion a la cola local de manera simulada.
    this.cargarColaLocal();
    this.successMsg = `"${track.name}" añadida en modo simulado (sin Premium).`;
    setTimeout(() => this.successMsg = '', 4000);
  }
}


















