-- ===========================================
-- MySQL表结构定义
-- 数据库: graph-agent
-- 连接信息: 192.168.3.78:3307 root/123456
-- ===========================================

USE `graph-agent`;

-- 1. 创建mcp_tool_results表
-- 存储MCP工具调用的结果数据
CREATE TABLE `mcp_tool_results` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `thread_id` VARCHAR(100) NOT NULL COMMENT '线程ID',
    `agent` VARCHAR(50) NOT NULL COMMENT '代理名称', 
    `result_id` VARCHAR(100) NOT NULL COMMENT '结果ID',
    `role` VARCHAR(20) NOT NULL COMMENT '角色',
    `content` LONGTEXT NOT NULL COMMENT '内容',
    `tool_call_id` VARCHAR(100) NOT NULL COMMENT '工具调用ID',
    `created_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_thread_id` (`thread_id`),
    INDEX `idx_agent` (`agent`),
    INDEX `idx_tool_call_id` (`tool_call_id`),
    INDEX `idx_result_id` (`result_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MCP工具调用结果表';

-- 2. 创建tool_call_names表
-- 存储工具调用的名称和参数信息
CREATE TABLE `tool_call_names` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `name` VARCHAR(100) NOT NULL COMMENT '工具名称',
    `call_id` VARCHAR(100) NOT NULL COMMENT '调用ID',
    `type` VARCHAR(50) NOT NULL COMMENT '调用类型',
    `args` TEXT COMMENT '参数信息（JSON格式）',
    `call_index` INT COMMENT '调用索引',
    `created_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_name` (`name`),
    INDEX `idx_call_id` (`call_id`),
    INDEX `idx_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工具调用名称表';

-- 3. 创建analysis_sessions表（可选）
-- 存储分析会话信息
CREATE TABLE `analysis_sessions` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `session_id` VARCHAR(100) NOT NULL COMMENT '会话ID',
    `success` BOOLEAN NOT NULL COMMENT '是否成功',
    `message` TEXT COMMENT '消息内容',
    `results_count` INT DEFAULT 0 COMMENT '结果数量',
    `tool_calls_count` INT DEFAULT 0 COMMENT '工具调用数量',
    `timestamp` BIGINT COMMENT '时间戳',
    `created_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_session_id` (`session_id`),
    INDEX `idx_timestamp` (`timestamp`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分析会话表';



