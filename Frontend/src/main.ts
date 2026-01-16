import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { App } from './app/app';

// Punto de entrada de la SPA: arranca Angular con rutas y providers.
bootstrapApplication(App, appConfig)
  .catch((err) => console.error(err));
