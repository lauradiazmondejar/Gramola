# Gramola – Backend (Spring Boot)

## Requisitos
- Java 17
- Maven 3.x
- MySQL (dev) o H2 para tests
- Node/Angular (front en `gramolafe`, sirve en 127.0.0.1:4200)
- Stripe (clave secreta y clave pública de test)
- Mailtrap u otro SMTP para envío de correos

## Configuración (variables recomendadas)
- `SPRING_DATASOURCE_URL` / `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD`
- `STRIPE_SECRET_KEY` (`stripe.secret-key`): tu `sk_test...` o `sk_live...`
- `MAILTRAP_USER` / `MAILTRAP_PASS` (SMTP)
- `APP_CRYPTO_KEY` (`app.crypto.key`): clave de 32 bytes para cifrar `clientSecret` de Spotify
- Opcional precios (céntimos): `APP_PRICE_SUBSCRIPTION_MONTHLY`, `APP_PRICE_SUBSCRIPTION_ANNUAL`, `APP_PRICE_SONG`

## Arranque
```
mvn spring-boot:run
```
Front (desde `gramolafe`): `ng serve` (127.0.0.1:4200).

## Tests
- Unit/integración: `mvn test`
  - Ejecuta `MusicServiceTests` y tests HTTP de `/music/add` con pagos simulados.
- E2E Selenium (requiere front en 127.0.0.1:4200):  
  `mvn test -DrunSeleniumTests=true`
- Flujos Stripe reales (requiere `STRIPE_SECRET_KEY` y cuenta de test):  
  `mvn test -DrunStripeTests=true -Dstripe.secret-key=sk_test_...`

## Notas de seguridad
- No hardcodees claves; usa variables/propiedades.
- `clientSecret` de Spotify se almacena cifrado (`app.crypto.key`).
- Ajusta `spring.jpa.hibernate.ddl-auto` fuera de dev (usa migraciones en producción).
