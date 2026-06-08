import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class MusicService {
  private base = environment.backendUrl;

  constructor(private http: HttpClient) {}

  getQueue(email: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.base}/music/queue?email=${encodeURIComponent(email)}`);
  }

  addSong(body: {
    title: string; artist: string; uri: string;
    clientId: string; email: string; bar: string | null;
    lat?: number; lon?: number;
  }): Observable<any> {
    return this.http.post(`${this.base}/music/add`, body);
  }

  addSongFree(body: {
    title: string; artist: string; uri: string;
    email: string; clientId: string; password: string;
  }): Observable<any> {
    return this.http.post(`${this.base}/music/add-free`, body);
  }
}
