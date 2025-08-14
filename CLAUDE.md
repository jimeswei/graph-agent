# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Spring Boot 3.4.5 application that serves as an intelligent graph agent system. It implements a multi-service MCP (Model Control Protocol) architecture for knowledge graph querying. The system provides streaming chat interfaces and manages multiple specialized MCP services for graph algorithms, content details, and general queries.

## Common Development Commands

### Build and Run
```bash
# Build the project
mvn clean compile

# Run tests
mvn test

# Run the application
mvn spring-boot:run

# Package the application
mvn clean package
```

### Database Setup
The application uses MySQL at `192.168.3.78:3307`. Execute the SQL files in order:
1. `create_tables.sql` - Basic table structure (if exists)
2. `graph-agent.sql` - Main schema and data setup

Database configuration is in `application.yml` with connection pooling via HikariCP.

## System Architecture

### Core Services Architecture
The system operates as a proxy/gateway that coordinates multiple specialized MCP services:

- **Main Application** (Port 58220): Spring Boot gateway handling authentication and request routing
- **knowledge-graph-algorithm-service** (Port 5821): Graph algorithm processing

### Key Components

#### Authentication System
- JWT-based authentication with Spring Security
- `JwtAuthenticationFilter` processes Bearer tokens
- `AuthController` handles login/authentication endpoints
- User entities stored in MySQL with JPA

#### Streaming Chat System
- `ChatStreamController` provides `/api/chat/stream/proxy` endpoint
- `ChatStreamService` uses WebClient for reactive streaming to external MCP services
- Supports Server-Sent Events (SSE) for real-time responses
- Request proxying to external graph agent at `http://192.168.3.78:48558`

#### MCP Tool Integration
- `McpToolResultService` extracts and processes MCP tool call results
- Database persistence for tool results and analysis sessions
- Caching with Redis for performance optimization

#### Entity Extraction and Processing
- `ExtractEntityService` performs AI-powered entity extraction from messages using DeepSeek-R1 model
- `AgentProcessService` orchestrates the complete processing pipeline:
  - Extracts entities from input messages
  - Saves extraction results to graph_cache table (content field)
  - Returns streaming MCP tool results via `McpToolResultService`
- ThreadId serves as both session identifier and cache key

### Data Layer
- **MySQL Database**: User management, session data, tool results, graph cache
- **JPA/Hibernate**: ORM with DDL auto-update disabled (ddl-auto: none)
- **Connection Pool**: HikariCP with optimized settings
- **Graph Database Integration**: External graph databases via MCP services
- **Entity Storage**: `GraphCache` entity stores extracted entities in content field with threadId mapping

## Configuration

### Application Properties
Key configuration in `application.yml`:
- Server port: 58220
- Database: MySQL at 192.168.3.78:3307
- External chat service: http://192.168.3.78:48558
- JWT secret and expiration settings
- Logging with file output to `logs/mcp-tool-analysis.log`
- LLM API configuration for DeepSeek-R1 model via DashScope

### Security Configuration
- Spring Security with JWT filter chain
- CORS enabled for cross-origin requests
- Public endpoints: `/api/auth/**`
- Protected endpoints require valid JWT tokens

## Development Guidelines

### Package Structure
```
com.datacent.agent/
├── config/          # Security and application configuration
├── controller/      # REST endpoints and request handling
├── dto/            # Data transfer objects
├── entity/         # JPA entities for database mapping
├── repository/     # Spring Data JPA repositories
├── security/       # Authentication filters and security components
├── service/        # Business logic and external service integration
└── util/           # Utilities (JWT, response parsing, etc.)
```

### Key Patterns
- **Reactive Streams**: Uses WebFlux for streaming responses, especially in `AgentProcessService.process()`
- **Proxy Pattern**: Main app proxies requests to specialized MCP services
- **Repository Pattern**: Spring Data JPA for data access
- **DTO Pattern**: Separate DTOs for API contracts
- **Service Layer**: Business logic separated from controllers
- **Pipeline Pattern**: `AgentProcessService` coordinates entity extraction → storage → MCP streaming

### Testing
- JUnit 5 for unit testing
- Spring Boot Test for integration testing
- Test classes in `src/test/java/com/datacent/agent/`
- Run specific test: `mvn test -Dtest=ClassName`
- Run specific test method: `mvn test -Dtest=ClassName#methodName`
- Run all tests in package: `mvn test -Dtest=com.datacent.agent.controller.*`

### Dependencies
- Spring Boot 3.4.5 (Web, WebFlux, Security, Data JPA)
- MySQL Connector 8.0.33
- JWT (jjwt) 0.12.6
- Fastjson2 2.0.57 for JSON processing
- Lombok for boilerplate reduction
- JUnit 5.10.0 for testing

## External Integrations

### MCP Services
The application integrates with three external MCP services that provide specialized graph querying capabilities. Each service exposes different tools for graph analysis, content retrieval, and relationship queries.

### LLM Integration
- **DashScope API**: Alibaba Cloud's AI service platform
- **DeepSeek-R1 Model**: Used for entity extraction from natural language messages
- **Entity Template**: Uses `doc/entities.json` as extraction template for consistent output format

### Database Connections
- Primary MySQL database for application data
- External graph databases accessed via MCP services
- Redis caching layer for performance optimization

# important-instruction-reminders
Do what has been asked; nothing more, nothing less.
NEVER create files unless they're absolutely necessary for achieving your goal.
ALWAYS prefer editing an existing file to creating a new one.
NEVER proactively create documentation files (*.md) or README files. Only create documentation files if explicitly requested by the User.
# important-instruction-reminders
Do what has been asked; nothing more, nothing less.
NEVER create files unless they're absolutely necessary for achieving your goal.
ALWAYS prefer editing an existing file to creating a new one.
NEVER proactively create documentation files (*.md) or README files. Only create documentation files if explicitly requested by the User.