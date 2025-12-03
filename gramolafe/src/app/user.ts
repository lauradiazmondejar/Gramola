// src/app/user.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class UserService {
  // 1. CORREGIDO: Quitado el espacio al principio de la URL
  private apiUrl = 'http://127.0.0.1:8080/users/register';

  constructor(private http: HttpClient) {}

  // 2. CORREGIDO: Añadidos los argumentos que faltaban (bar, clientId, clientSecret)
  register(bar: string, email: string, pwd1: string, pwd2: string, clientId: string, clientSecret: string) {
    let info = {
      bar: bar,
      email: email,
      pwd1: pwd1, 
      pwd2: pwd2,
      clientId: clientId,
      clientSecret: clientSecret
    }
    return this.http.post<any>(this.apiUrl, info);
  }

  login(email: string, pwd: string): Observable<string> {
    let info = {
      email: email,
      password: pwd
    };
    // Esperamos una respuesta de texto (el clientId)
    return this.http.post(this.apiUrl.replace('register', 'login'), info, { responseType: 'text' });
  }
}
