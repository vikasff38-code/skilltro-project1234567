# SkillTro

SkillTro is a React + Vite frontend and Java 17 / Spring Boot REST backend. Spring Data JPA stores accounts, profiles, skills and fictional work evidence, knowledge assessments, opportunities, applications, and worker-to-opportunity match snapshots in a local file-backed H2 database.

The existing frontend uses the `/api` routes and cookie sessions. Vite proxies API calls to Spring Boot on port 8080 during local development.

## Requirements

- Java 17 or newer (the Maven Wrapper pins Maven 3.9.9)
- Node.js 20 or newer and npm
- No PostgreSQL server, Docker, Supabase, or database download is needed

## Start the backend (PowerShell)

```powershell
Set-Location C:\Users\chviv\skilltro
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-26.0.1'
$env:SPRING_PROFILES_ACTIVE = 'local'
.\mvnw.cmd spring-boot:run
```

Change `JAVA_HOME` if your JDK is installed elsewhere. The first run downloads Maven dependencies, applies `src/main/resources/db/migration/V1__initial_schema.sql` through Flyway, creates `data\skilltro.mv.db`, and adds the synthetic demo records. The H2 database is file-based and survives backend restarts. Flyway owns schema creation; Hibernate validates the schema at startup.

The H2 console is disabled by default. Enable it only for local development by activating the `local` profile. The console is then at [http://localhost:8080/h2-console](http://localhost:8080/h2-console). Use JDBC URL `jdbc:h2:file:./data/skilltro;DB_CLOSE_ON_EXIT=FALSE`, username `sa`, and the empty password unless you set `H2_DATABASE_PASSWORD`.

To run without the H2 console, omit the `local` profile or set a non-local active profile before starting:

```powershell
$env:SPRING_PROFILES_ACTIVE = 'production'
.\mvnw.cmd spring-boot:run
```

Unset it after use with `Remove-Item Env:SPRING_PROFILES_ACTIVE -ErrorAction SilentlyContinue`.

## Database configuration

The default connection and credentials are in `src/main/resources/application.properties`. Override them in PowerShell when needed:

```powershell
$env:H2_DATABASE_URL = 'jdbc:h2:file:./data/skilltro;DB_CLOSE_ON_EXIT=FALSE'
$env:H2_DATABASE_USERNAME = 'sa'
$env:H2_DATABASE_PASSWORD = ''
```

`.env.example` is a reference only; Spring Boot does not load `.env` automatically. Do not commit database passwords. The local H2 default uses an empty password and is intended for a local development database. The H2 console is restricted to loopback access and disabled unless the `local` profile is active.

## Start the frontend (second PowerShell window)

```powershell
Set-Location C:\Users\chviv\skilltro
npm ci
npm run dev
```

Open [http://localhost:5173](http://localhost:5173). Vite forwards `/api` requests to `http://localhost:8080`. The backend allows the local Vite origins listed by `SKILLTRO_CORS_ALLOWED_ORIGINS`.

## Demo data

On startup, the idempotent seeder adds **20 fictional workers, 10 fictional employer profiles, 30 fictional opportunities, 40 fictional skill-evidence entries, 30 assessment questions, 10 synthetic knowledge-check results, and up to 600 worker/opportunity match snapshots**. It checks stable demo account emails and record relationships so restarts do not duplicate seeded records. It does not overwrite existing profile data, and it never creates applications or hiring outcomes.

Demo worker and opportunity cards carry a “Demo data” label. Evidence and seeded assessment results are explicitly identified as fictional; they are not endorsements, real assessments, or verified credentials. A match score is an explainable profile fit indicator, not a hiring probability or hiring outcome.

Demo sign-ins:

- Worker: `worker.demo@skilltro.local` / `worker-demo-123` (Ravi Kumar)
- Employer: `employer.demo@skilltro.local` / `employer-demo-123`

The passwords exist only for local presentation data. Seeded applications are intentionally empty.

## Matching behavior

The backend uses one matching calculation for live API responses and persisted snapshots. The score weights required-skill overlap (60%), experience fit (15%), evidence coverage among matched required skills (15%), and location fit (10%). Remote opportunities match workers in any location. Each response includes the match percentage, matched and missing required skills, relevant fictional or user-supplied evidence, experience/location checks, and a plain-language explanation. Preferred skills are shown separately and do not replace missing requirements.

## Run checks

From the repository root in PowerShell:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-26.0.1'
.\mvnw.cmd clean test
npm run build
```

The backend tests use isolated in-memory H2 databases, so they do not modify the local presentation database. To smoke-test the running application, log in and retrieve database-backed matches:

```powershell
$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$loginBody = @{ email = 'worker.demo@skilltro.local'; password = 'worker-demo-123' } | ConvertTo-Json
Invoke-RestMethod -Uri 'http://localhost:8080/api/auth/login' -Method Post -WebSession $session -ContentType 'application/json' -Body $loginBody
Invoke-RestMethod -Uri 'http://localhost:8080/api/worker/jobs' -Method Get -WebSession $session
```

Employer worker discovery is available at `/api/employer/workers?skill=Electrical%20wiring&location=Hyderabad`. Worker opportunities accept optional `search` and `location` query parameters. The API requires a session login for those routes.

## Key files

- `src/main/resources/application.properties`: H2 connection, Flyway, Hibernate validation, CORS.
- `src/main/resources/application-local.properties`: local-only H2 console.
- `src/main/resources/db/migration/V1__initial_schema.sql`: H2 schema creation script.
- `src/main/java/org/skilltro/Domain.java`: persisted entities and relationships.
- `src/main/java/org/skilltro/Repositories.java`: Spring Data repositories.
- `src/main/java/org/skilltro/SkillTroService.java`: API use cases, persistence, and match snapshots.
- `src/main/java/org/skilltro/MatchingRules.java`: shared explainable matching calculation.
- `src/main/java/org/skilltro/SeedData.java`: idempotent fictional presentation data.
- `src/main/java/org/skilltro/Api.java`: REST routes and session handling.
- `src/App.jsx`: existing React interface and API client.
