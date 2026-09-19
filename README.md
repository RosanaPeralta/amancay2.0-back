# Amancay

Backend de Amancay construido con Java, Spring Boot y Maven.

## Stack

- Java 21
- Spring Boot 4.1.1
- Maven
- Spring Web, Spring Data JPA, Validation, Lombok
- PostgreSQL (Supabase)
- OpenAPI / Swagger UI

## Requisitos

- JDK 21+
- PostgreSQL corriendo localmente (o accesible por red)

## Configuración

La conexión a la base de datos se configura por variables de entorno (con valores por defecto para desarrollo local):

| Variable      | Default     |
|---------------|-------------|
| `DB_HOST`     | `localhost` |
| `DB_PORT`     | `5432`      |
| `DB_NAME`     | `amancay`   |
| `DB_USER`     | `postgres`  |
| `DB_PASSWORD` | `postgres`  |

Además, la autenticación y el front requieren:

| Variable                             | Default                 | Descripción                                                    |
|--------------------------------------|-------------------------|----------------------------------------------------------------|
| `SUPABASE_URL`                       | *(obligatoria)*         | Project URL de Supabase. Sin ella la aplicación no arranca.    |
| `CORS_ALLOWED_ORIGINS`               | `http://localhost:5173` | Orígenes permitidos para el front, separados por coma.         |
| `AMANCAY_SECURITY_DEV_USER_ENABLED`  | `false`                 | Bypass de autenticación para desarrollo (ver Autenticación).   |

Todas pueden definirse en un archivo `.env` en la raíz del proyecto (ver `.env.example`; el archivo está ignorado por git).

### Supabase - Primeros pasos para levantar el proyecto:

Para levantar el proyecto usando una base compartida:

1. Crear un proyecto en [Supabase](https://supabase.com/).
2. Entrar en **Connect**.
3. En **Connection Method**, seleccionar **Session Pooler**.
4. Revisar los datos de conexión desplazándose hacia abajo en la página y copiar el host, puerto, base de datos y usuario que muestra Supabase. (Al ejecutar el powershell reemplazar los datos por los propios siguiendo estos pasos)

No es necesario modificar `application.properties`. Cada integrante debe configurar sus propias variables en el terminal de PowerShell de Visual Studio Code. Reemplazar los valores de ejemplo por los datos reales de Supabase:

```powershell
$env:SPRING_DATASOURCE_URL="jdbc:postgresql://HOST_SUPABASE:PUERTO_SUPABASE/postgres?sslmode=require"
$env:DB_USER="USUARIO_SUPABASE"
$env:DB_PASSWORD="CONTRASEÑA_SUPABASE"

./mvnw.cmd spring-boot:run
```

El schema **vive en Supabase** y la aplicación solo lo valida al arrancar (`spring.jpa.hibernate.ddl-auto=validate`). No hay migraciones automáticas: los cambios de schema se aplican en el SQL Editor de Supabase.

Con la aplicación funcionando, Swagger se puede abrir en:

```text
http://localhost:8080/swagger-ui/index.html
```

## Cómo correr el proyecto

```bash
./mvnw spring-boot:run
```

## Productos

La API CRUD está disponible en `/api/products`. Las operaciones de creación y actualización reciben el producto junto con sus variantes; una actualización elimina las variantes existentes que no se envían.

- `GET /api/products?page=0&size=20&q=cafe&is_active=true`
- `GET /api/products/{id}`
- `POST /api/products`
- `PUT /api/products/{id}`
- `DELETE /api/products/{id}`

Swagger UI está disponible en `/swagger-ui/index.html` y el contrato adicional en `src/main/resources/openapi/products.yaml`.

## Usuarios y Reseñas

Perfil (`/api/me`), direcciones (`/api/me/addresses`), favoritos (`/api/me/favorites`), reseñas (`/api/products/{id}/reviews`, `/api/reviews/{id}`, `/api/me/reviews`) y administración (`/api/admin/**`, solo ADMIN). Todos los endpoints están en Swagger UI.

## Autenticación

El login y el registro ocurren en **Supabase Auth** (desde el front). El backend no tiene endpoints de login: actúa como *resource server* y valida en cada request el JWT que emite Supabase, enviado como `Authorization: Bearer <access_token>`.

- La firma se verifica contra el JWKS del proyecto (`SUPABASE_URL/auth/v1/.well-known/jwks.json`); también se validan el issuer y el audience `authenticated`.
- El `sub` del token es el `id` de la tabla `users`. En la primera request autenticada, si el usuario no existe, se crea automáticamente con rol `BUYER` (nombre tomado de `user_metadata.name`, si viene en el token).
- Requieren usuario autenticado: `/api/me/**`, `/api/admin/**` (además exige rol `ADMIN`) y la creación/edición/borrado de reseñas. El resto es público.
- Errores de autenticación responden `401 {"error": "Authentication required"}` y de autorización `403 {"error": "Access denied"}`.

### Probar endpoints autenticados en Swagger

1. Obtener un `access_token` iniciando sesión desde el front (en la consola del navegador: `JSON.parse(localStorage.getItem(Object.keys(localStorage).find(k => k.startsWith('sb-')))).access_token`).
2. En Swagger UI, botón **Authorize** y pegar el token.

### Bypass de autenticación para desarrollo

Con `AMANCAY_SECURITY_DEV_USER_ENABLED=true` toda request sin token corre como un usuario fijo (`00000000-0000-0000-0000-000000000001`, `dev@amancay.local`), sin necesidad de Supabase. Un token real sigue teniendo prioridad si se envía. **Nunca habilitarlo fuera de desarrollo local.**

## Tests

Pruebas unitarias e integración con Testcontainers (requiere Docker):

```bash
./mvnw test
```

Sin Docker, excluyendo la integración:

```powershell
.\mvnw.cmd test "-Dtest=!ProductIntegrationTest" -DfailIfNoSpecifiedTests=false
```
