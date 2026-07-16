# URL Shortener Service

A Spring Boot service that turns long URLs into short codes and redirects visitors back to the original link.

## Features

- `POST /shorten` — shorten a URL, optionally with a custom alias
- `GET /{code}` — 301 redirect to the original URL (404 for unknown codes)
- Idempotent deduplication — shortening the same URL twice returns the same code
- Custom aliases — provide your own short code; 409 if it's taken
- URL validation — only http/https, normalized before storage
- Collision-free codes — 7-char Base62, UNIQUE constraint + bounded retry

## Prerequisites

- **Java 17+** (JDK, not just JRE)
- Maven 3.9+ is **not** required — the included `./mvnw` wrapper downloads it automatically

Verify Java is installed:
```bash
java -version   # should show 17 or higher
```

No database setup required — the service uses H2 in-memory by default.

## Build & Run

```bash
# Clone and enter the project
cd url-shortener

# Build (compiles + runs all tests)
./mvnw clean package

# Run the service
./mvnw spring-boot:run
```

The service starts on `http://localhost:8080`.

## Test

```bash
# Run all tests (unit + integration)
./mvnw test
```

Test coverage:
- **Unit tests** — Base62 generator (charset, length, uniqueness across 100K codes), URL validator (normalization, scheme/host enforcement, edge cases), alias validator (length/charset rules)
- **Integration tests** — full HTTP round-trips via MockMvc: shorten→redirect, 404 for unknowns, idempotent dedup, custom aliases, alias conflicts (409), invalid URLs/aliases (400), normalization dedup, multi-code for one URL

## Try It

```bash
# Shorten a URL
curl -X POST http://localhost:8080/shorten \
  -H "Content-Type: application/json" \
  -d '{"url": "https://example.com/very/long/path?ref=123"}'

# Response:
# {"code":"aB3xY7q","shortUrl":"http://localhost:8080/aB3xY7q","longUrl":"https://example.com/very/long/path?ref=123","created":true}

# Follow the short link (will 301 redirect)
curl -I http://localhost:8080/aB3xY7q

# Shorten with a custom alias
curl -X POST http://localhost:8080/shorten \
  -H "Content-Type: application/json" \
  -d '{"url": "https://example.com/page", "alias": "mypage"}'

# Unknown code returns 404
curl -I http://localhost:8080/doesNotExist
```

## H2 Console

Available at `http://localhost:8080/h2-console` during development.

- JDBC URL: `jdbc:h2:mem:urlshortener`
- Username: `sa`
- Password: (empty)

## Configuration

Key properties in `application.properties`:

| Property | Default | Description |
|----------|---------|-------------|
| `app.shortcode.length` | `7` | Characters per generated code (4–32) |
| `spring.datasource.url` | `jdbc:h2:mem:urlshortener` | Swap to MySQL/Postgres for persistence across restarts |

## Project Structure

```
src/main/java/com/shiva/url_shortener/
├── config/              # ShortCodeProperties (externalized config)
├── entity/              # UrlMapping JPA entity
├── exception/           # Domain exceptions (InvalidUrl, AliasAlreadyExists, etc.)
├── repository/          # Spring Data JPA repository
├── service/             # Core logic: generator, validator, hasher, service
└── web/                 # Controller, exception handler, DTOs
```

## Design Decisions

See [WRITEUP.md](WRITEUP.md) for the full one-page design write-up covering trade-offs, AI usage, and what's missing.
