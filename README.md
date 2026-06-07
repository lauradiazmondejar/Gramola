# GRAMOLA - TECNOLOGÍAS Y SISTEMAS WEB

Repositorio de Laura Díaz Mondéjar para el laboratorio de Tecnologías y Sistemas Web 2025/2026.

## Ramas

| Rama | Descripción |
|------|-------------|
| `ordinaria` | Convocatoria ordinaria |
| `main` | Convocatoria extraordinaria (rama activa) |

## Estructura
- `Backend/` — Spring Boot 3 (API REST)
- `Frontend/` — Angular 20

## Novedades de la convocatoria extraordinaria

- **Cola de canciones con prioridad** — canciones de pago se cuelan delante de las del catálogo (`priority` en entidad `Song`)
- **Búsqueda de canciones proxiada** — el frontend pide la búsqueda al backend, que la reenvía a Spotify con el token del bar
- **Precios en base de datos** — importes de suscripción y canción en tabla `price`, leídos en tiempo de ejecución
- **URLs y configuración en base de datos** — credenciales Spotify, URLs de callback, etc. en tabla `config`
- **Cifrado AES-256/GCM del `clientSecret` de Spotify** — se guarda cifrado en `config`, se descifra en memoria
- **Geolocalización** — el cliente envía coordenadas al reproducir; el backend rechaza si está demasiado lejos del bar
- **Correo local con Mailpit** — sustituye a Mailtrap; sin necesidad de credenciales externas
- **Test de carga JMeter** — plan en `Backend/src/test/jmeter/`, seeder de 1000 usuarios incluido
- **Test Selenium `ClientSongFlowSeleniumTest`** — flujo completo cliente: buscar, pagar canción y reproducir

## Requisitos

- Java 17+
- Maven 3.x
- Node.js 18+ y npm
- MySQL 8
- [Mailpit](https://mailpit.axllent.org/) (SMTP local, sin auth)
- Cuenta Stripe (clave de test)

## Variables de entorno

Solo se necesita una variable de entorno:

| Variable | Descripción |
|----------|-------------|
| `STRIPE_SECRET_KEY` | Clave secreta de Stripe (`sk_test_...`) |

El archivo `.env` en `Backend/` (no versionado) la exporta:
```bash
source Backend/.env
```

## Ejecutar Mailpit (servidor de correo local)

```bash
brew install mailpit   # solo la primera vez
mailpit                # arranca en localhost:1025 (SMTP) y localhost:8025 (web)
```

## Ejecutar backend

```bash
cd Backend
source .env
mvn spring-boot:run
```

## Ejecutar frontend

```bash
cd Frontend
npm install
npm start              # equivalente a ng serve --host 127.0.0.1 --port 4200
```

El host `127.0.0.1` está fijado en `angular.json`; no hace falta pasarlo a mano.

## Ejecutar tests Selenium

Requisitos: frontend activo en `http://127.0.0.1:4200` y Chrome disponible.  
El backend **no** debe estar levantado; los tests lo arrancan internamente.

```bash
cd Backend
# Tests de suscripción (Selenium puro, sin Stripe)
mvn test -DrunSeleniumTests=true

# Tests de suscripción con Stripe (requiere STRIPE_SECRET_KEY y backend en 8080)
mvn test -DrunUiStripeTests=true -Dserver.port=8080

# Test flujo cliente: buscar canción, pagar y reproducir
mvn test -DrunClientSongTests=true
```

## Test de carga JMeter

### 1. Sembrar 1000 usuarios en MySQL

Necesario solo la primera vez (o tras limpiarlos):

```bash
cd Backend
# Renombra temporalmente application.properties para que el seeder use MySQL
mv src/main/resources/application.properties src/main/resources/application.properties.old
mvn test -DrunJMeterSeeder=true -Dtest=JMeterUserSeeder
mv src/main/resources/application.properties.old src/main/resources/application.properties
```

### 2. Limpiar usuarios de carga tras el test

```bash
mv src/main/resources/application.properties src/main/resources/application.properties.old
mvn test -DrunJMeterSeeder=true -DjmeterClean=true -Dtest=JMeterUserSeeder
mv src/main/resources/application.properties.old src/main/resources/application.properties
```

### 3. Ejecutar plan JMeter (con backend arrancado)

```bash
rm -f jmeter-results.jtl && rm -rf jmeter-report/
jmeter -n \
  -t src/test/jmeter/gramola-login-load-test.jmx \
  -l jmeter-results.jtl \
  -e -o jmeter-report/
open jmeter-report/index.html
```

## Precios en base de datos (obligatorio)

Los importes **no se hardcodean**; se insertan en la tabla `price`:

```sql
INSERT INTO price (code, description, amount) VALUES
  ('subscription_monthly', 'Suscripcion mensual', 1000),
  ('subscription_annual',  'Suscripcion anual',   10000),
  ('song',                 'Cancion individual',   50);
```

## Notas

- Backend escucha en `http://127.0.0.1:8080`.
- Frontend se sirve en `http://127.0.0.1:4200`.
- Si usas Run Java en VS Code, abre VS Code desde una terminal que ya tenga las variables (`source .env`).
- No subir `.env`, `data.sql`, `jmeter-report/`, ni `jmeter-results.jtl` al repositorio.
