# Repository Guidelines

## Project Structure & Module Organization
Backend sources live in `src/main/java/com/slimbahael/beauty_center`, split by concern (`config`, `controller`, `dto`, `model`, `repository`, `security`, `service`). Keep the dependency flow controller → service → repository and store templates/config in `src/main/resources`. Assets upload to `uploads/products`. The React client is nested in `beauty-center-frontend/src` with feature folders (`components`, `pages`, `contexts`, `services`). Backend tests belong in `src/test/java/...` mirroring the package tree; frontend specs sit next to the component (`ComponentName.test.js`).

## Build, Test, and Development Commands
`./mvnw spring-boot:run -Dspring.profiles.active=dev` boots the API, while `./mvnw clean verify` performs a full build plus tests. Start MongoDB with `docker-compose up -d mongo` if you do not have a local instance. Frontend contributors run `cd beauty-center-frontend && npm install`, then `npm start` for development, `npm run build` for production bundles, and `npm test` for Jest/RTL suites.

## Coding Style & Naming Conventions
Target Java 17 with 4-space indentation. Use Lombok constructors (`@RequiredArgsConstructor`), suffix DTOs with `Request`/`Response`, and expose REST endpoints under `/api/public` or `/api/admin` in dedicated `*Controller` classes. Mongo documents stay in `model` with `@Document`. React files follow ES6 modules, 2-space indentation, PascalCase components, and camelCase hooks/utilities. Secrets belong in environment variables; never add live keys to `application.properties` or commits.

## Testing Guidelines
Default backend tests rely on JUnit 5 and Spring Boot slices (`@WebMvcTest`, `@DataMongoTest`, Mockito). Name files `<Type>Test` and cover both happy-path and validation branches for new logic. Frontend tests use Jest with React Testing Library; co-locate specs with components and mock network calls via jest mocks or MSW. Failing or skipped suites must be called out in the PR description.

## Commit & Pull Request Guidelines
History shows concise, imperative commits (`removed demo cred`, `added privacy and cookies pages`); continue that tone, optionally adding a subsystem prefix (`auth: refresh tokens`). Every PR should outline the change, list impacted endpoints or UI flows, link issues, and include screenshots or sample payloads where relevant. State which commands you ran (`./mvnw test`, `npm test`) and describe configuration updates such as new environment variables, SSL changes, or database indexes.

## Security & Configuration Tips
Secrets, Stripe keys, mail passwords, and SSL material are parameterized in `src/main/resources/application.properties`; override them via environment variables or deployment config instead of editing the tracked file. Replace `devcert.p12` with your own keystore for local SSL, and scrub anything under `uploads/` before pushing. When adding endpoints, revisit CORS settings (`spring.web.cors.*`) and ensure `@PreAuthorize` roles match the service-level authorization checks.
