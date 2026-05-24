# ApplyTabAI

ApplyTabAI is an AI-assisted internship and entry-level job application tracker.

The backend lives in `applytabai-api` and is currently set up for Phase 1 foundation work:

- Java 25, Spring Boot 4, Maven, PostgreSQL, Spring Data JPA, validation, actuator, Docker Compose support, and Testcontainers.
- Domain-first package roots for users, resume files, jobs, applications, preferences, common infrastructure, configuration, and AI foundation code.
- Base API response wrapper, global exception handling, UUID primary key strategy, and timestamp base entity support.
- Spring AI OpenAI and Spring AI PDF document reader dependencies are included. OpenAI chat is disabled by default so the app starts without an API key.

## Local Development

```bash
cd applytabai-api
cp .env.sample .env
./mvnw spring-boot:run
```

To enable OpenAI later, set these values in `applytabai-api/.env`:

```properties
SPRING_AI_MODEL_CHAT=openai
OPENAI_API_KEY=your-api-key
OPENAI_CHAT_MODEL=gpt-4o-mini
```

## Verification

```bash
cd applytabai-api
JAVA_HOME=/usr/lib/jvm/java-25-openjdk ./mvnw verify
```
