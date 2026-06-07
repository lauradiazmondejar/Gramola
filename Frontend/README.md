# Frontend (Angular 20)

UI de la gramola.

## Requisitos

- Node.js 18+ y npm
- Angular CLI

## Ejecutar

```bash
cd Frontend
npm install
npm start
```

El host `127.0.0.1` está fijado en `angular.json` (necesario para el OAuth de Spotify y para evitar errores CORS).  
Equivale a `ng serve --host 127.0.0.1 --port 4200`.

## Configuración

- Backend base URL: `http://127.0.0.1:8080` (ver `src/environments/environment.ts`)
- OAuth Spotify redirect: `http://127.0.0.1:4200/callback`

## Novedades convocatoria extraordinaria

- **Búsqueda de canciones**: el componente `music` llama al backend (`/music/search`), que a su vez consulta Spotify usando el token del bar
- **Cola con prioridad**: las canciones de pago aparecen antes en la cola
- **Geolocalización**: al reproducir una canción, se envían las coordenadas del cliente al backend
- **`environment.ts`**: URL del backend centralizada; no hay URLs hardcodeadas en los componentes

## Notas

- Accede siempre desde `http://127.0.0.1:4200`, nunca desde `http://localhost:4200` (CORS).
