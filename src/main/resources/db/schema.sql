-- ===========================================
-- AICR - AI 智能研发助手 数据库初始化脚本
-- ===========================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS aicr_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE aicr_db;

-- ===========================================
-- 文档管理表
-- ===========================================
CREATE TABLE IF NOT EXISTS document (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    name VARCHAR(255) NOT NULL COMMENT '文档名称',
    original_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
    file_type VARCHAR(50) NOT NULL COMMENT '文件类型(pdf/word/txt/md)',
    file_size BIGINT NOT NULL COMMENT '文件大小(字节)',
    file_path VARCHAR(500) NOT NULL COMMENT '文件存储路径',
    summary TEXT COMMENT '文档内容摘要',
    status TINYINT DEFAULT 0 COMMENT '状态:0-待处理 1-处理中 2-已完成 3-失败',
    chunk_count INT DEFAULT 0 COMMENT '分块数量',
    creator_id BIGINT COMMENT '创建者ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除标志:0-未删除 1-已删除',
    INDEX idx_creator_id (creator_id),
    INDEX idx_status (status),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档表';

-- ===========================================
-- 文档分块表
-- ===========================================
CREATE TABLE IF NOT EXISTS document_chunk (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    document_id BIGINT NOT NULL COMMENT '所属文档ID',
    chunk_index INT NOT NULL COMMENT '分块序号',
    content LONGTEXT NOT NULL COMMENT '分块内容',
    content_length INT NOT NULL COMMENT '分块内容长度',
    vector_id VARCHAR(100) COMMENT '向量ID(Pinecone中)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_document_id (document_id),
    INDEX idx_vector_id (vector_id),
    FOREIGN KEY (document_id) REFERENCES document(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档分块表';

-- ===========================================
-- 聊天会话表
-- ===========================================
CREATE TABLE IF NOT EXISTS chat_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    session_id VARCHAR(64) NOT NULL UNIQUE COMMENT '会话唯一标识',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    title VARCHAR(255) COMMENT '会话标题',
    document_ids VARCHAR(500) COMMENT '关联的知识库文档ID列表(逗号分隔)',
    status TINYINT DEFAULT 0 COMMENT '状态:0-活跃 1-已归档 2-已删除',
    message_count INT DEFAULT 0 COMMENT '消息数量',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    last_active_time DATETIME COMMENT '最后活跃时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除标志',
    INDEX idx_session_id (session_id),
    INDEX idx_user_id (user_id),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天会话表';

-- ===========================================
-- 聊天消息表
-- ===========================================
CREATE TABLE IF NOT EXISTS chat_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    session_id BIGINT NOT NULL COMMENT '所属会话ID',
    role VARCHAR(20) NOT NULL COMMENT '消息角色:user/assistant/system',
    content LONGTEXT NOT NULL COMMENT '消息内容',
    message_type VARCHAR(20) DEFAULT 'text' COMMENT '消息类型:text/code/image',
    reference_chunk_ids VARCHAR(500) COMMENT '引用的文档块ID列表(逗号分隔)',
    token_count INT COMMENT '消耗的Token数',
    response_time BIGINT COMMENT '响应时间(毫秒)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_session_id (session_id),
    INDEX idx_create_time (create_time),
    FOREIGN KEY (session_id) REFERENCES chat_session(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天消息表';

-- ===========================================
-- 长期记忆表
-- ===========================================
CREATE TABLE IF NOT EXISTS long_term_memory (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    content TEXT NOT NULL COMMENT '记忆内容',
    memory_type VARCHAR(50) COMMENT '记忆类型:preference/fact/goal/habit',
    vector_id VARCHAR(100) COMMENT '向量ID',
    importance INT DEFAULT 5 COMMENT '重要性评分(1-10)',
    access_count INT DEFAULT 0 COMMENT '访问次数',
    last_access_time DATETIME COMMENT '最后访问时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除标志',
    INDEX idx_user_id (user_id),
    INDEX idx_memory_type (memory_type),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='长期记忆表';

-- ===========================================
-- 代码审查记录表
-- ===========================================
CREATE TABLE IF NOT EXISTS code_review_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    review_id VARCHAR(64) NOT NULL UNIQUE COMMENT '审查任务ID',
    project_name VARCHAR(255) NOT NULL COMMENT '项目名称',
    branch_name VARCHAR(255) COMMENT '分支名称',
    commit_id VARCHAR(100) COMMENT '提交ID/PR ID',
    file_path VARCHAR(500) COMMENT '审查的文件路径',
    code_content LONGTEXT COMMENT '代码内容或diff',
    review_result JSON COMMENT '审查结果(JSON格式)',
    critical_count INT DEFAULT 0 COMMENT 'Critical问题数量',
    warning_count INT DEFAULT 0 COMMENT 'Warning问题数量',
    info_count INT DEFAULT 0 COMMENT 'Info问题数量',
    status TINYINT DEFAULT 0 COMMENT '状态:0-待审查 1-审查中 2-已完成 3-失败',
    creator_id BIGINT COMMENT '创建者ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    complete_time DATETIME COMMENT '完成时间',
    INDEX idx_review_id (review_id),
    INDEX idx_project_name (project_name),
    INDEX idx_status (status),
    INDEX idx_creator_id (creator_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代码审查记录表';
