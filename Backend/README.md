# Backend (Spring Boot)

API REST para la gramola: usuarios, pagos, musica y correo.

## Requisitos
- Java 17+
- Maven 3.x
- MySQL 8
- SMTP (Mailtrap)
- Stripe

## Configuracion
Archivo principal: `Backend/src/main/resources/application.properties`

## Variables de entorno
- MAILTRAP_USER
- MAILTRAP_PASS
- STRIPE_SECRET_KEY

## Ejecutar

```powershell
cd Backend
$env:MAILTRAP_USER="TU_USER"
$env:MAILTRAP_PASS="TU_PASS"
$env:STRIPE_SECRET_KEY="sk_test_..."
mvn spring-boot:run
```

## Tests
- Unit/integracion: `mvn test`
- Selenium E2E (requiere front): `mvn test -DrunSeleniumTests=true`

## Notas
- No subir claves al repositorio.
- Puerto por defecto: 8080.