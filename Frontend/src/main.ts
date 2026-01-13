import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { App } from './app/app';

bootstrapApplication(App, appConfig)
  // Arranca la aplicacion Angular con la configuracion principal
  .catch((err) => console.error(err));
