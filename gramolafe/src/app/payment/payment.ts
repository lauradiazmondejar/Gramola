import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PaymentService } from '../payment';
import { Router, ActivatedRoute } from '@angular/router';

declare let Stripe: any;

@Component({
  selector: 'app-payment',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './payment.html',
  styleUrl: './payment.css'
})
export class Payment implements OnInit {
  stripe = Stripe('pk_test_51SIV2CRfAGkgoJHtjzPD344TigvazTauIQXxhm98Tk78mAuc7H79dD9XWvSO8cIfKNG8DS5MvEw5ldw6LhfUuEsg00QDV18Afz');

  elements: any;
  card: any;
  clientSecret: string = '';
  internalTransactionId: string = '';
  token: string = '';
  paymentStatus: string = '';
  planCode: string = 'subscription_monthly';
  prices: Record<string, number> = {};

  constructor(
    private paymentService: PaymentService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.token = this.route.snapshot.queryParams['token'];
    this.cargarPrecios();
    if (!this.token) {
      this.paymentStatus = 'Falta el token de activaci\u00f3n en la URL.';
    }
  }

  private cargarPrecios() {
    this.paymentService.listPrices().subscribe({
      next: (list: any[]) => {
        list.forEach(p => this.prices[p.code] = p.amount);
      },
      error: (err) => {
        console.error('No se pudieron cargar los precios', err);
      }
    });
  }

  getPrecioSeleccionado(): number | undefined {
    return this.prices[this.planCode];
  }

  iniciarPago() {
    this.paymentStatus = 'Conectando con el servidor...';

    this.paymentService.prepay(this.planCode).subscribe({
      next: (response: any) => {
        const data = JSON.parse(response);
        this.clientSecret = data.client_secret;
        this.internalTransactionId = data.id;
        this.paymentStatus = '';
        this.mostrarFormularioStripe();
      },
      error: (err) => {
        console.error(err);
        this.paymentStatus = 'Error al iniciar el pago.';
      }
    });
  }

  mostrarFormularioStripe() {
    this.elements = this.stripe.elements();

    const style = {
      base: {
        color: '#32325d',
        fontFamily: '"Helvetica Neue", Helvetica, sans-serif',
        fontSmoothing: 'antialiased',
        fontSize: '16px',
        '::placeholder': { color: '#aab7c4' }
      },
      invalid: { color: '#fa755a', iconColor: '#fa755a' }
    };

    this.card = this.elements.create('card', { style: style });
    this.card.mount('#card-element');
    const form = document.getElementById('payment-form');
    if (form) {
      form.style.display = 'block';
    }
  }

  pagar() {
    this.paymentStatus = 'Procesando pago...';

    this.stripe.confirmCardPayment(this.clientSecret, {
      payment_method: { card: this.card }
    }).then((result: any) => {
      if (result.error) {
        this.paymentStatus = result.error.message;
      } else {
        if (result.paymentIntent.status === 'succeeded') {
          this.confirmarEnBackend(result);
        }
      }
    });
  }

  confirmarEnBackend(stripeResult: any) {
    this.paymentService.confirm(stripeResult, this.internalTransactionId, this.token)
      .subscribe({
        next: () => {
          this.paymentStatus = 'Pago realizado. Cuenta activada.';
          setTimeout(() => this.router.navigate(['/login']), 3000);
        },
        error: (err) => {
          this.paymentStatus = 'Pago en Stripe OK, pero error al activar cuenta en servidor.';
          console.error(err);
        }
      });
  }
}
