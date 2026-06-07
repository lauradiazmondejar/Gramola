import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { UserService } from '../user';

@Component({
  selector: 'app-forgot',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './forgot.html'
})
// Solicitud de reset de password via email.
export class Forgot {
  email = '';
  success?: string;
  error?: string;
  submitted = false;

  constructor(private userService: UserService) {}

  solicitar() {
    this.submitted = true;
    // Pide al backend el envio del correo de reseteo.
    this.success = undefined;
    this.error = undefined;
    if (!this.email) {
      return;
    }
    this.userService.requestReset(this.email).subscribe({
      next: () => {
        this.success = 'Hemos enviado un email con el enlace para restablecer la contraseña.';
      },
      error: (err) => {
        console.error(err);
        this.error = err?.error?.message || 'No se pudo enviar el correo.';
      }
    });
  }
}

