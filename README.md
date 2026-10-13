# Pokémon Interface

> **Built entirely with AI in about 6 hours. No code was written by hand.** I used Claude Code with an experimental
> workflow: hexagonal architecture, ATDD for the outer loop and TDD for the inner loop. How it works:
> [`GENAI_ASSIGNMENT.md`](GENAI_ASSIGNMENT.md).

A full-stack app that browses Pokémon live from [PokeAPI](https://pokeapi.co/docs/v2), copies the ones you
choose into a local database, and lets signed-in users add their own fields (localized name, region, tags) on top.

| Part | Stack | Directory |
|---|---|---|
| Backend | Java 21, Spring Boot 4 (MVC, Data JPA, Security, Validation), MariaDB, Caffeine, JWT | [`BE/`](BE) |
| Frontend | React 18, TypeScript, Vite, React Router, TanStack Query | [`FE/`](FE) |
| Specs | User stories with example-mapped rules | [`doc/specs/`](doc/specs) |

---

## Quick start

### Option A: everything in Docker (recommended)

You only need Docker with Compose v2.

```bash
docker compose up --build -d
```

| Container | What it runs | URL |
|---|---|---|
| `pokemon-frontend` | nginx serving the React build and proxying `/api` to the backend | http://localhost:3000 |
| `pokemon-backend` | Spring Boot jar on a Java 21 JRE | http://localhost:8080/api/pokemon |
| `pokemon-mariadb` | MariaDB 11.4 (data kept in the `pokemon-db-data` volume) | `localhost:3306` |

Open http://localhost:3000 and sign in with **`demo` / `demo1234`**, or click **Sign in as demo**. The backend
waits for MariaDB to report healthy before it starts.

```bash
docker compose logs -f backend    # follow the backend log
docker compose down               # stop (keeps the data)
docker compose down -v            # stop and wipe the database; demo data is seeded again on next start
```

You can set these in your shell or in a `.env` file at the repository root:

| Variable | Default | Purpose |
|---|---|---|
| `FRONTEND_PORT` | `3000` | Host port for the UI |
| `BACKEND_PORT` | `8080` | Host port for the API (e.g. `8081` if a locally run backend already uses 8080) |
| `JWT_SECRET` | dev-only value | HS256 signing key, at least 32 bytes |

Each app also builds on its own:

```bash
docker build -t pokemon-interface-be BE
docker build -t pokemon-interface-fe FE   # the BACKEND_URL env var sets where nginx proxies /api
```

### Option B: run locally for development

#### Prerequisites

| Tool | Version |
|---|---|
| JDK | 21 |
| Node.js | 18+ (tested with 22) |
| Docker | any recent version (for MariaDB) |

#### 1. Start the database

```bash
docker compose up -d mariadb  # MariaDB 11.4 on localhost:3306, db/user/password: pokemon
```

#### 2. Start the backend (port 8080)

```bash
cd BE
./mvnw spring-boot:run        # Windows: mvnw.cmd spring-boot:run
```

#### 3. Start the frontend (port 5173)

```bash
cd FE
npm install
npm run dev
```

Open http://localhost:5173 and sign in with **`demo` / `demo1234`**, or click **Sign in as demo**.

### Demonstration data

On startup the backend fills in any missing demo data, so a fresh database is ready to use right away:

- The first 10 Pokémon (Bulbasaur … Caterpie) in the local collection.
- A demo user: **`demo` / `demo1234`**.

Seeding is idempotent: it only runs when the collection is empty or the demo user is missing.

### Trying the API: IntelliJ HTTP Client

[`http/`](http) holds ready-made requests for every endpoint, including the error cases (400, 401, 404, 409):

| File | Covers |
|---|---|
| [`auth.http`](http/auth.http) | Sign in as demo (stores the JWT), register, duplicate and invalid registrations |
| [`pokemon.http`](http/pokemon.http) | Paginated list and detailed view from PokeAPI |
| [`stored-pokemon.http`](http/stored-pokemon.http) | Collection: browse, view, sync, edit, delete |

Open a file in IntelliJ IDEA and pick an environment from
[`http-client.env.json`](http/http-client.env.json): **`local`** sends requests straight to the backend on :8080,
and **`docker`** goes through nginx on :3000. Run the sign-in request first. It saves the JWT as `{{token}}`, which
the protected requests use.

---

## Configuration

### Backend: `BE/src/main/resources/application.yaml`

| Property | Default | Purpose |
|---|---|---|
| `spring.datasource.url` | `jdbc:mariadb://localhost:3306/pokemon` | MariaDB connection |
| `spring.datasource.username` / `password` | `pokemon` / `pokemon` | Matches `docker-compose.yaml` |
| `pokeapi.base-url` | `https://pokeapi.co/api/v2` | Upstream API |
| `security.jwt.secret` (env `JWT_SECRET`) | dev-only value | HS256 signing key, at least 32 bytes. **Set it outside local development.** |

### Frontend: `FE/.env` (optional, copy from `FE/.env.example`)

| Variable | Default | Purpose |
|---|---|---|
| `BACKEND_URL` | `http://localhost:8080` | Where the Vite dev server proxies `/api/*` |

The dev server proxies every `/api` request to the backend, so the browser sees a single origin and the backend
needs no CORS configuration. The frontend Docker image works the same way: nginx serves `FE/dist`, routes `/api` to
Spring Boot ([`FE/nginx.conf.template`](FE/nginx.conf.template)), and sends every other path to `index.html`.

### Overriding backend settings

Any backend property can be overridden with an environment variable (Spring relaxed binding). This is how
`docker-compose.yaml` points the backend at the `mariadb` container. The variables are `SPRING_DATASOURCE_URL`,
`SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `POKEAPI_BASE_URL` and `JWT_SECRET`.

---

## Features → user stories

| Story | What the UI does | CRUD | Auth |
|---|---|---|---|
| US-01 Enumeration | **Browse**: paginated grid with sprite, category, weight and expandable skills (moves + how learned) | Read | Public |
| US-02 Detailed view | **Pokémon page**: artwork, description, base-stat bars, evolution chain (branches shown) | Read | Public |
| US-03 Synchronisation | **Save to collection** from any card, the detail page, or *Add by Pokédex #*; **Refresh from PokeAPI** on stored ones | Create | Signed in |
| US-03 Stored list | **My collection**: paginated list of stored Pokémon with their own fields | Read | Public |
| US-04 Modification | **Edit** localized name, region and tags (validated client- and server-side); **Remove** with confirmation | Update / Delete | Signed in |
| US-05 Authentication | **Sign in**, **Register** (then signed in automatically), **Sign out**; session ends when the JWT expires | Create (user) | Public |

---

## REST API

All errors use one body: an RFC 9457 problem detail (`application/problem+json`), plus `invalidFields` on 400s.

| Method | Path | Auth | Success |
|---|---|---|---|
| `GET` | `/api/pokemon?page=0&size=20` | – | 200 page of Pokémon from PokeAPI (size ≤ 100) |
| `GET` | `/api/pokemon/{id}` | – | 200 detailed Pokémon |
| `GET` | `/api/stored-pokemon?page=0&size=20` | – | 200 page of stored Pokémon |
| `GET` | `/api/stored-pokemon/{id}` | – | 200 stored Pokémon · 404 if not stored |
| `POST` | `/api/stored-pokemon/{id}/sync` | Bearer | 201 copied · 200 refreshed |
| `PUT` | `/api/stored-pokemon/{id}` | Bearer | 200 (full replace of own fields) |
| `DELETE` | `/api/stored-pokemon/{id}` | Bearer | 204 |
| `POST` | `/api/auth/register` | – | 201 · 409 username taken |
| `POST` | `/api/auth/sign-in` | – | 200 `{ "token": "…" }` · 401 bad credentials |

---

## Frontend architecture

```
FE/src/
  api/          HTTP layer: typed DTOs mirroring the backend, fetch client, one module per resource
  auth/         JWT session: decode sub/exp, persist, auto sign-out on expiry or a 401
  hooks/        TanStack Query hooks (queries + mutations + cache keys), URL-backed pagination
  components/   Reusable UI: Layout, Pagination, cards, SyncButton, StatBars, EvolutionTree, form, dialog, toasts
  pages/        One component per route; pages compose hooks and components, with no fetch calls of their own
  styles/       One stylesheet with design tokens and light/dark themes
```

**State management**

- **Server state** lives only in TanStack Query. Mutations write the confirmed result into the cache and
  invalidate the collection's list pages, so every view stays in step without manual refetching. Previous pages
  stay on screen (dimmed) while the next one loads.
- **Session state** (the JWT) lives in a small React context, persisted to `localStorage`. The API client gets
  the token through a provider function, so it has no React dependency.
- **UI state that should survive reloads**, such as the page number and page size, lives in the URL
  (`?page=2&size=24`), so views are bookmarkable and the back button works.
- Everything else is local component state.

**Routes**

| Path | Page | Guard |
|---|---|---|
| `/` | Browse | – |
| `/pokemon/:id` | Detail + collection panel | – |
| `/collection` | My collection | Actions hidden when signed out |
| `/collection/:id/edit` | Edit own fields | Redirects to sign-in, then back |
| `/sign-in`, `/register` | Auth | – |

**UX details**

- Responsive from 320px up: fluid card grid, collapsible nav on small screens, stacked forms.
- Light and dark themes follow the OS setting; visible focus rings, labelled controls, native `<dialog>` for
  confirmations, and reduced motion respected.
- Loading skeletons, empty states, retryable errors, and toasts for every mutation.
- Client-side validation mirrors the backend limits (name/region ≤ 50, ≤ 10 unique tags of ≤ 30 chars), and
  server `invalidFields` are mapped back onto the matching inputs.

### Frontend scripts

```bash
npm run dev        # dev server with /api proxy
npm run build      # type-check + production build to dist/
npm run preview    # serve the production build
npm run typecheck  # tsc only
```

---

## Backend

Hexagonal architecture (`domain` ← `application` ← `adapter`), developed with ATDD + TDD. See
[`CLAUDE.md`](CLAUDE.md) for conventions and [`doc/specs/`](doc/specs) for the rules each story implements.

```bash
cd BE
./mvnw test                   # all tests (in-memory H2, PokeAPI stubbed)
```
