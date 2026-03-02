-- Phase 8.1: Dashboard 导航页 - 环境与地址层级
-- 环境（一组）-> 地址项（多个）

CREATE TABLE IF NOT EXISTS dashboard_nav_environment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL COMMENT '环境名称，如 A、B',
    description VARCHAR(512) DEFAULT NULL COMMENT '可选描述',
    sort_order INT DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS dashboard_nav_address (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    environment_id BIGINT NOT NULL COMMENT '所属环境',
    short_name VARCHAR(50) NOT NULL COMMENT '短名称，如 A1、A2',
    label VARCHAR(100) NOT NULL COMMENT '显示名/标签，如 my、th',
    url VARCHAR(1024) DEFAULT NULL COMMENT '地址 URL',
    remark VARCHAR(512) DEFAULT NULL COMMENT '备注',
    sort_order INT DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (environment_id) REFERENCES dashboard_nav_environment(id) ON DELETE CASCADE
);

CREATE INDEX idx_nav_address_env ON dashboard_nav_address(environment_id);
