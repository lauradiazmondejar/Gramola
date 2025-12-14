import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { UserService } from '../user';
import { Router } from '@angular/router';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './login.html',
  styleUrl: './login.css'
})
export class Login {
  email?: string;
  pwd?: string;
  errorMsg?: string;

  // Datos para la conexion con Spotify (Paso 1 del OAuth). Ajusta redirectUrl igual que en el Dashboard de Spotify.
  spoti = {
    authorizeUrl: 'https://accounts.spotify.com/authorize',
    redirectUrl: 'http://127.0.0.1:4200/callback' 
  };
  
  // Permisos solicitados a Spotify
  scopes = [
    "user-read-private", "user-read-email", "playlist-read-private", 
    "playlist-read-collaborative", "user-read-playback-state", 
    "user-modify-playback-state", "user-read-currently-playing", 
    "user-library-read", "user-library-modify", "user-read-recently-played", 
    "user-top-read", "app-remote-control", "streaming"
  ];

  constructor(private service: UserService, private router: Router) {}

  loguear() {
    if (!this.email || !this.pwd) {
      this.errorMsg = "Rellena todos los campos";
      return;
    }

    this.service.login(this.email, this.pwd).subscribe({
      next: (resp) => {
        const clientId = resp?.clientId;
        const signature = resp?.signature;
        console.log('Login correcto. Client ID recibido:', clientId);
        
        // Guardamos el clientId en sesion para usarlo luego
        sessionStorage.setItem("clientId", clientId);
        // Guardamos el email del bar logueado para validarlo en /music/add
        sessionStorage.setItem("email", this.email!);
        if (signature) {
          sessionStorage.setItem('signature', signature);
        }

        // Iniciamos el flujo de Spotify
        this.iniciarSpotifyOAuth(clientId);
      },
      error: (err) => {
        console.error(err);
        this.errorMsg = "Credenciales incorrectas o error en servidor";
      }
    });
  }

  iniciarSpotifyOAuth(clientId: string) {
    const state = this.generateRandomString(16);
    sessionStorage.setItem("oauth_state", state);

    const params = new URLSearchParams({
      response_type: 'code',
      client_id: clientId,
      scope: this.scopes.join(' '),
      redirect_uri: this.spoti.redirectUrl,
      state: state
    });

    // Redirigimos al usuario a Spotify
    window.location.href = `${this.spoti.authorizeUrl}?${params.toString()}`;
  }

  generateRandomString(length: number) {
    let text = '';
    const possible = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
    for (let i = 0; i < length; i++) {
      text += possible.charAt(Math.floor(Math.random() * possible.length));
    }
    return text;
  }
}
