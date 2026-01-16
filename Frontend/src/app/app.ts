import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.html',
  // Estilos globales en src/styles.css.
})
export class App {
  // Signal usado para exponer el titulo de la aplicacion.
  protected readonly title = signal('frontend');
}
