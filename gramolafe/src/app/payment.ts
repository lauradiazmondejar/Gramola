import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class PaymentService {
  private apiUrl = 'http://127.0.0.1:8080/payments';

  constructor(private http: HttpClient) {}

  // Paso 1: Pedir al backend que inicie el pago
  prepay(amount: number): Observable<any> {
    // 1000 = 10.00 EUR
    return this.http.post(`${this.apiUrl}/prepay?amount=${amount}`, {}, { responseType: 'text' });
  }

  // Paso 2: Confirmar al backend que el pago se ha realizado
  confirm(stripeResponse: any, internalId: string, token: string): Observable<any> {
    // Enviamos los datos necesarios para que el backend active la cuenta
    let info = {
      stripeId: stripeResponse.paymentIntent.id,
      internalId: internalId,
      token: token
    };
    return this.http.post(`${this.apiUrl}/confirm`, info, { responseType: 'text' });
  }
}
