# Amancay

Backend de Amancay construido con Java, Spring Boot y Maven.

## Stack

- Java 21
- Spring Boot 4.1.1
- Maven
- Spring Web, Spring Data JPA, Validation, Lombok
- PostgreSQL

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

## Cómo correr el proyecto

```bash
./mvnw spring-boot:run
```

## Cómo correr los tests

```bash
./mvnw test
```
