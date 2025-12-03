import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PaymentService } from '../payment'; // Importamos tu servicio 'payment.ts'
import { Router, ActivatedRoute } from '@angular/router';

// Declaramos la variable global de Stripe
declare let Stripe: any;

@Component({
  selector: 'app-payment',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './payment.html',     // Tu archivo HTML corto
  styleUrl: './payment.css'          // Tu archivo CSS corto (si lo usas)
})
export class Payment implements OnInit {
  // Pon aquí tu Clave Pública (pk_test_...)
  stripe = Stripe('pk_test_51SIV2CRfAGkgoJHtjzPD344TigvazTauIQXxhm98Tk78mAuc7H79dD9XWvSO8cIfKNG8DS5MvEw5ldw6LhfUuEsg00QDV18Afz'); 
  
  elements: any;
  card: any;
  clientSecret: string = '';
  internalTransactionId: string = ''; // ID de la transacción en tu BD
  token: string = '';
  paymentStatus: string = '';

  constructor(
    private paymentService: PaymentService, 
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    // Recogemos el token de la URL (http://localhost:4200/payment?token=XYZ)
    this.token = this.route.snapshot.queryParams['token'];
  }

  iniciarPago() {
    this.paymentStatus = 'Conectando con el servidor...';
    
    // 1. Pedimos preparar el pago de 10€ (1000 céntimos)
    this.paymentService.prepay(1000).subscribe({
      next: (response: any) => {
        // El backend nos devuelve un JSON (como texto) con los datos de Stripe
        const data = JSON.parse(response);
        
        this.clientSecret = data.client_secret;
        this.internalTransactionId = data.id; // Guardamos el ID para confirmar luego
        
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

    this.card = this.elements.create('card', {style: style});
    this.card.mount('#card-element');
    
    // Hacemos visible el formulario
    document.getElementById('payment-form')!.style.display = 'block';
  }

  pagar() {
    this.paymentStatus = 'Procesando pago...';

    // 2. Confirmamos el pago directamente con Stripe
    this.stripe.confirmCardPayment(this.clientSecret, {
      payment_method: { card: this.card }
    }).then((result: any) => {
      if (result.error) {
        this.paymentStatus = result.error.message;
      } else {
        if (result.paymentIntent.status === 'succeeded') {
          // 3. Si Stripe dice OK, avisamos a nuestro Backend para activar la cuenta
          this.confirmarEnBackend(result);
        }
      }
    });
  }

  confirmarEnBackend(stripeResult: any) {
    this.paymentService.confirm(stripeResult, this.internalTransactionId, this.token)
      .subscribe({
        next: (res) => {
          this.paymentStatus = '¡Pago realizado! Cuenta activada.';
          // Redirigir al login después de 3 segundos
          setTimeout(() => this.router.navigate(['/login']), 3000);
        },
        error: (err) => {
          this.paymentStatus = 'Pago en Stripe OK, pero error al activar cuenta en servidor.';
          console.error(err);
        }
      });
  }
}