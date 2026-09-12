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

## Tests

Pruebas unitarias e integración con Testcontainers (requiere Docker):

```bash
./mvnw test
```

Sin Docker, excluyendo la integración:

```powershell
.\mvnw.cmd test "-Dtest=!ProductIntegrationTest" -DfailIfNoSpecifiedTests=false
```
