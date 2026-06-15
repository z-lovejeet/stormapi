# ⚡ StormAPI

> A production-grade API Performance Testing Platform built with Java 21 + Spring Boot 3.4 + React + TypeScript.

**🚧 Under Active Development**

## Tech Stack

| Layer | Technology |
|---|---|
| **Backend** | Java 21, Spring Boot 3.4, Spring Data JPA, WebSocket STOMP |
| **Frontend** | React 18, TypeScript, Vite, Recharts |
| **Database** | H2 (dev), PostgreSQL (prod) |
| **DevOps** | Docker, Docker Compose, GitHub Actions |

## Quick Start

### Prerequisites
- Java 21+
- Node.js 20+
- Docker (optional)

### Backend
```bash
cd backend
./mvnw spring-boot:run
```
Backend starts at `http://localhost:8080`

### Frontend
```bash
cd frontend
npm install
npm run dev
```
Frontend starts at `http://localhost:5173`

### Docker (Full Stack)
```bash
docker-compose up
```

## License

[MIT](LICENSE)
