# Getting Started

## Isolated test setup with Docker (Postgres + Mock LLM)

This project includes a `docker-compose.yml` to run an isolated Postgres database for testing and a simple mock LLM service that always returns a dummy output.

### Start infrastructure

1. Ensure Docker is running.
2. From the project root, run:

```
docker compose up -d --build
```

Services:
- Postgres test database on host port `5433` (db `llm_test`, user `llm_user`, password `llm_pass`).
- Mock LLM service on `http://localhost:8081` with endpoints:
  - `POST /api/llm/process/email` → `{ "output": "llm output" }`
  - `POST /api/llm/process/pv` → `{ "output": "llm output" }`

### Run backend against test infra (default profile)

The default `application.properties` already points to Docker Postgres and the mock LLM.

Run with Maven wrapper:

```
./mvnw spring-boot:run
```

On Windows PowerShell:

```
./mvnw.cmd spring-boot:run
```

Tables are created automatically by JPA (`spring.jpa.hibernate.ddl-auto=update`). Note: because the entities use `@ElementCollection`, auxiliary tables are created for collections.

### Authentication for Postman

Security is enabled. Use Basic Auth:
- username: `user`
- password: `1234`

### Example Postman calls

- Create EmailRequest:
  - `POST http://localhost:8080/api/process/email`
  - Body (JSON):
```
{
  "templateType": "EMAIL",
  "subject": "Test subject",
  "bulletPoints": ["a", "b"],
  "recipientEmails": ["a@example.com"],
  "userId": 1
}
```

- Create PvRequest:
  - `POST http://localhost:8080/api/process/pv`
  - Body (JSON):
```
{
  "templateType": "PV",
  "date": "2025-10-05",
  "startTime": "10:00:00",
  "location": "Room 1",
  "participants": ["p1", "p2"],
  "bulletPoints": ["x", "y"],
  "userId": 1
}
```

- Get by id for user:
  - `GET http://localhost:8080/api/process/email/1/{requestId}`
  - `GET http://localhost:8080/api/process/pv/1/{requestId}`

### Tear down

```
docker compose down -v
```

### Reference Documentation

For further reference, please consider the following sections:

* [Official Apache Maven documentation](https://maven.apache.org/guides/index.html)
* [Spring Boot Maven Plugin Reference Guide](https://docs.spring.io/spring-boot/3.5.6/maven-plugin)
* [Create an OCI image](https://docs.spring.io/spring-boot/3.5.6/maven-plugin/build-image.html)
* [Spring Data JPA](https://docs.spring.io/spring-boot/3.5.6/reference/data/sql.html#data.sql.jpa-and-spring-data)
* [Spring Boot DevTools](https://docs.spring.io/spring-boot/3.5.6/reference/using/devtools.html)
* [Spring Security](https://docs.spring.io/spring-boot/3.5.6/reference/web/spring-security.html)
* [Spring Web](https://docs.spring.io/spring-boot/3.5.6/reference/web/servlet.html)

### Guides

The following guides illustrate how to use some features concretely:

* [Accessing Data with JPA](https://spring.io/guides/gs/accessing-data-jpa/)
* [Securing a Web Application](https://spring.io/guides/gs/securing-web/)
* [Spring Boot and OAuth2](https://spring.io/guides/tutorials/spring-boot-oauth2/)
* [Authenticating a User with LDAP](https://spring.io/guides/gs/authenticating-ldap/)
* [Building a RESTful Web Service](https://spring.io/guides/gs/rest-service/)
* [Serving Web Content with Spring MVC](https://spring.io/guides/gs/serving-web-content/)
* [Building REST services with Spring](https://spring.io/guides/tutorials/rest/)

### Maven Parent overrides

Due to Maven's design, elements are inherited from the parent POM to the project POM.
While most of the inheritance is fine, it also inherits unwanted elements like `<license>` and `<developers>` from the
parent.
To prevent this, the project POM contains empty overrides for these elements.
If you manually switch to a different parent and actually want the inheritance, you need to remove those overrides.

