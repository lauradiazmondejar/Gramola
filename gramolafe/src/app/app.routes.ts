import { Routes } from '@angular/router';
import { Register } from './register/register';
import { Login } from './login/login';
import { Payment } from './payment/payment';
import { Callback } from './callback/callback'; // Importar
import { Music } from './music/music'; // Importar

export const routes: Routes = [
    { path: 'register', component: Register },
    { path: 'login', component: Login },
    { path: 'payment', component: Payment },
    { path: 'callback', component: Callback }, // Esta es la pista de aterrizaje
    { path: 'music', component: Music },
    { path: '', redirectTo: '/register', pathMatch: 'full' }
];