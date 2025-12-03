import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class SpotiService {
  private apiUrl = 'http://127.0.0.1:8080/spoti';
  private spotifyApiUrl = 'https://api.spotify.com/v1';

  constructor(private http: HttpClient) {}

  getAuthorizationToken(code: string): Observable<any> {
    // Recuperamos el clientId que guardamos en el login
    const clientId = sessionStorage.getItem('clientId');
    return this.http.get(`${this.apiUrl}/getAuthorizationToken?code=${code}&clientId=${clientId}`);
  }

  // 1. Obtener los dispositivos disponibles (altavoces, ordenador, móvil...)
  getDevices(): Observable<any> {
    const headers = this.getHeaders();
    return this.http.get(`${this.spotifyApiUrl}/me/player/devices`, { headers });
  }

  // 2. Obtener las playlists del usuario
  getPlaylists(): Observable<any> {
    const headers = this.getHeaders();
    return this.http.get(`${this.spotifyApiUrl}/me/playlists`, { headers });
  }

  // 3. Buscar canciones
  search(q: string): Observable<any> {
    const headers = this.getHeaders();
    // type=track indica que solo buscamos canciones (no álbumes ni artistas)
    return this.http.get(`${this.spotifyApiUrl}/search?q=${q}&type=track`, { headers });
  }

  // 4. Añadir a la cola de reproducción
  addToQueue(uri: string, deviceId: string): Observable<any> {
    const headers = this.getHeaders();
    // Esta petición es un POST y requiere el ID del dispositivo donde va a sonar
    // Spotify devuelve 204/200 sin JSON; pedimos texto para evitar error de parseo
    return this.http.post(
      `${this.spotifyApiUrl}/me/player/queue?uri=${uri}&device_id=${deviceId}`,
      {},
      { headers, responseType: 'text' as const }
    );
  }

  // Helper para crear las cabeceras con el token
  private getHeaders(): HttpHeaders {
    const token = sessionStorage.getItem('spoti_token'); // Recuperamos el token que guardaste en el Callback
    return new HttpHeaders({
      'Authorization': `Bearer ${token}`
    });
  }
}
