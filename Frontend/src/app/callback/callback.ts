import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { SpotiService } from '../spoti';

@Component({
  selector: 'app-callback',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './callback.html',
  styleUrl: './callback.css'
})
export class Callback implements OnInit {

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private spotiService: SpotiService
  ) {}

  ngOnInit(): void {
    // Valida parametros devueltos por Spotify antes de pedir el token
    const code = this.route.snapshot.queryParams['code'];
    const state = this.route.snapshot.queryParams['state'];
    const savedState = sessionStorage.getItem('oauth_state');
    const error = this.route.snapshot.queryParams['error'];
    const clientId = sessionStorage.getItem('clientId') || '';

    if (error) {
      console.error('Error en auth de Spotify', error);
      this.router.navigate(['/login']);
      return;
    }

    if (!code || !state) {
      console.error('No se recibio el parametro code/state en el callback');
      this.router.navigate(['/login']);
      return;
    }

    if (!savedState || savedState !== state) {
      console.error('State devuelto por Spotify no coincide');
      this.router.navigate(['/login']);
      return;
    }

    if (!clientId) {
      console.error('Falta clientId en sesion; vuelve a iniciar sesion');
      this.router.navigate(['/login']);
      return;
    }

    // Intercambiamos el code por el token de acceso y guardamos en sesion
    this.spotiService.getAuthorizationToken(code).subscribe({
      next: (data: any) => {
        sessionStorage.setItem('spoti_token', data.access_token);
        sessionStorage.removeItem('oauth_state');
        this.router.navigate(['/music']);
      },
      error: (err) => {
        console.error('Error intercambiando token', err);
        this.router.navigate(['/login']);
      }
    });
  }
}

