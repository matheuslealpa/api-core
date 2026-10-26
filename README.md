# api-core

## (Produtividade-DEV)

Starter Spring Boot para APIs REST. Basta adicionar a dependência para ter, já configurados:

| Feature | O que vem pronto |
|---|---|
| **Cache** | `@Cacheable` com Caffeine, specs por cache e estatísticas expostas como métricas |
| **JPA** | `BaseEntity` (id UUID, `@Version`, auditoria), `BaseRepository` (CRUD + `Specification` + `findByIdOrThrow`), `PageResponse` |
| **Filtros** | `@RsqlFilter` converte `?filter=` em `Specification` (RSQL), com whitelist de campos e erros 400 em `problem+json` |
| **Auditoria** | `createdBy`/`updatedBy`/`createdAt`/`updatedAt` preenchidos com o usuário do JWT |
| **Segurança** | OAuth2 Resource Server (JWT), stateless, roles a partir de claim (inclusive Keycloak), CORS, `@PreAuthorize`, 401/403 em `problem+json` |
| **Observabilidade** | Actuator (health/probes, info, metrics, prometheus), tracing Micrometer + OpenTelemetry (OTLP), `traceId` nos logs, `@Observed` |
| **Erros** | Handler global com `ProblemDetail` (validação, 404, 409, 422, 403) incluindo `traceId` |
| **Lombok** | Disponível no projeto, com `@SuperBuilder` funcionando nas entidades que estendem `BaseEntity`/`AuditableEntity` |
| **Runtime** | Virtual threads ligadas, `open-in-view` desligado |
| **Null safety** | API anotada com [JSpecify](https://jspecify.dev) (`@NullMarked`/`@Nullable`), no mesmo padrão do Spring 7: Kotlin e ferramentas como NullAway enxergam a nulidade |

Requisitos: **Java 25** e **Spring Boot 4.1**.

## Uso

```bash
mvn install   # publica no repositório local
```

```xml
<dependency>
    <groupId>io.github.matheuslealpa</groupId>
    <artifactId>api-core</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

Configuração mínima do projeto consumidor (`application.yml`):

```yaml
spring:
  application:
    name: minha-api
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://auth.exemplo.com/realms/meu-realm

management:
  opentelemetry:
    tracing:
      export:
        otlp:
          endpoint: http://otel-collector:4318/v1/traces
```

Sem `issuer-uri` ou `jwk-set-uri` a aplicação não sobe, pois não há como validar o JWT.

### API sem segurança

Para APIs internas ou que não precisam de autenticação:

```yaml
api-core:
  security:
    enabled: false
```

Todas as rotas ficam liberadas (inclusive o Actuator) e o JWT deixa de ser exigido, então `issuer-uri` não é necessário. O CORS continua valendo, a auditoria grava o `system-auditor` e métodos com `@PreAuthorize` respondem 403.

### Entidades e repositórios

```java
@Entity
public class Produto extends BaseEntity {
    private String nome;
}

public interface ProdutoRepository extends BaseRepository<Produto, UUID> {
}

@GetMapping
PageResponse<ProdutoDto> listar(Pageable pageable) {
    return PageResponse.of(repository.findAll(pageable), ProdutoDto::from);
}
```

Para entidades com id próprio, estenda `AuditableEntity`. Lance `ResourceNotFoundException` (404) ou `BusinessException` (422) e o handler global monta a resposta.

### Filtros RSQL

```java
@GetMapping
PageResponse<ProdutoDto> listar(@RsqlFilter(allow = {"nome", "preco", "createdAt"}) Specification<Produto> filtro,
        Pageable pageable) {
    return PageResponse.of(repository.findAll(filtro, pageable), ProdutoDto::from);
}
```

| Requisição | Significado |
|---|---|
| `?filter=nome==tecl*` | `nome` começa com "tecl" |
| `?filter=preco=gt=100;preco=le=500` | `;` = E |
| `?filter=nome==mouse,nome==teclado` | `,` = OU |
| `?filter=nome=in=(mouse,teclado)` | lista de valores |
| `?filter=createdAt=bt=(2026-01-01T00:00:00Z,2026-12-31T23:59:59Z)` | intervalo |

Outros operadores: `!=`, `=lt=`, `=ge=`, `=out=`, `=isnull=`, `=notnull=`, `=like=`, `=icase=`. Sem o parâmetro, a consulta não é filtrada. Sintaxe inválida, campo fora do `allow` ou valor de tipo errado resultam em 400.

Com `allow` vazio, **qualquer** atributo da entidade, inclusive de relacionamentos, pode ser filtrado. Em APIs públicas, liste os campos permitidos.

### Lombok

O Lombok vem junto com o api-core, mas a partir do Java 23 o `javac` só executa annotation processors declarados explicitamente. Sem isso, as anotações são ignoradas e a compilação falha com `cannot find symbol`. No `pom.xml` do projeto:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <annotationProcessorPaths>
            <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
                <version>${lombok.version}</version> <!-- vem do spring-boot-starter-parent -->
            </path>
        </annotationProcessorPaths>
    </configuration>
</plugin>
```

Entidades:

```java
@Entity
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Produto extends BaseEntity {
    private String nome;
}

Produto produto = Produto.builder().nome("Teclado").build();
```

O builder expõe só os campos da entidade: `id`, `version` e os campos de auditoria ficam de fora, porque são gerenciados pelo JPA. Não use `@Data` nem `@EqualsAndHashCode`: o `equals`/`hashCode` do `BaseEntity` é `final` (igualdade por id, segura com proxies), e o compilador acusa erro se o Lombok tentar gerar outro.

### Regras de autorização extras

```java
@Bean
AuthorizeRequestsCustomizer adminRules() {
    return auth -> auth.requestMatchers("/admin/**").hasRole("ADMIN");
}
```

## Propriedades

| Propriedade | Padrão | Descrição |
|---|---|---|
| `api-core.banner.enabled` | `true` | Banner do api-core abaixo do banner do Spring Boot (segue `spring.main.banner-mode`) |
| `api-core.defaults.enabled` | `true` | Aplica os defaults opinativos abaixo |
| `api-core.cache.enabled` | `true` | Liga o `CaffeineCacheManager` |
| `api-core.cache.default-spec` | `maximumSize=1000,expireAfterWrite=10m` | Spec dos caches sem configuração própria |
| `api-core.cache.specs.<nome>` | – | Spec por cache, ex.: `maximumSize=500,expireAfterWrite=5m` |
| `api-core.audit.enabled` | `true` | Liga `@EnableJpaAuditing` |
| `api-core.audit.system-auditor` | `system` | Auditor quando não há usuário autenticado |
| `api-core.audit.username-claims` | `preferred_username` | Claims consultadas para o auditor (fallback: `sub`) |
| `api-core.security.enabled` | `true` | `false` libera todas as rotas, sem JWT |
| `api-core.security.public-paths` | `/actuator/health/**, /actuator/info, /error` | Rotas sem autenticação |
| `api-core.security.roles-claim` | `roles` | Claim das roles; Keycloak: `realm_access.roles` |
| `api-core.security.role-prefix` | `ROLE_` | Prefixo aplicado às roles |
| `api-core.security.method-security` | `true` | Habilita `@PreAuthorize` |
| `api-core.security.cors.allowed-origins` | – | Origens liberadas (aceita padrões, ex.: `https://*.exemplo.com`) |
| `api-core.security.cors.allowed-methods` | `GET, POST, PUT, PATCH, DELETE, OPTIONS` | |
| `api-core.security.cors.allowed-headers` | `*` | |
| `api-core.security.cors.exposed-headers` | – | |
| `api-core.security.cors.allow-credentials` | `false` | |
| `api-core.security.cors.max-age` | `1h` | |
| `api-core.web.enabled` | `true` | Liga o `GlobalExceptionHandler` |
| `api-core.rsql.enabled` | `true` | Liga o `@RsqlFilter` |
| `api-core.rsql.param-name` | `filter` | Nome padrão do parâmetro da expressão RSQL |

### Defaults aplicados

Ficam com a menor precedência, então qualquer valor no `application.yml` ou em variável de ambiente prevalece:

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
spring.mvc.problemdetails.enabled=true
```

Em produção, reduza `management.tracing.sampling.probability` conforme o volume.

## Observações

- **Métricas de cache** só aparecem para caches conhecidos no startup. Declare-os em `api-core.cache.specs.*`.
- **Prometheus** exige token por padrão. Para scrape interno, adicione `/actuator/prometheus` em `api-core.security.public-paths` e restrinja o acesso pela rede.
- **Tracing** exporta para `localhost:4318` se o endpoint não for configurado. Sem coletor, use `management.tracing.export.enabled=false`.

## Logs

O api-core loga via SLF4J 2, então funciona com qualquer backend.

Logo abaixo do banner do Spring Boot, aparece o do api-core (desligue com `api-core.banner.enabled=false`):

```
               _
   __ _ _ __  (_)       ___ ___  _ __ ___
  / _` | '_ \ | |_____ / __/ _ \| '__/ _ \
 | (_| | |_) || |_____| (_| (_) | | |  __/
  \__,_| .__/ |_|      \___\___/|_|  \___|
       |_|

 :: api-core ::                   (v1.0.0)
```

Quando a aplicação termina de subir, ele registra a versão, as features ligadas e as principais configurações:

```
api-core 1.0.0 started:
        Cache: on (default spec: maximumSize=1000,expireAfterWrite=10m, caches: [produtos])
        Audit: on (system auditor: system)
        Security: jwt (roles claim: roles, public paths: [/actuator/health/**, /actuator/info, /error])
        RSQL filters: on (param: filter)
        Error handler: on
        Defaults: on
```

Para silenciar: `logging.level.io.github.matheuslealpa.apicore.config=warn`.

O backend padrão é o Logback, do Spring Boot. Para usar Log4j2, exclua o `spring-boot-starter-logging` também da dependência do api-core:

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

## Sobrescrevendo

Todo bean é `@ConditionalOnMissingBean`: declarar o seu substitui o padrão.

| Para trocar | Declare |
|---|---|
| Segurança inteira | `SecurityFilterChain` |
| Conversão de roles | `JwtAuthenticationConverter` |
| CORS | `CorsConfigurationSource` |
| Cache | `CacheManager` |
| Auditor | `AuditorAware<String>` |
| Tratamento de erros | `ResponseEntityExceptionHandler` |

## Build

```bash
JAVA_HOME=/caminho/para/jdk-25 mvn verify
```

O build roda o [NullAway](https://github.com/uber/NullAway) (via Error Prone) sobre o código principal: todo pacote é `@NullMarked`, e um valor possivelmente nulo sem `@Nullable` quebra a compilação. As flags que o Error Prone precisa na JVM do Maven ficam em `.mvn/jvm.config`.
