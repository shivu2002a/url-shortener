# Design Write-Up

## 1. What I asked the AI to do vs. what I decided myself

I used Claude (AI assistant) to scaffold the implementation — entity, repository, service, controller, exception handling, and tests. The AI generated the boilerplate and wiring code.

**What I decided myself:**

- **Short-code algorithm**: I chose random Base62, 7 chars, UNIQUE constraint + bounded retry over sequential encoding or hash-truncation. The AI presented three options; I picked the one that balances unpredictability, collision safety, and simplicity.
- **Duplicate-URL policy**: I chose idempotent deduplication for auto-generated codes (same URL → same code) but honour custom aliases independently (one URL can have multiple codes). This is a deliberate design split, not an accident.
- **Custom alias conflict handling**: I chose 409 Conflict over silently falling back to a random code. Explicit failure is easier to reason about on the client side.
- **URL validation strictness**: Only http/https, require a host, reject everything else with 400. No localhost, no ftp.
- **Redirect type**: 301 Permanent, per the spec. I'm aware this trades away the ability to update or expire codes later.
- **Logging approach**: I corrected the AI when it tried to use Lombok's `@Slf4j` (which wasn't being processed by the annotation processor). I directed it to use explicit `LoggerFactory.getLogger(...)` instead.

## 2. Where I overrode or corrected the AI

1. **Lombok `@Slf4j` failure**: The AI applied `@Slf4j` annotations across four classes. The build failed because the Lombok annotation processor wasn't generating the `log` field in this Spring Boot 4 snapshot environment. I caught this and told the AI to use explicit `Logger` declarations instead.

2. **Test dependency imports**: Spring Boot 4 moved packages (`AutoConfigureMockMvc` moved to `org.springframework.boot.webmvc.test.autoconfigure`, Jackson 3 lives under `tools.jackson`). The AI initially used the old Spring Boot 3 packages and I had to guide the fix.

3. **Design pattern scope**: When I asked about design patterns, the AI proposed making the Strategy pattern explicit via externalized config. I accepted that but rejected additional patterns (Chain of Responsibility for validation, Factory for entities) as over-engineering for this size.

4. **Entity structure**: I asked the AI to add a `custom` boolean flag to distinguish auto-generated from alias mappings, enabling the dedup-only-for-auto-generated behaviour. This was my design call; the AI initially had dedup apply to all mappings.

## 3. Biggest trade-offs

### Trade-off 1: Random codes vs. sequential encoding

**Chose**: Random Base62 with UNIQUE constraint + retry  
**Alternative**: Auto-increment ID → Base62 encode (structurally collision-free)  
**Why**: Random codes are non-enumerable (can't guess `/1`, `/2`, `/3` to walk the dataset), work without a global sequence in distributed setups, and don't leak the total number of URLs. The cost is a theoretical (but practically impossible at 62^7 ≈ 3.5 trillion) collision, handled by the retry loop. The UNIQUE constraint makes correctness independent of randomness.

### Trade-off 2: Idempotent dedup vs. always-mint-new

**Chose**: Same URL (auto-generated) → returns existing code; custom aliases always create new  
**Alternative**: Every request mints a fresh code regardless  
**Why**: Idempotent behaviour is friendlier for clients (stable short URLs, no keyspace waste). The split between auto and custom keeps it predictable: you only get dedup when you didn't ask for a specific alias. The cost is an extra indexed lookup on the URL hash before insert.

### Trade-off 3: 301 Permanent vs. 302/307 Temporary redirect

**Chose**: 301 Moved Permanently (per assignment spec)  
**Alternative**: 302 would let us change targets or add analytics later  
**Why**: The spec says 301. In production I'd likely use 302 to retain the ability to update mappings, expire codes, or count clicks. With 301, browsers cache aggressively and may never hit the server again for the same code.

## 4. What's missing / what I'd do with another day

- **Rate limiting**: No protection against abuse (mass creation of codes). I'd add Spring's `RateLimiter` or a Bucket4j filter.
- **Expiration / TTL**: Codes live forever. I'd add an `expires_at` column and a scheduled cleanup job.
- **Analytics**: No click tracking. A lightweight event table (code, timestamp, referrer, user-agent) would enable basic analytics.
- **Persistent storage**: H2 in-memory means all data is lost on restart. In prod, swap to MySQL/Postgres via config.
- **API security**: No auth. For a multi-tenant version I'd add API keys or OAuth2.
- **Input sanitization for open-redirect**: The service currently redirects to any valid http/https URL. A malicious user could shorten a phishing URL. I'd consider a blocklist or safe-browsing check.
- **Observability**: Structured logging is in place but I'd add Micrometer metrics (shorten rate, redirect rate, collision count, dedup hit rate) and health checks.
- **Docker / deployment**: A Dockerfile and docker-compose (with MySQL) for one-command deployment.
