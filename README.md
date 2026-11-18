# api-core

[![CI](https://github.com/matheuslealpa/api-core/actions/workflows/ci.yml/badge.svg)](https://github.com/matheuslealpa/api-core/actions/workflows/ci.yml)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.matheuslealpa/api-core)](https://central.sonatype.com/artifact/io.github.matheuslealpa/api-core)
[![License](https://img.shields.io/github/license/matheuslealpa/api-core)](https://github.com/matheuslealpa/api-core/blob/master/LICENSE)
![Java](https://img.shields.io/badge/Java-25-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.x-6DB33F?logo=springboot)

## (Dev-Productivity)

A Spring Boot starter for REST APIs. Just add the dependency and you get, already configured:

| Feature | What you get out of the box |
|---|---|
| **Cache** | `@Cacheable` backed by Caffeine, per-cache specs and statistics exposed as metrics |
| **JPA** | `BaseEntity` (UUID id, `@Version`, auditing), `BaseRepository` (CRUD + `Specification` + `findByIdOrThrow`), `PageResponse` |
| **CRUD** | `CrudService` (transactional, with hooks for business rules) and `CrudController` (REST endpoints with pagination, RSQL filtering, validation, `ETag`/`If-Match` and optional `PATCH`) ready to extend |
| **Filters** | Every `Specification<T>` controller parameter is populated from `?filter=` (RSQL) automatically, with a whitelist that is safe by default (also applied to `?sort=`) and 400 errors as `problem+json` |
| **Auditing** | `createdBy`/`updatedBy`/`createdAt`/`updatedAt` filled in with the user from the JWT |
| **Security** | OAuth2 Resource Server (JWT), stateless, roles from a claim (Keycloak included), CORS, `@PreAuthorize`, 401/403 as `problem+json` |
| **Observability** | Actuator (health/probes, info, metrics, prometheus), Micrometer + OpenTelemetry tracing (OTLP), `traceId` in logs and in the `X-Trace-Id` header, `@Observed` |
| **Errors** | Global handler using `ProblemDetail` (validation, invalid sort, 404, 409 for concurrency or constraint violations, 412, 422, 401/403) including the `traceId` |
| **OpenAPI** | With springdoc on the classpath: `filter` documented with the allowed fields, `Pageable` as `page`/`size`/`sort`, and a JWT security scheme |
| **Testing** | `ApiCoreJwt.jwtWithRoles("ADMIN")` to authenticate in `MockMvc` with roles mapped exactly as at runtime |
| **Lombok** | Available in the project, with `@SuperBuilder` working on entities that extend `BaseEntity`/`AuditableEntity` |
| **Runtime** | Virtual threads enabled, `open-in-view` disabled |
| **Null safety** | API annotated with [JSpecify](https://jspecify.dev) (`@NullMarked`/`@Nullable`), following the same approach as Spring 7: Kotlin and tools such as NullAway see the nullability |

Requirements: **Java 25** and **Spring Boot 4.1**.

## Usage

Available on [Maven Central](https://central.sonatype.com/artifact/io.github.matheuslealpa/api-core):

```xml
<dependency>
    <groupId>io.github.matheuslealpa</groupId>
    <artifactId>api-core</artifactId>
    <version>1.0.0</version>
</dependency>
```

Minimal configuration for the consuming project (`application.yml`):

```yaml
spring:
  application:
    name: my-api
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://auth.example.com/realms/my-realm

management:
  opentelemetry:
    tracing:
      export:
        otlp:
          endpoint: http://otel-collector:4318/v1/traces
```

Without `issuer-uri` or `jwk-set-uri` the application won't start, since there is no way to validate the JWT.

### API without security

For internal APIs or ones that don't need authentication:

```yaml
api-core:
  security:
    enabled: false
```

All routes become open (including the Actuator) and a JWT is no longer required, so `issuer-uri` isn't needed. CORS still applies, auditing records the `system-auditor`, and methods annotated with `@PreAuthorize` respond with 403.

### Entities and repositories

```java
@Entity
public class Product extends BaseEntity {
    private String name;
}

public interface ProductRepository extends BaseRepository<Product, UUID> {
}

@GetMapping
PageResponse<ProductDto> list(Pageable pageable) {
    return PageResponse.of(repository.findAll(pageable), ProductDto::from);
}
```

For entities with their own id, extend `AuditableEntity`. Throw `ResourceNotFoundException` (404) or `BusinessException` (422) and the global handler builds the response.

### CRUD service

Extend `CrudService` and the service comes with the operations ready to use:

```java
@Service
public class ProductService extends CrudService<Product, UUID, ProductRepository> {

    public ProductService(ProductRepository repository) {
        super(repository);
    }

    @Override
    protected void beforeDelete(Product product) {
        if (product.isActive()) {
            throw new BusinessException("An active product cannot be deleted."); // 422
        }
    }
}
```

| Method | What it does |
|---|---|
| `findAll(pageable)` | Paginated list |
| `findAll(specification, pageable)` | Filtered list, e.g. with the `Specification` received in the controller |
| `findById(id)` | Finds it or throws 404 |
| `existsById(id)` | |
| `create(entity)` | Calls `beforeCreate` and saves |
| `update(id, changes)` | Loads it (404 if missing), applies the changes, calls `beforeUpdate` and saves |
| `update(id, expectedVersion, changes)` | Same, but first checks that the entity is still at the version the client read (412 if not) |
| `delete(id)` | Loads it (404 if missing), calls `beforeDelete` and deletes |

Reads run in a read-only transaction and writes in their own transaction. Methods from the specific repository (`findByEmail`…) are available in the subclass through the `repository` field. In the controller:

```java
@GetMapping
PageResponse<ProductDto> list(Specification<Product> filter, Pageable pageable) {
    return PageResponse.of(service.findAll(filter, pageable), ProductDto::from);
}

@PutMapping("/{id}")
ProductDto update(@PathVariable UUID id, @Valid @RequestBody ProductRequest request) {
    return ProductDto.from(service.update(id, product -> product.setName(request.name())));
}
```

### CRUD controller

Extend `CrudController` and the REST endpoints come ready, using the service above:

```java
@RestController
@RequestMapping("/products")
@RsqlFilter(allow = {"name", "price"})
public class ProductController extends CrudController<Product, UUID, ProductRequest, ProductResponse> {

    public ProductController(ProductService service) {
        super(service);
    }

    @Override
    protected ProductResponse toResponse(Product product) {
        return new ProductResponse(product.getId(), product.getName(), product.getPrice());
    }

    @Override
    protected Product toEntity(ProductRequest request) {
        return Product.builder().name(request.name()).price(request.price()).build();
    }

    @Override
    protected void applyChanges(Product product, ProductRequest request) {
        product.setName(request.name());
        product.setPrice(request.price());
    }
}
```

| Endpoint | Response |
|---|---|
| `GET /products?filter=price=gt=100&page=0&size=20&sort=name` | 200 with a `PageResponse`; a `sort` outside the whitelist gives 400 |
| `GET /products/{id}` | 200 with `ETag: "<version>"`, or 404 |
| `POST /products` | 201 with `ETag`; body validated with `@Valid` (400 with the list of `errors`) |
| `PUT /products/{id}` | 200 with the new `ETag`; 404; 412 if `If-Match` doesn't match the current version |
| `PATCH /products/{id}` | 405, unless the subclass implements `applyPatch` (accepts `If-Match`) |
| `DELETE /products/{id}` | 204, or 404 |

The class-level `@RsqlFilter` is optional and defines which fields of the listing can be filtered and sorted; without it, the entity's simple fields apply. For per-endpoint access rules, override the method and annotate it with `@PreAuthorize`, or use an `AuthorizeRequestsCustomizer`. Extra endpoints go in the subclass as usual.

**Concurrency.** To keep two clients from overwriting each other's edits, the client sends back on `PUT`/`PATCH` the `ETag` it received from `GET`, in the `If-Match` header. If the entity changed in the meantime, the response is 412 and nothing is saved. Without `If-Match` (or with `If-Match: *`), the update happens without checking the version. Only versioned entities (`BaseEntity`/`AuditableEntity`) have an `ETag`.

**PATCH.** Disabled (405) so as not to expose a generic merge without validation. To enable it, override `applyPatch`. A missing key means "keep" and a `null` value means "clear":

```java
@Override
protected void applyPatch(Product product, Map<String, Object> patch) {
    if (patch.containsKey("price")) {
        if (!(patch.get("price") instanceof Integer price) || price < 0) {
            throw new BusinessException("price must be a non-negative integer."); // 422
        }
        product.setPrice(price);
    }
}
```

### RSQL filters

Every `Specification<T>` parameter of a controller is populated from `?filter=`, no annotation needed:

```java
@GetMapping
PageResponse<ProductDto> list(Specification<Product> filter, Pageable pageable) {
    return PageResponse.of(repository.findAll(filter, pageable), ProductDto::from);
}
```

| Request | Meaning |
|---|---|
| `?filter=name==keyb*` | `name` starts with "keyb" |
| `?filter=price=gt=100;price=le=500` | `;` = AND |
| `?filter=name==mouse,name==keyboard` | `,` = OR |
| `?filter=name=in=(mouse,keyboard)` | list of values |
| `?filter=createdAt=bt=(2026-01-01T00:00:00Z,2026-12-31T23:59:59Z)` | range |

Other operators: `!=`, `=lt=`, `=ge=`, `=out=`, `=isnull=`, `=notnull=`, `=like=`, `=icase=`. Without the parameter, the query isn't filtered. Invalid syntax, a field outside `allow`, or a value of the wrong type result in 400.

By default, only the entity's **simple fields** can be filtered (including those inherited from `BaseEntity`, such as `id` and `createdAt`). Relationships, collections and embeddables are blocked, so as not to expose data from other tables or allow arbitrary joins. Use `@RsqlFilter` on the parameter or on the controller class to change this:

| Annotation | Filterable fields |
|---|---|
| (none) | The entity's simple fields |
| `@RsqlFilter(allow = {"name", "category"})` | Only those listed; a listed relationship opens up its fields (`category.name`) |
| `@RsqlFilter(allow = "*")` | All of them, including relationships |

`@RsqlFilter` also accepts `param` (the parameter name in the URL) and `distinct` (useful when filtering by collections).

The same whitelist applies to `?sort=` on these endpoints: sorting by a field outside it gives 400 with the field in `property`, so sorting can't become a way to probe fields or perform joins the filter blocks. On endpoints without a `Specification`, a `sort` by a nonexistent field also responds with 400 rather than 500.

### OpenAPI

springdoc isn't bundled. Just add it to your project and api-core adjusts the documentation:

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>3.2.0</version>
</dependency>
```

- The `Specification<T>` parameter is removed from the documentation and replaced with `filter` (string), listing the filterable and sortable fields of that endpoint.
- `Pageable` shows up as `page`, `size` and `sort`, without needing `@ParameterObject`.
- With security enabled, the `bearer-jwt` scheme is registered and applied to every operation.

springdoc's routes require a token like any other. To open them up, add them to `api-core.security.public-paths` (e.g. `/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html`). Turn it all off with `api-core.openapi.enabled=false`.

### Testing

The artifact with the `tests` classifier ships `ApiCoreJwt`, which authenticates in `MockMvc` with the roles in the same claim and with the same prefix used at runtime:

```xml
<dependency>
    <groupId>io.github.matheuslealpa</groupId>
    <artifactId>api-core</artifactId>
    <version>${api-core.version}</version>
    <classifier>tests</classifier>
    <scope>test</scope>
</dependency>
```

```java
mvc.perform(delete("/products/{id}", id).with(ApiCoreJwt.jwtWithRoles("ADMIN")))
        .andExpect(status().isNoContent());

// With api-core.security.roles-claim=realm_access.roles (Keycloak)
mvc.perform(get("/admin").with(ApiCoreJwt.jwtWithClaimRoles("realm_access.roles", "ADMIN")));
```

It uses `spring-security-test`, which must be among the project's test dependencies.

### Lombok

Lombok comes with api-core, but since Java 23 `javac` only runs annotation processors that are declared explicitly. Without that, the annotations are ignored and compilation fails with `cannot find symbol`. In the project's `pom.xml`:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <annotationProcessorPaths>
            <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
                <version>${lombok.version}</version> <!-- comes from spring-boot-starter-parent -->
            </path>
        </annotationProcessorPaths>
    </configuration>
</plugin>
```

Entities:

```java
@Entity
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends BaseEntity {
    private String name;
}

Product product = Product.builder().name("Keyboard").build();
```

The builder exposes only the entity's own fields: `id`, `version` and the auditing fields are left out, because they are managed by JPA. Don't use `@Data` or `@EqualsAndHashCode`: `BaseEntity`'s `equals`/`hashCode` are `final` (id-based equality, safe with proxies), and the compiler reports an error if Lombok tries to generate others.

### Extra authorization rules

```java
@Bean
AuthorizeRequestsCustomizer adminRules() {
    return auth -> auth.requestMatchers("/admin/**").hasRole("ADMIN");
}
```

## Properties

| Property | Default | Description |
|---|---|---|
| `api-core.banner.enabled` | `true` | api-core banner below the Spring Boot banner (follows `spring.main.banner-mode`) |
| `api-core.defaults.enabled` | `true` | Applies the opinionated defaults below |
| `api-core.cache.enabled` | `true` | Enables the `CaffeineCacheManager` |
| `api-core.cache.default-spec` | `maximumSize=1000,expireAfterWrite=10m` | Spec for caches without their own configuration |
| `api-core.cache.specs.<name>` | – | Per-cache spec, e.g. `maximumSize=500,expireAfterWrite=5m` |
| `api-core.audit.enabled` | `true` | Enables `@EnableJpaAuditing` |
| `api-core.audit.system-auditor` | `system` | Auditor when there is no authenticated user |
| `api-core.audit.username-claims` | `preferred_username` | Claims checked for the auditor (fallback: `sub`) |
| `api-core.security.enabled` | `true` | `false` opens all routes, no JWT |
| `api-core.security.public-paths` | `/actuator/health/**, /actuator/info, /error` | Routes without authentication |
| `api-core.security.roles-claim` | `roles` | Roles claim; Keycloak: `realm_access.roles` |
| `api-core.security.role-prefix` | `ROLE_` | Prefix applied to roles |
| `api-core.security.method-security` | `true` | Enables `@PreAuthorize` |
| `api-core.security.cors.allowed-origins` | – | Allowed origins (accepts patterns, e.g. `https://*.example.com`) |
| `api-core.security.cors.allowed-methods` | `GET, POST, PUT, PATCH, DELETE, OPTIONS` | |
| `api-core.security.cors.allowed-headers` | `*` | |
| `api-core.security.cors.exposed-headers` | – | Added to `ETag` and the trace header, which are always exposed |
| `api-core.security.cors.allow-credentials` | `false` | |
| `api-core.security.cors.max-age` | `1h` | |
| `api-core.web.enabled` | `true` | Enables the `GlobalExceptionHandler` |
| `api-core.rsql.enabled` | `true` | Enables RSQL filters on `Specification<T>` parameters |
| `api-core.web.trace-id-header` | `X-Trace-Id` | Response header carrying the trace id; empty disables it |
| `api-core.rsql.param-name` | `filter` | Default name of the RSQL expression parameter |
| `api-core.openapi.enabled` | `true` | Adjustments to the springdoc documentation (only with springdoc on the classpath) |

### Applied defaults

They have the lowest precedence, so any value in `application.yml` or in an environment variable wins:

```properties
management.endpoints.web.exposure.include=health,info,metrics,prometheus
management.endpoint.health.probes.enabled=true
management.endpoint.health.show-details=when-authorized
management.info.env.enabled=true
management.metrics.tags.application=${spring.application.name}
management.otlp.metrics.export.enabled=false
management.tracing.sampling.probability=1.0
management.observations.annotations.enabled=true
spring.threads.virtual.enabled=true
spring.jpa.open-in-view=false
spring.data.web.pageable.max-page-size=100
spring.mvc.problemdetails.enabled=true
```

In production, lower `management.tracing.sampling.probability` according to your traffic.

## Notes

- **Cache metrics** only show up for caches known at startup. Declare them in `api-core.cache.specs.*`.
- **Prometheus** requires a token by default. For internal scraping, add `/actuator/prometheus` to `api-core.security.public-paths` and restrict access at the network level.
- **Trace id**: every response, including 401/403, carries the `X-Trace-Id` header. The same value goes into the `traceId` field of errors and into the logs, so the client can tell which request failed.
- **Tracing** exports to `localhost:4318` if no endpoint is configured. Without a collector, use `management.tracing.export.enabled=false`.
- **Primitives in the request body**: in Jackson 3 (Spring Boot 4), a primitive field (`boolean`, `int`...) missing from the JSON makes deserialization fail before validation, and the response is a 400 without the `errors` list. In request records, use wrappers with `@NotNull` (`@NotNull Boolean active`), so the missing value becomes a validation error like any other.

## Logs

api-core logs via SLF4J 2, so it works with any backend.

Right below the Spring Boot banner, the api-core banner is shown (turn it off with `api-core.banner.enabled=false`):

```
               _
   __ _ _ __  (_)       ___ ___  _ __ ___
  / _` | '_ \ | |_____ / __/ _ \| '__/ _ \
 | (_| | |_) || |_____| (_| (_) | | |  __/
  \__,_| .__/ |_|      \___\___/|_|  \___|
       |_|

 :: api-core ::                   (v1.0.0)
```

Once the application finishes starting, it logs the version, the enabled features and the main settings:

```
api-core 1.0.0 started:
        Cache: on (default spec: maximumSize=1000,expireAfterWrite=10m, caches: [products])
        Audit: on (system auditor: system)
        Security: jwt (roles claim: roles, public paths: [/actuator/health/**, /actuator/info, /error])
        RSQL filters: on (param: filter)
        Error handler: on
        Defaults: on
```

To silence it: `logging.level.io.github.matheuslealpa.apicore.config=warn`.

The default backend is Spring Boot's Logback. To use Log4j2, also exclude `spring-boot-starter-logging` from the api-core dependency:

```xml
<dependency>
    <groupId>io.github.matheuslealpa</groupId>
    <artifactId>api-core</artifactId>
    <version>${api-core.version}</version>
    <exclusions>
        <exclusion>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-logging</artifactId>
        </exclusion>
    </exclusions>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-log4j2</artifactId>
</dependency>
```

## Overriding

Every bean is `@ConditionalOnMissingBean`: declaring your own replaces the default.

| To replace | Declare |
|---|---|
| The whole security setup | `SecurityFilterChain` |
| Role conversion | `JwtAuthenticationConverter` |
| CORS | `CorsConfigurationSource` named `corsConfigurationSource` |
| Cache | `CacheManager` |
| Auditor | `AuditorAware<String>` |
| Error handling | `ResponseEntityExceptionHandler` |

## Build

```bash
JAVA_HOME=/path/to/jdk-25 mvn verify
```

The build runs [NullAway](https://github.com/uber/NullAway) (via Error Prone) over the main code: every package is `@NullMarked`, and a possibly-null value without `@Nullable` breaks compilation. The flags Error Prone needs on Maven's JVM live in `.mvn/jvm.config`.
