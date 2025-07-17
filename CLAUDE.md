# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Spring Boot 3.4.5 application that implements a streaming chat proxy service. It provides RESTful endpoints that forward user messages to an external graph agent backend service and return streaming responses using Server-Sent Events (SSE).

## Key Architecture Components

- **Spring Boot Web**: Traditional MVC web stack for REST endpoints
- **Spring WebFlux**: Reactive web client for HTTP calls to external services
- **Streaming Response**: Server-Sent Events (SSE) for real-time data streaming
- **JSON Processing**: FastJSON2 for serialization/deserialization

## Core Technologies

- Java 17
- Spring Boot 3.4.5
- Spring WebFlux (WebClient for HTTP calls)
- FastJSON2 for JSON processing
- Lombok for code generation

## Development Commands

### Build and Run
```bash
# Build the project
mvn clean compile

# Run the application
mvn spring-boot:run

# Build JAR package
mvn clean package
```

### Testing
```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=ClassName
```

## Application Configuration

- **Port**: 5822 (configured in application.yml)
- **External API**: http://192.168.3.78:48558 (graph agent backend)
- **Service Name**: chat-stream-proxy-service

## Key Service Classes

- `ChatStreamService`: Core service handling HTTP calls to external graph agent API
- `ChatStreamController`: REST controller providing streaming chat endpoints

## API Endpoints

### Streaming Chat Endpoints
- `POST /api/chat/stream` - Full-featured streaming chat with optional thread_id
- `POST /api/chat/stream/simple` - Simplified streaming chat (auto-generates thread_id)
- `GET /api/chat/stream` - GET version for testing purposes
- `GET /api/chat/examples` - API usage examples

### Request/Response Format
All endpoints accept user messages and forward them to the external graph agent service with pre-configured MCP settings for knowledge graph querying.

## External Service Integration

The application forwards requests to `POST /api/chat/stream` on the external service with:
- Complete MCP server configurations for 3 knowledge graph services
- Dynamic thread_id generation or user-provided thread_id
- Predefined query parameters (auto_accepted_plan, report_style, etc.)

## Project Structure

```
src/main/java/com/datacent/agent/
├── AgentApplication.java              # Main Spring Boot application
├── controller/ChatStreamController.java # Streaming chat REST endpoints
├── service/ChatStreamService.java     # HTTP client service for external API
└── entity/                           # Request/response entity classes
    ├── ChatMessage.java              # Chat message entity
    ├── ChatStreamRequest.java        # Main request entity
    ├── MCPServer.java               # MCP server configuration entity
    └── MCPSettings.java             # MCP settings container
```