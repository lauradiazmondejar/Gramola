import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class PaymentService {
  private apiUrl = `${environment.backendUrl}/payments`;

  constructor(private http: HttpClient) {}

  // Prepara un pago tomando el precio desde BD usando un codigo (song, subscription_monthly, etc.).
  prepay(code: string, email?: string, bar?: string, type: string = 'subscription'): Observable<any> {
    let url = `${this.apiUrl}/prepay?code=${code}&type=${type}`;
    if (email) url += `&email=${encodeURIComponent(email)}`;
    if (bar) url += `&bar=${encodeURIComponent(bar)}`;
    return this.http.post(url, {}, { responseType: 'text' });
  }

  // Paso 2: confirmar al backend que el pago se ha realizado.
  confirm(stripeResponse: any, internalId: string, token: string | null): Observable<any> {
    // Enviamos los datos necesarios para que el backend active la cuenta.
    let info = {
      stripeId: stripeResponse.paymentIntent.id,
      internalId: internalId,
      token: token
    };
    return this.http.post(`${this.apiUrl}/confirm`, info, { responseType: 'text' });
  }

  getPrice(code: string, email?: string): Observable<any> {
    // Recupera el precio: personalizado por bar si se pasa email, global en otro caso
    let url = `${this.apiUrl}/prices/${code}`;
    if (email) url += `?email=${encodeURIComponent(email)}`;
    return this.http.get(url);
  }

  listPrices(): Observable<any> {
    // Devuelve todos los precios disponibles para pintar la UI.
    return this.http.get(`${this.apiUrl}/prices`);
  }
}
