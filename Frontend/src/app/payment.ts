import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../environments/environment';
import { Price, StripeConfirmRequest } from './models/payment.model';

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
  confirm(stripeResponse: any, internalId: string, token: string | null): Observable<string> {
    const info: StripeConfirmRequest = {
      stripeId: stripeResponse.paymentIntent.id,
      internalId,
      token
    };
    return this.http.post(`${this.apiUrl}/confirm`, info, { responseType: 'text' });
  }

  getPrice(code: string, email?: string): Observable<Price> {
    let url = `${this.apiUrl}/prices/${code}`;
    if (email) url += `?email=${encodeURIComponent(email)}`;
    return this.http.get<Price>(url);
  }

  listPrices(): Observable<Price[]> {
    return this.http.get<Price[]>(`${this.apiUrl}/prices`);
  }
}
