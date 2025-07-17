# MCP工具调用结果数据库存储系统

## 概述

本系统基于Spring Boot 3.4.5实现，用于将MCP工具调用结果保存到MySQL数据库中。系统解析JSON数据并将其存储到三个主要表中：`mcp_tool_results`、`tool_call_names`和`analysis_sessions`。

## 数据库配置

### 连接信息
- **数据库地址**: 192.168.3.78:3307
- **数据库名**: graph-agent
- **用户名**: root
- **密码**: 123456
- **数据库类型**: MySQL 8.4

### 表结构

#### 1. mcp_tool_results表
存储MCP工具调用的结果数据
```sql
CREATE TABLE `mcp_tool_results` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `thread_id` VARCHAR(100) NOT NULL,
    `agent` VARCHAR(50) NOT NULL,
    `result_id` VARCHAR(100) NOT NULL,
    `role` VARCHAR(20) NOT NULL,
    `content` LONGTEXT NOT NULL,
    `tool_call_id` VARCHAR(100) NOT NULL,
    `created_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

#### 2. tool_call_names表
存储工具调用的名称和参数信息
```sql
CREATE TABLE `tool_call_names` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(100) NOT NULL,
    `call_id` VARCHAR(100) NOT NULL,
    `type` VARCHAR(50) NOT NULL,
    `args` TEXT,
    `call_index` INT,
    `created_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

#### 3. analysis_sessions表
存储分析会话信息
```sql
CREATE TABLE `analysis_sessions` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `session_id` VARCHAR(100) NOT NULL,
    `success` BOOLEAN NOT NULL,
    `message` TEXT,
    `results_count` INT DEFAULT 0,
    `tool_calls_count` INT DEFAULT 0,
    `timestamp` BIGINT,
    `created_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

## 系统架构

### 核心组件

1. **实体类 (Entity)**
   - `McpToolResult.java` - MCP工具调用结果实体
   - `ToolCallName.java` - 工具调用名称实体
   - `AnalysisSession.java` - 分析会话实体

2. **数据访问层 (Repository)**
   - `McpToolResultRepository.java` - MCP工具调用结果数据访问
   - `ToolCallNameRepository.java` - 工具调用名称数据访问
   - `AnalysisSessionRepository.java` - 分析会话数据访问

3. **业务逻辑层 (Service)**
   - `McpToolResultService.java` - 核心业务逻辑，处理数据保存和查询

4. **控制器 (Controller)**
   - `ResponseAnalysisController.java` - 处理HTTP请求，集成数据库操作

## API接口

### 主要接口

#### 1. 提取MCP工具调用结果并保存到数据库
```
POST /api/analysis/extract-mcp-tool-results
```

**请求体**:
```json
{
    "message": "分析下周杰伦和刘德华共同有哪些共同好友？"
}
```

**响应**: 返回提取的结果并自动保存到数据库

#### 2. 查询数据库统计信息
```
GET /api/analysis/database/statistics
```

**响应**: 返回数据库中的统计信息

#### 3. 根据线程ID查询结果
```
GET /api/analysis/database/results/{threadId}
```

#### 4. 根据工具名称查询工具调用
```
GET /api/analysis/database/tools/{toolName}
```

#### 5. 获取所有工具名称
```
GET /api/analysis/database/tools
```

#### 6. 获取最近的分析会话
```
GET /api/analysis/database/sessions
```

## 数据流程

1. **接收请求**: Controller接收包含消息的JSON请求
2. **流式处理**: 调用外部图谱agent API获取流式响应
3. **数据解析**: 解析流式响应，提取包含`tool_call_id`的数据块和工具调用信息
4. **数据保存**: 将解析后的数据保存到MySQL数据库的三个表中
5. **响应返回**: 返回处理结果给客户端

## 配置文件

### application.yml
```yaml
spring:
  datasource:
    url: jdbc:mysql://192.168.3.78:3307/graph-agent?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: root
    password: 123456
    driver-class-name: com.mysql.cj.jdbc.Driver
  
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.MySQL8Dialect
```

### Maven依赖
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <version>8.0.33</version>
</dependency>
```

## 使用说明

### 1. 数据库初始化
执行 `create_tables.sql` 文件中的SQL语句创建数据库表结构。

### 2. 启动应用
```bash
mvn spring-boot:run
```

### 3. 测试接口
使用POST请求调用 `/api/analysis/extract-mcp-tool-results` 接口，传入消息内容。

### 4. 查询数据
使用提供的查询接口查看保存到数据库中的数据。

## 特性

- **自动数据库操作**: 无需手动SQL操作，系统自动处理数据保存
- **完整的CRUD操作**: 支持数据的增删改查
- **事务支持**: 确保数据一致性
- **错误处理**: 完善的异常处理机制
- **日志记录**: 详细的操作日志
- **统计功能**: 提供数据统计和分析功能

## 注意事项

1. 确保MySQL数据库服务正常运行
2. 数据库连接配置正确
3. 表结构与实体类保持一致
4. 注意数据库连接池的配置
5. 生产环境中建议使用数据库连接加密