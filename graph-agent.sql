/*
 Navicat Premium Data Transfer

 Source Server         : 192.168.3.78
 Source Server Type    : MySQL
 Source Server Version : 80404
 Source Host           : 192.168.3.78:3307
 Source Schema         : graph-agent

 Target Server Type    : MySQL
 Target Server Version : 80404
 File Encoding         : 65001

 Date: 25/07/2025 11:52:46
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for analysis_sessions
-- ----------------------------
DROP TABLE IF EXISTS `analysis_sessions`;
CREATE TABLE `analysis_sessions` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `session_id` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '会话ID',
  `success` tinyint(1) NOT NULL COMMENT '是否成功',
  `message` text COLLATE utf8mb4_unicode_ci COMMENT '消息内容',
  `results_count` int DEFAULT '0' COMMENT '结果数量',
  `tool_calls_count` int DEFAULT '0' COMMENT '工具调用数量',
  `timestamp` bigint DEFAULT NULL COMMENT '时间戳',
  `created_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_session_id` (`session_id`),
  KEY `idx_timestamp` (`timestamp`)
) ENGINE=InnoDB AUTO_INCREMENT=66 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分析会话表';

-- ----------------------------
-- Table structure for graph_cache
-- ----------------------------
DROP TABLE IF EXISTS `graph_cache`;
CREATE TABLE `graph_cache` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `thread_id` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `content` text COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '内容',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_time` datetime(6) DEFAULT NULL,
  `updated_time` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_thread_id` (`thread_id`),
  KEY `idx_session_thread` (`thread_id`)
) ENGINE=InnoDB AUTO_INCREMENT=673 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='图数据缓存表';

-- ----------------------------
-- Table structure for mcp_tool_results
-- ----------------------------
DROP TABLE IF EXISTS `mcp_tool_results`;
CREATE TABLE `mcp_tool_results` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `thread_id` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '线程ID',
  `agent` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '代理名称',
  `result_id` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '结果ID',
  `role` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色',
  `content` longtext COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '内容',
  `tool_call_id` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '工具调用ID',
  `created_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_thread_id` (`thread_id`),
  KEY `idx_agent` (`agent`),
  KEY `idx_tool_call_id` (`tool_call_id`),
  KEY `idx_result_id` (`result_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1333 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MCP工具调用结果表';

-- ----------------------------
-- Table structure for tool_call_names
-- ----------------------------
DROP TABLE IF EXISTS `tool_call_names`;
CREATE TABLE `tool_call_names` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '工具名称',
  `call_id` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '调用ID',
  `type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '调用类型',
  `args` text COLLATE utf8mb4_unicode_ci COMMENT '参数信息（JSON格式）',
  `call_index` int DEFAULT NULL COMMENT '调用索引',
  `created_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_name` (`name`),
  KEY `idx_call_id` (`call_id`),
  KEY `idx_type` (`type`)
) ENGINE=InnoDB AUTO_INCREMENT=1397 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工具调用名称表';

-- ----------------------------
-- Table structure for users
-- ----------------------------
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `enabled` bit(1) NOT NULL,
  `password` varchar(255) COLLATE utf8mb4_general_ci NOT NULL,
  `role` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `username` varchar(255) COLLATE utf8mb4_general_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKr43af9ap4edm43mmtq01oddj6` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Table structure for agent_report
-- ----------------------------
DROP TABLE IF EXISTS `agent_report`;
CREATE TABLE `agent_report` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `thread_id` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '线程ID',
  `agent` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'reporter' COMMENT '代理名称',
  `content` longtext COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '完整报告内容',
  `created_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_thread_id` (`thread_id`),
  KEY `idx_agent` (`agent`),
  KEY `idx_created_time` (`created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='代理报告表';

SET FOREIGN_KEY_CHECKS = 1;


-- ----------------------------
-- Table structure for employee_config
-- ----------------------------
DROP TABLE IF EXISTS `employee_config`;
CREATE TABLE `employee_config` (
   `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '自增主键',
   `model` VARCHAR(50) NOT NULL COMMENT '模型名称',
   `thread_id` VARCHAR(100) NOT NULL COMMENT '线程ID',
   `user_id` VARCHAR(50) NOT NULL COMMENT '用户ID',
   `employee_type` VARCHAR(50) NOT NULL COMMENT '员工类型',
   `employee_desc` TEXT COMMENT '员工描述',
   `max_iterations` INT NOT NULL DEFAULT 0 COMMENT '最大迭代次数',
   `iterations_per_round` INT NOT NULL DEFAULT 0 COMMENT '每轮迭代次数',
   `max_search_results` INT NOT NULL DEFAULT 0 COMMENT '最大检索结果',
   `knowledge_base` VARCHAR(100) COMMENT '知识库',
   `mcp_service` VARCHAR(100) COMMENT 'MCP服务',
   `tools` VARCHAR(255) COMMENT '工具列表',
   `created_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
   `updated_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
   UNIQUE KEY uk_thread_employee (`thread_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工模型配置表';

-- ----------------------------
-- Table structure for current_plan
-- ----------------------------
DROP TABLE IF EXISTS `current_plan`;
CREATE TABLE `current_plan` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '自增主键',
    `thread_id` VARCHAR(100) NOT NULL COMMENT '线程ID',
    `plan_id` VARCHAR(100) NOT NULL COMMENT '计划唯一标识',
    `locale` VARCHAR(10) DEFAULT 'zh-CN' COMMENT '语言环境',
    `has_enough_context` BOOLEAN DEFAULT FALSE COMMENT '是否有足够上下文',
    `thought` TEXT COMMENT '思考过程',
    `title` VARCHAR(500) NOT NULL COMMENT '计划标题',
    `created_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_thread_id` (`thread_id`),
    UNIQUE KEY `uk_plan_id` (`plan_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='计划表';

-- ----------------------------
-- Table structure for plan_steps
-- ----------------------------
DROP TABLE IF EXISTS `plan_steps`;
CREATE TABLE `plan_steps` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '自增主键',
    `plan_id` VARCHAR(100) NOT NULL COMMENT '计划ID，关联current_plan.plan_id',
    `step_index` INT NOT NULL COMMENT '步骤序号',
    `need_get_data` BOOLEAN DEFAULT TRUE COMMENT '是否需要获取数据',
    `title` VARCHAR(500) NOT NULL COMMENT '步骤标题',
    `description` TEXT COMMENT '步骤描述',
    `step_type` VARCHAR(50) DEFAULT 'research' COMMENT '步骤类型：research/analysis/synthesis等',
    `execution_res` LONGTEXT COMMENT '执行结果',
    `status` ENUM('pending', 'in_progress', 'completed', 'failed') DEFAULT 'pending' COMMENT '执行状态',
    `created_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_plan_id` (`plan_id`),
    INDEX `idx_step_index` (`plan_id`, `step_index`),
    FOREIGN KEY (`plan_id`) REFERENCES `current_plan`(`plan_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='计划步骤表';