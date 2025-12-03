import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { UserService } from '../user'; // Asegúrate de que la ruta sea correcta

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './register.html',
  styleUrl: './register.css'
})
export class Register {

  // Nuevos campos
  bar?: string;
  email?: string;
  pwd1?: string;
  pwd2?: string;
  clientId?: string;
  clientSecret?: string;

  registroOK: boolean = false;
  registroKO: boolean = false;

  constructor(private service: UserService) { }

  registrar() {
    this.registroOK = false;
    this.registroKO = false;

    // Validación básica
    if (!this.bar || !this.email || !this.pwd1 || !this.clientId || !this.clientSecret) {
      alert('Por favor, rellena todos los campos.');
      return;
    }

    if (this.pwd1 != this.pwd2) {
      alert('Las contraseñas no coinciden');
      return;
    }

    // Llamada al servicio con TODOS los datos
    this.service.register(this.bar, this.email, this.pwd1, this.pwd2, this.clientId, this.clientSecret)
      .subscribe({
        next: (response) => {
          console.log('Registro exitoso', response);
          this.registroOK = true;
          this.registroKO = false;
        },
        error: (error) => {
          console.error('Error en el registro', error);
          this.registroKO = true;
          this.registroOK = false;
        }
      });
  }
}