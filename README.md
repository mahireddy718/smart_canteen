# Smart Canteen Token System

A Java web app (Spring Boot 3, Thymeleaf, Spring Security, JPA) where students order food, receive a
token number, and follow a first-in-first-out kitchen queue. Deploys to Render with Docker + PostgreSQL.

## Features
**Customers:** register / login, browse and search menu by category, cart, checkout (cash or UPI at counter),
kitchen note, automatic daily token (T-001, T-002, ...), live queue position and estimated wait,
cancel while still "Placed", order history, printable token, profile and password change, dark mode.

**Kitchen staff:** FIFO queue (oldest first), "Call next in line", start / ready / collected buttons,
verify a token at the counter, cancel with stock restored, auto-refresh.

**Admin:** dashboard (orders, revenue, queue, low stock, best sellers), food CRUD with stock, prep time,
availability, restock, user management (roles, deactivate), all orders with status filter, CSV export,
reports (7-day revenue, busiest hours, top items).

**Public:** live token board at `/display` for a canteen TV (Preparing / Ready / Waiting).

Database tables: users, food_items, orders, order_items, tokens.

## Demo logins (created automatically)
| Role | Email | Password |
|---|---|---|
| Admin | admin@canteen.com | admin123 |
| Staff | staff@canteen.com | staff123 |
| Student | student@canteen.com | student123 |

Change these after the first deploy (Profile page, or Admin > Users).

## Run locally
Requires Java 17 and Maven. Uses an in-memory H2 database, so no setup is needed.
```
mvn spring-boot:run
```
Open http://localhost:8080 (data resets on every restart).

## Deploy to Render
1. Create a GitHub repository and push this folder to it.
2. In Render: **New > Blueprint**, pick the repository. `render.yaml` creates the web service
   (Docker) and a free PostgreSQL database and wires the connection variables.
3. Wait for the build to finish, then open your `.onrender.com` URL.

Manual setup instead of a Blueprint: create a PostgreSQL database, then a Web Service with runtime Docker
and these environment variables: `SPRING_PROFILES_ACTIVE=prod`, `DB_HOST`, `DB_PORT`, `DB_NAME`,
`DB_USER`, `DB_PASSWORD` (use the internal connection values from the database page). Health check path: `/health`.

Notes: the free web service sleeps when idle (first request is slow), and Render's free PostgreSQL
database expires after 30 days, so upgrade it or recreate it for long-term use.
