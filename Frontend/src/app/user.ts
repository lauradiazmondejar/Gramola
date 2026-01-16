// src/app/user.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class UserService {
  // Base de la API del backend para cuentas de bar.
  private apiUrl = 'http://127.0.0.1:8080/users/register';

  constructor(private http: HttpClient) {}

  // Registro completo del bar (incluye datos de Spotify y firma).
  register(bar: string, email: string, pwd1: string, pwd2: string, clientId: string, clientSecret: string, lat?: number, lon?: number, signature?: string) {
    // Construye el cuerpo de registro y lo envia al backend.
    let info = {
      bar: bar,
      email: email,
      pwd1: pwd1,
      pwd2: pwd2,
      clientId: clientId,
      clientSecret: clientSecret,
      lat: lat,
      lon: lon,
      signature: signature
      
    }
    return this.http.post<any>(this.apiUrl, info);
  }

  login(email: string, pwd: string): Observable<any> {
    // Solicita login y espera clientId y firma de respuesta.
    let info = {
      email: email,
      password: pwd
    };
    // Esperamos JSON con clientId y firma.
    return this.http.post(this.apiUrl.replace('register', 'login'), info);
  }

  verifyPassword(email: string, pwd: string): Observable<any> {
    // Verifica credenciales sin alterar el flujo del login.
    return this.login(email, pwd);
  }

  requestReset(email: string) {
    // Pide al backend que genere un token de reseteo.
    return this.http.post(this.apiUrl.replace('register', 'reset/request'), { email });
  }

  confirmReset(token: string, pwd1: string, pwd2: string) {
    // Finaliza el reseteo de contrasena con el token recibido.
    return this.http.post(this.apiUrl.replace('register', 'reset/confirm'), { token, pwd1, pwd2 });
  }
}




