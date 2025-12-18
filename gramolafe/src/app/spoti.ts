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
    const clientId = sessionStorage.getItem('clientId');
    return this.http.get(`${this.apiUrl}/getAuthorizationToken?code=${code}&clientId=${clientId}`);
  }

  getDevices(): Observable<any> {
    const headers = this.getHeaders();
    return this.http.get(`${this.spotifyApiUrl}/me/player/devices`, { headers });
  }

  getPlaylists(): Observable<any> {
    const headers = this.getHeaders();
    return this.http.get(`${this.spotifyApiUrl}/me/playlists`, { headers });
  }

  search(q: string): Observable<any> {
    const headers = this.getHeaders();
    return this.http.get(`${this.spotifyApiUrl}/search?q=${q}&type=track`, { headers });
  }

  addToQueue(uri: string, deviceId: string): Observable<any> {
    const headers = this.getHeaders();
    return this.http.post(
      `${this.spotifyApiUrl}/me/player/queue?uri=${uri}&device_id=${deviceId}`,
      {},
      { headers, responseType: 'text' as const }
    );
  }

  getQueue(): Observable<any> {
    const headers = this.getHeaders();
    return this.http.get(`${this.spotifyApiUrl}/me/player/queue`, { headers });
  }

  private getHeaders(): HttpHeaders {
    const token = sessionStorage.getItem('spoti_token');
    return new HttpHeaders({
      'Authorization': `Bearer ${token}`
    });
  }
}
