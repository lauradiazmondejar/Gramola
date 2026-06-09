# Backend (Spring Boot 3)

API REST de la gramola: usuarios, pagos, música, correo y cola de canciones.

## Requisitos

- Java 17+
- Maven 3.x
- MySQL 8
- Mailpit
- Cuenta Stripe (clave de test)

## Configuración

Archivo principal: `src/main/resources/application.properties`

### Variables de entorno

| Variable | Descripción |
|----------|-------------|
| `STRIPE_SECRET_KEY` | Clave secreta de Stripe (`sk_test_...`) |

El archivo `.env` (no versionado) la exporta:
```bash
source .env
```

### Datos iniciales en base de datos

El archivo `src/main/resources/data.sql` se ejecuta automáticamente al arrancar y puebla:

- Tabla `price`: importes de suscripción y canción
- Tabla `config`: URLs de Spotify, frontend, backend y callback

## Ejecutar

```bash
# 1. Arrancar Mailpit (solo la primera vez por sesión)
mailpit

# 2. Arrancar el backend
cd Backend
source .env
mvn spring-boot:run
```

## Tests

### Tests unitarios e integración (H2 en memoria)

```bash
mvn test
```

### Tests Selenium E2E (requieren frontend activo en http://127.0.0.1:4200)

```bash
# Flujo de suscripción (sin Stripe)
mvn test -DrunSeleniumTests=true

# Flujo de suscripción con Stripe (requiere STRIPE_SECRET_KEY y backend en 8080)
mvn test -DrunUiStripeTests=true -Dserver.port=8080

# Flujo completo cliente: buscar canción, pagar y reproducir
mvn test -DrunClientSongTests=true
```

### Test de carga JMeter

Sembrar 1000 usuarios (hay que quitar el application.properties de test para que Spring use el de main, que apunta a MySQL):

```bash
mv src/test/resources/application.properties src/test/resources/application.properties.old
mvn test -DrunJMeterSeeder=true -Dtest=JMeterUserSeeder
mv src/test/resources/application.properties.old src/test/resources/application.properties
```

Limpiar usuarios de carga:

```bash
mv src/test/resources/application.properties src/test/resources/application.properties.old
mvn test -DrunJMeterSeeder=true -DjmeterClean=true -Dtest=JMeterUserSeeder
mv src/test/resources/application.properties.old src/test/resources/application.properties
```

## Novedades convocatoria extraordinaria

- `Config`: entidad/DAO/servicio para guardar URLs y credenciales en BD
- `Song.priority`: booleano para gestionar la cola (canciones de pago se cuelan)
- `MusicService`: geolocalización del cliente antes de reproducir; pago se consume solo tras validar
- `SpotiService`: búsqueda proxiada de canciones (frontend -> backend -> Spotify)
- `SecretEncryptionService`: cifrado AES-256/GCM del `clientSecret` de Spotify
- Correo: Mailtrap reemplazado por Mailpit

## Notas

- Puerto por defecto: 8080.
- No subir `.env`, `data.sql`, `jmeter-report/`, ni `jmeter-results.jtl`.
