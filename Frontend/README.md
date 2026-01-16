# Frontend (Angular)

UI de la gramola (Angular 20.x).

## Requisitos
- Node.js 18+ y npm
- Angular CLI

## Ejecutar

```powershell
cd Frontend
npm install
ng serve --host 127.0.0.1 --port 4200
```

## Configuracion
- Backend base: http://127.0.0.1:8080 (ver `src/app/music/music.ts`).
- OAuth Spotify redirect: http://127.0.0.1:4200/callback (ver `src/app/login/login.ts`).
