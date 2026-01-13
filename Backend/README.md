# Backend (Spring Boot)

API REST para la gramola: usuarios, pagos, musica y correo.

## Requisitos
- Java 17+
- Maven 3.x (o mvnw)
- MySQL 8 (o H2 para tests)
- SMTP (Mailtrap u otro)
- Stripe secret key

## Configuracion
Archivo principal: `Backend/src/main/resources/application.properties`
- `spring.datasource.url`, `spring.datasource.username`, `spring.datasource.password`
- `app.frontend.host` (link de pago en emails)

## Variables de entorno
- MAILTRAP_USER
- MAILTRAP_PASS
- STRIPE_SECRET_KEY
- APP_CRYPTO_KEY (32 bytes, para cifrar secretos)
- Opcional: APP_PRICE_SUBSCRIPTION_MONTHLY, APP_PRICE_SUBSCRIPTION_ANNUAL, APP_PRICE_SONG

## Ejecutar

```powershell
cd Backend
$env:MAILTRAP_USER="TU_USER"
$env:MAILTRAP_PASS="TU_PASS"
$env:STRIPE_SECRET_KEY="sk_test_..."
$env:APP_CRYPTO_KEY="change-me-please-change-me-32bytes"
mvn spring-boot:run
```

## Tests
- Unit/integracion: `mvn test`
- Selenium E2E (requiere front): `mvn test -DrunSeleniumTests=true`
- Stripe tests: `mvn test -DrunStripeTests=true -Dstripe.secret-key=sk_test_...`

## Notas
- No subir claves al repositorio.
- Puerto por defecto: 8080.