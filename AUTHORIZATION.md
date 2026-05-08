# anu Authorization Server

A minimal [Spring Authorization Server](https://github.com/spring-projects/spring-authorization-server) instance that issues OAuth2 / OIDC tokens for the `anu` platform.

- Spring Boot 3.4.1, Spring Authorization Server, Java 21
- In-memory client (`anu-client` / `anu-secret`)
- In-memory user (`user` / `password`)
- In-memory RSA signing key (rotates on restart)

## Run

```bash
./gradlew bootRun
```

Server starts on `http://localhost:9000`. Sanity-check the discovery document:

```bash
curl http://localhost:9000/.well-known/openid-configuration
```

## Postman

Import [postman/anu-authorization-server.postman_collection.json](postman/anu-authorization-server.postman_collection.json) for ready-to-run authorization-code, refresh-token, client-credentials, introspect, JWKS, and discovery requests.

---

# Protecting a microservice with this authorization server

Use this auth server to protect a downstream microservice as an OAuth2 **resource server** that validates JWTs.

## 1. Add the dependency

**Gradle:**
```gradle
implementation 'org.springframework.boot:spring-boot-starter-oauth2-resource-server'
implementation 'org.springframework.boot:spring-boot-starter-web'
```

**Maven:**
```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
```

## 2. Point it at the auth server

`application.yml`:
```yaml
server:
  port: 8081

spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:9000
```

`issuer-uri` triggers OIDC discovery — Spring fetches `/.well-known/openid-configuration` and `/oauth2/jwks` automatically, so signature verification, expiry, and issuer checks are all wired up.

## 3. Lock down endpoints

```java
@Configuration
@EnableWebSecurity
public class ResourceServerConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/public/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/**").hasAuthority("SCOPE_read")
                .requestMatchers(HttpMethod.POST, "/api/**").hasAuthority("SCOPE_write")
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .csrf(AbstractHttpConfigurer::disable);
        return http.build();
    }
}
```

Spring maps each scope in the JWT's `scope` claim to an authority prefixed with `SCOPE_`. So the `read` scope → `SCOPE_read`.

## 4. Use the principal

```java
@RestController
@RequestMapping("/api")
class HelloController {

    @GetMapping("/me")
    public Map<String, Object> me(@AuthenticationPrincipal Jwt jwt) {
        return Map.of(
            "subject", jwt.getSubject(),
            "scopes", jwt.getClaimAsStringList("scope"),
            "issuer", jwt.getIssuer().toString()
        );
    }
}
```

## 5. Register the resource's scopes on the client

In [AuthorizationServerConfig.java](src/main/java/com/anu/authorization/config/AuthorizationServerConfig.java) the client already has `read` and `write` scopes — request them when getting a token:

```
scope=openid profile read write
```

If you add new scopes (e.g. `orders:read`), add them to the `RegisteredClient` builder too.

## 6. Call the protected microservice

```bash
# 1. Get a token (client_credentials is simplest for service-to-service)
curl -u anu-client:anu-secret \
  -d 'grant_type=client_credentials&scope=read' \
  http://localhost:9000/oauth2/token

# 2. Call the resource server with the access_token
curl -H "Authorization: Bearer eyJhbGciOi..." \
  http://localhost:8081/api/me
```

## Things to watch for

- **JWT signing key is in-memory.** Every restart of the auth server rotates the RSA key, invalidating previously issued tokens. Fine for dev; for prod, persist the key (load from a keystore or KMS).
- **Issuer-uri must match exactly.** The JWT's `iss` claim must equal what the resource server resolved from `issuer-uri`. If you put the auth server behind a proxy or change ports, update both ends.
- **Clock skew.** If services run on different machines, default 60s leeway usually suffices; tune via `JwtTimestampValidator` if needed.
- **Network reachability.** The resource server fetches JWKS from the auth server at startup *and* on key rotation — they must be able to reach each other. In Docker Compose use the service name (`http://auth-server:9000`) as `issuer-uri`.
- **Method-level security.** Add `@EnableMethodSecurity` and use `@PreAuthorize("hasAuthority('SCOPE_read')")` for finer-grained checks.
- **Roles vs scopes.** Out of the box you get scopes only. To map custom claims (e.g. `roles`) to authorities, supply a `JwtAuthenticationConverter` bean.
