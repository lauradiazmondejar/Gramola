import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { UserService } from '../user';

@Component({
  selector: 'app-reset',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './reset.html'
})
export class Reset implements OnInit {
  token = '';
  pwd1 = '';
  pwd2 = '';
  success?: string;
  error?: string;

  constructor(private route: ActivatedRoute, private router: Router, private userService: UserService) {}

  ngOnInit(): void {
    this.token = this.route.snapshot.queryParams['token'] || '';
    if (!this.token) {
      this.error = 'Falta el token en la URL';
    }
  }

  reset() {
    this.success = undefined;
    this.error = undefined;
    if (!this.token) {
      this.error = 'Falta token';
      return;
    }
    if (!this.pwd1 || !this.pwd2) {
      this.error = 'Introduce la nueva contraseña en ambos campos';
      return;
    }
    this.userService.confirmReset(this.token, this.pwd1, this.pwd2).subscribe({
      next: () => {
        this.success = 'Contraseña cambiada. Redirigiendo a login...';
        setTimeout(() => this.router.navigate(['/login']), 2000);
      },
      error: (err) => {
        console.error(err);
        this.error = err?.error?.message || 'No se pudo cambiar la contraseña';
      }
    });
  }
}
