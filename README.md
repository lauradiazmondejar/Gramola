# GRAMOLA - TECNOLOGÍAS Y SISTEMAS WEB

Repositorio de Laura Díaz Mondéjar para el laboratorio de Tenologías y Sistemas Web 2025/2026.

## Estructura
- Backend/ (Spring Boot)
- Frontend/ (Angular)

## Requisitos
- Java 17+
- Maven 3.x (o mvnw)
- Node.js 18+ y npm
- MySQL 8 (o ajustar a H2 si no hay MySQL)
- Credenciales SMTP (Mailtrap u otro)
- Stripe (clave secreta de test)

## Variables de entorno
- MAILTRAP_USER
- MAILTRAP_PASS
- STRIPE_SECRET_KEY

## Ejecutar backend
Desde otra terminal (se usan variables de entorno):

```powershell
cd Backend
$env:MAILTRAP_USER="TU_USER"
$env:MAILTRAP_PASS="TU_PASS"
$env:STRIPE_SECRET_KEY="sk_test_..."
mvn spring-boot:run
```

## Ejecutar frontend

```powershell
cd Frontend
npm install
ng serve --host 127.0.0.1 --port 4200
```

## Notas
- Backend escucha en http://127.0.0.1:8080.
- Frontend se sirve en http://127.0.0.1:4200.
- Si usas Run Java en VS Code, abre VS Code desde una terminal que ya tenga las variables.

## Precios en base de datos (obligatorio)
Los importes de suscripcion y de cancion **no se hardcodean** ni se guardan en archivos de recursos.
Deben estar en la tabla `price` de la base de datos y el backend los lee en tiempo de ejecucion.

Ejemplo de insercion manual (MySQL):
```sql
INSERT INTO price (code, description, amount) VALUES
('subscription_monthly', 'Suscripcion mensual', 1000),
('subscription_annual', 'Suscripcion anual', 10000),
('song', 'Cancion individual', 50);
```
