# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Spring Boot 3.4.5 application that implements a streaming chat proxy service with MCP (Model Context Protocol) tool result extraction and analysis. It provides RESTful endpoints for user authentication, streaming chat proxying, and real-time analysis of MCP tool execution results from external graph agent services.

## Key Architecture Components

- **Spring Boot Web + WebFlux**: Hybrid architecture using traditional MVC for REST endpoints and reactive WebClient for external service calls
- **JWT Authentication**: Spring Security with JWT tokens for API access control
- **Stream Processing**: Real-time SSE (Server-Sent Events) data processing with immediate database persistence
- **JPA Data Layer**: Spring Data JPA with MySQL for persistent storage of analysis sessions and tool results
- **MCP Tool Analysis**: Real-time extraction and persistence of MCP tool execution results from streaming responses

## Core Technologies

- Java 17
- Spring Boot 3.4.5 (Web + WebFlux + Security + Data JPA)
- MySQL 8.0 with HikariCP connection pooling
- FastJSON2 for JSON processing
- JWT (io.jsonwebtoken) for authentication
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

- **Port**: 58220 (configured in application.yml)
- **External Graph Agent API**: http://192.168.3.78:48558 (streaming chat backend)
- **Database**: MySQL at 192.168.3.78:3307/graph-agent
- **Service Name**: chat-stream-proxy-service

## Core Service Architecture

### Service Layer Responsibilities

1. **ChatStreamService**: Handles HTTP communication with external graph agent API using WebClient
2. **McpToolResultService**: Real-time stream processing and MCP tool result extraction with immediate database persistence
3. **ChatRequestBuilderService**: Constructs complete MCP-enabled requests with predefined server configurations
4. **McpToolResultQueryService**: Provides query capabilities for extracted tool results and graph cache data
5. **AuthService**: JWT-based user authentication and authorization

### Controller Layer

1. **ChatStreamController**: Main streaming chat proxy with authentication (`POST /api/chat/stream/proxy`)
2. **McpToolResultQueryController**: Query interface for tool results and graph cache data
3. **AuthController**: User registration and login endpoints

### Data Layer Architecture

The application uses three main entity types:
- **AnalysisSession**: Session tracking with threadId as primary identifier
- **McpToolResult**: MCP tool execution results with real-time persistence
- **ToolCallName**: Tool invocation metadata and arguments

## Key Implementation Patterns

### Stream Processing with Real-Time Persistence

The core pattern involves processing SSE streams and immediately persisting extracted data:

```java
// McpToolResultService.extractMcpToolResults(streamData, threadId)
// - Uses threadId as sessionId (not auto-generated)  
// - Real-time chunk processing with immediate database writes
// - Atomic counters for statistics tracking
```

### MCP Tool Result Extraction

The service extracts two types of data from streaming responses:
1. **Tool Call Definitions**: From `tool_calls` arrays containing name, id, type, args
2. **Tool Results**: From chunks containing `tool_call_id` with execution results

### Database Design

- **Unified Session Management**: Uses `threadId` directly as `sessionId` for consistency
- **Real-Time Updates**: Session status updated on stream completion/failure
- **Graph Cache Integration**: Links extracted tool results to graph visualization data

## External Service Integration

The application acts as an intelligent proxy that:
- Forwards chat requests to external graph agent with pre-configured MCP settings
- Includes 3 knowledge graph MCP server configurations
- Supports both auto-generated and user-provided thread_ids
- Streams responses while simultaneously extracting and persisting tool results

## Authentication & Security

- JWT tokens with 24-hour expiration
- Protected endpoints: `/api/chat/*` and `/api/mcp-tool-results/*`
- Public endpoints: `/api/auth/*` and `/api/mcp-tool-results/examples`
- Spring Security configuration with JWT filter chain

## Important Development Notes

1. **Thread Safety**: MCP result extraction uses AtomicInteger counters and proper synchronization
2. **Error Handling**: Graceful stream error recovery with session status updates  
3. **Database Transactions**: Individual entity saves for real-time persistence (not batch operations)
4. **Method Signatures**: `extractMcpToolResults(streamData, threadId)` expects threadId parameter
5. **Session Management**: threadId serves as sessionId - no auto-generation of session identifiers