<p align="center">
  <img src="https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 21" />
  <img src="https://img.shields.io/badge/Spring_Boot-3.4-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white" alt="Spring Boot 3.4" />
  <img src="https://img.shields.io/badge/React-19-61DAFB?style=for-the-badge&logo=react&logoColor=black" alt="React 19" />
  <img src="https://img.shields.io/badge/TypeScript-5.x-3178C6?style=for-the-badge&logo=typescript&logoColor=white" alt="TypeScript" />
  <img src="https://img.shields.io/badge/Vite-6-646CFF?style=for-the-badge&logo=vite&logoColor=white" alt="Vite 6" />
  <img src="https://img.shields.io/badge/License-MIT-blue?style=for-the-badge" alt="MIT License" />
</p>

<h1 align="center">⚡ StormAPI</h1>

<p align="center">
  <strong>A full-stack performance testing platform for HTTP APIs.</strong><br/>
  Configure, execute, monitor, and analyze load tests — all from a single dashboard.
</p>

<p align="center">
  <a href="#features">Features</a> •
  <a href="#architecture">Architecture</a> •
  <a href="#tech-stack">Tech Stack</a> •
  <a href="#getting-started">Getting Started</a> •
  <a href="#api-reference">API Reference</a> •
  <a href="#project-structure">Project Structure</a> •
  <a href="#contributing">Contributing</a> •
  <a href="#license">License</a>
</p>

---

## Overview

StormAPI is an open-source performance testing platform that enables developers to stress-test their HTTP APIs without leaving the browser. It ships **six specialized test engines** — Load, Stress, Spike, Soak, Breakpoint, and Scalability — each designed for a distinct testing scenario. Results are streamed in real time over WebSockets, visualized through interactive charts, and exportable as JSON, CSV, HTML, or PDF reports.

The backend is built on **Spring Boot 3.4 (Java 21)** with Spring Security, JWT authentication, OAuth2 (Google & GitHub), and an in-memory H2 database. The frontend is a **React 19 + TypeScript** SPA built with Vite, featuring a dark-themed glassmorphism UI with Recharts-powered analytics dashboards.

---

## Features

### Test Engines

| Engine | Purpose | Key Metric |
|--------|---------|------------|
| **Load** | Constant virtual user load at a steady rate | Avg response time, throughput |
| **Stress** | Progressively ramp beyond system capacity | Error rate at overload |
| **Spike** | Sudden burst of users, then recover | Recovery time (ms) |
| **Soak** | Extended-duration endurance test | Degradation slope (ms/sec) |
| **Breakpoint** | Step-wise increase to find the breaking point | Breakpoint user count |
| **Scalability** | Measure throughput/latency scaling curve | Dual-axis throughput vs. latency |

### Real-Time Monitoring

- **WebSocket streaming** — live metrics broadcast every second via STOMP over SockJS
- **Live dashboard** — active users, requests/sec, avg response time, error rate, P95 latency
- **Interactive timeline** — switch between metrics (throughput, latency, error rate, P95, active users)

### Analytics & Charts

- **Donut Chart** — success/failure ratio with center percentage label
- **Percentile Bar Chart** — P50 / P75 / P90 / P95 / P99 latency distribution
- **Status Code Chart** — HTTP status code distribution (2xx, 3xx, 4xx, 5xx)
- **Response Time Histogram** — latency bucket distribution
- **Timeline Chart** — time-series performance metrics with tab selector
- **Scalability Curve** — dual Y-axis throughput vs. latency line chart

### Export & Reporting

- **JSON** — full test result payload
- **CSV** — time-series metric snapshots
- **HTML** — self-contained styled report (Thymeleaf template)
- **PDF** — rendered from the HTML report via OpenHTMLtoPDF

### Authentication

- **Local auth** — email/password registration and login with JWT
- **OAuth2** — Google and GitHub social login
- **Session management** — HttpOnly cookie-based JWT tokens

### UI Design

- Dark-themed interface with glassmorphism effects
- Animated landing page with floating orbs and scroll reveals
- Fully responsive layout with modular CSS Modules
- Premium component library (buttons, badges, modals, tabs, data tables)

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        Frontend (React 19)                      │
│  Landing • Login • Register • Dashboard • Builder • Monitor     │
│  Recharts • STOMP/SockJS • Axios • CSS Modules                 │
└──────────────────────┬───────────────────┬──────────────────────┘
                       │ REST API (HTTP)   │ WebSocket (STOMP)
┌──────────────────────▼───────────────────▼──────────────────────┐
│                     Backend (Spring Boot 3.4)                   │
│                                                                 │
│  ┌─────────────┐  ┌──────────────┐  ┌────────────────────────┐ │
│  │   Auth       │  │  Test CRUD   │  │  Metrics + WebSocket   │ │
│  │  Controller  │  │  Controller  │  │     Controller         │ │
│  └──────┬──────┘  └──────┬───────┘  └───────────┬────────────┘ │
│         │                │                      │              │
│  ┌──────▼──────┐  ┌──────▼───────┐  ┌───────────▼────────────┐ │
│  │  Security   │  │    Test      │  │  LiveMetricsBroadcaster │ │
│  │  + JWT      │  │ Orchestrator │  │  + SessionRegistry      │ │
│  │  + OAuth2   │  └──────┬───────┘  └────────────────────────┘ │
│  └─────────────┘         │                                     │
│                   ┌──────▼───────┐                              │
│                   │  6 Test      │                              │
│                   │  Engines     │                              │
│                   └──────┬───────┘                              │
│                          │                                      │
│  ┌───────────────────────▼──────────────────────────────────┐  │
│  │           Spring Data JPA + H2 Database                  │  │
│  │   TestConfig • TestResult • MetricSnapshot • RequestLog  │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │              Export Module                                │  │
│  │   JSON • CSV • HTML (Thymeleaf) • PDF (OpenHTMLtoPDF)    │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

---

## Tech Stack

### Backend

| Technology | Version | Purpose |
|-----------|---------|---------|
| Java | 21 | Language runtime |
| Spring Boot | 3.4.1 | Application framework |
| Spring Security | 6.x | Authentication & authorization |
| Spring Data JPA | 3.x | Database access & ORM |
| Spring WebSocket | 6.x | Real-time metrics streaming |
| H2 Database | 2.x | Embedded relational database |
| JJWT | 0.12.6 | JWT token generation & validation |
| Thymeleaf | 3.x | HTML report template engine |
| OpenHTMLtoPDF | 1.1.37 | HTML-to-PDF conversion |
| SpringDoc OpenAPI | 2.8.4 | Swagger UI & API documentation |
| Lombok | 1.18.36 | Boilerplate reduction |
| JUnit 5 + Mockito | — | Testing framework |

### Frontend

| Technology | Version | Purpose |
|-----------|---------|---------|
| React | 19 | UI framework |
| TypeScript | 5.x | Type-safe JavaScript |
| Vite | 6.3.5 | Build tool & dev server |
| React Router | 7.x | Client-side routing |
| Axios | 1.9.x | HTTP client |
| Recharts | 2.15.x | Charting library |
| @stomp/stompjs | 7.1.x | WebSocket STOMP client |
| Lucide React | 0.507 | Icon library |
| Vitest | 3.2.x | Unit testing framework |
| Testing Library | 16.x | Component testing utilities |

---

## Getting Started

### Prerequisites

- **Java 21+** (JDK)
- **Node.js 18+** and npm
- **Maven 3.9+** (or use the included `mvnw` wrapper)

### 1. Clone the Repository

```bash
git clone https://github.com/z-lovejeet/stormapi.git
cd stormapi
```

### 2. Configure Environment Variables

```bash
cp .env.example backend/.env.local
```

Edit `backend/.env.local` with your credentials:

```env
GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret
GITHUB_CLIENT_ID=your-github-client-id
GITHUB_CLIENT_SECRET=your-github-client-secret
JWT_SECRET=your-jwt-secret-minimum-64-characters-long
```

> **Note:** OAuth2 credentials are optional. Local email/password authentication works without them.

### 3. Start the Backend

```bash
cd backend
./mvnw spring-boot:run
```

The API server starts at **http://localhost:8080**. Swagger UI is available at `/swagger-ui.html`.

### 4. Start the Frontend

```bash
cd frontend
npm install
npm run dev
```

The development server starts at **http://localhost:5173**.

### 5. Docker (Alternative)

```bash
docker-compose up --build
```

This starts both the backend (port 8080) and frontend (port 5173).

---

## API Reference

### Authentication

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/auth/register` | Register a new account |
| `POST` | `/api/auth/login` | Login with email & password |
| `POST` | `/api/auth/logout` | Logout and clear session |
| `GET` | `/api/auth/status` | Check authentication status |
| `GET` | `/api/auth/me` | Get current user profile |

### Test Management

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/tests` | Create a new test configuration |
| `GET` | `/api/tests` | List all tests (paginated) |
| `GET` | `/api/tests/{id}` | Get test configuration by ID |
| `PUT` | `/api/tests/{id}` | Update test configuration |
| `DELETE` | `/api/tests/{id}` | Delete test configuration |
| `POST` | `/api/tests/{id}/start` | Start test execution |
| `POST` | `/api/tests/{id}/cancel` | Cancel a running test |
| `GET` | `/api/tests/{id}/status` | Get current test status |
| `GET` | `/api/tests/{id}/result` | Get latest test result |
| `GET` | `/api/tests/{id}/results` | Get all results for a test |

### Metrics

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/metrics/{resultId}/snapshots` | Time-series metric snapshots |
| `GET` | `/api/metrics/{resultId}/request-logs` | Paginated request logs |
| `GET` | `/api/metrics/{resultId}/status-codes` | HTTP status code distribution |
| `GET` | `/api/metrics/{resultId}/histogram` | Response time histogram |

### Results

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/results/compare` | Compare two test results |

### Export

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/export/{resultId}/json` | Download result as JSON |
| `GET` | `/api/export/{resultId}/csv` | Download metrics as CSV |
| `GET` | `/api/export/{resultId}/html` | Download HTML report |
| `GET` | `/api/export/{resultId}/pdf` | Download PDF report |

### WebSocket

| Protocol | Endpoint | Description |
|----------|----------|-------------|
| STOMP | `/ws` | WebSocket connection endpoint |
| Subscribe | `/topic/metrics/{testId}` | Live metrics stream |

> Full interactive API docs are available at **http://localhost:8080/swagger-ui.html** when the backend is running.

---

## Project Structure

```
stormapi/
├── backend/
│   ├── src/main/java/com/stormapi/
│   │   ├── auth/                    # Authentication (controllers, OAuth2, JWT)
│   │   ├── common/                  # Shared models, exceptions, converters
│   │   ├── config/                  # Security, WebSocket, CORS configuration
│   │   ├── engine/                  # Test execution engines
│   │   │   ├── core/                #   TestEngine interface, RampUpStrategy
│   │   │   ├── metrics/             #   MetricsCollector, PercentileCalculator
│   │   │   ├── analysis/            #   EngineAnalysisResult record
│   │   │   ├── load/                #   LoadTestEngine
│   │   │   ├── stress/              #   StressTestEngine
│   │   │   ├── spike/               #   SpikeTestEngine
│   │   │   ├── soak/                #   SoakTestEngine
│   │   │   ├── breakpoint/          #   BreakpointTestEngine
│   │   │   └── scalability/         #   ScalabilityTestEngine
│   │   ├── export/                  # Report generation (JSON, CSV, HTML, PDF)
│   │   ├── metrics/                 # Metric snapshots, request logs, DTOs
│   │   ├── test/                    # Test config, results, orchestration
│   │   └── websocket/               # Live metrics broadcast over STOMP
│   ├── src/main/resources/
│   │   ├── templates/               # Thymeleaf HTML report template
│   │   └── application.properties
│   └── pom.xml
│
├── frontend/
│   ├── src/
│   │   ├── api/                     # Axios API modules
│   │   ├── components/
│   │   │   ├── charts/              # 6 Recharts visualization components
│   │   │   ├── common/              # Reusable UI components
│   │   │   ├── layout/              # Navbar, Sidebar
│   │   │   └── test-results/        # KPI cards, metrics table, export
│   │   ├── context/                 # React auth context
│   │   ├── hooks/                   # Custom React hooks
│   │   ├── pages/                   # Route-level page components
│   │   ├── types/                   # TypeScript type definitions
│   │   └── utils/                   # Formatting utilities
│   ├── package.json
│   └── vite.config.ts
│
├── docker-compose.yml
├── .env.example
└── LICENSE
```

---

## Running Tests

### Backend

```bash
cd backend
./mvnw test
```

Runs 13 test classes covering all 6 test engines, metrics calculation, WebSocket broadcasting, concurrency, and data converters.

### Frontend

```bash
cd frontend
npm test
```

Runs 20+ test files covering chart components, UI components, custom hooks, utility functions, API client, and page-level integration tests.

---

## Key Design Decisions

- **Engine Pattern** — Each test type (Load, Stress, Spike, etc.) is encapsulated in its own engine class implementing a common `TestEngine` interface. The `TestOrchestrator` manages the lifecycle of all engines through a unified orchestration flow.

- **Strategy Pattern** — `RampUpStrategy` handles the gradual increase of virtual users, decoupled from the engine logic itself.

- **Streaming Architecture** — Test results are streamed in real time via WebSocket (STOMP protocol) with a `LiveMetricsBroadcaster` publishing snapshots every second. The frontend subscribes to per-test topics for live updates.

- **Percentile Calculation** — The `PercentileCalculator` computes P50/P75/P90/P95/P99 latencies from a sorted response time array using linear interpolation.

- **Export Pipeline** — HTML reports are rendered server-side with Thymeleaf. PDF generation reuses the same HTML template by piping the rendered string through OpenHTMLtoPDF — zero template duplication.

- **Cookie-Based JWT** — Authentication tokens are stored in `HttpOnly` cookies (not localStorage) for XSS protection, with CSRF tokens for state-changing requests.

---

## Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/your-feature`)
3. Commit your changes (`git commit -m 'feat: add your feature'`)
4. Push to the branch (`git push origin feature/your-feature`)
5. Open a Pull Request

Please follow the existing code style and include tests for new features.

---

## License

This project is licensed under the **MIT License** — see the [LICENSE](LICENSE) file for details.

---

<p align="center">
  Built by <a href="https://github.com/z-lovejeet"><strong>Lovejeet Singh</strong></a>
</p>
