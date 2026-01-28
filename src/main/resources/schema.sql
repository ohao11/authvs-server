-- OIDC 客户端表
CREATE TABLE IF NOT EXISTS `oauth_clients` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '客户端ID',
  `client_id` VARCHAR(100) NOT NULL COMMENT '客户端标识',
  `client_secret` VARCHAR(255) NOT NULL COMMENT '客户端密钥（加密存储）',
  `client_name` VARCHAR(100) NOT NULL COMMENT '客户端名称',
  `grant_types` VARCHAR(500) NOT NULL COMMENT '授权类型：authorization_code,refresh_token,client_credentials,password',
  `redirect_uris` TEXT COMMENT '重定向URI',
  `post_logout_redirect_uris` TEXT COMMENT '登出后重定向URI',
  `access_token_validity` INT DEFAULT 3600 COMMENT 'Access Token有效期（秒）',
  `refresh_token_validity` INT DEFAULT 2592000 COMMENT 'Refresh Token有效期（秒）',
  `id_token_validity` INT DEFAULT 3600 COMMENT 'ID Token有效期（秒）',
  `scopes` VARCHAR(500) COMMENT '允许的作用域',
  `auto_approve` TINYINT(1) DEFAULT 0 COMMENT '是否自动授权：0-否 1-是',
  `client_type` TINYINT(1) DEFAULT 1 COMMENT '客户端类型',
  `description` VARCHAR(500) COMMENT '客户端描述',
  `logo_url` VARCHAR(255) COMMENT '客户端Logo URL',
  `home_url` VARCHAR(255) COMMENT '客户端主页URL',
  `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  `created_by` BIGINT COMMENT '创建人ID',
  `created_at` TIMESTAMP NULL COMMENT '创建时间',
  `updated_by` BIGINT COMMENT '更新人ID',
  `updated_at` TIMESTAMP NULL COMMENT '更新时间',
  UNIQUE KEY `uk_client_id` (`client_id`),
  KEY `idx_client_name` (`client_name`),
  KEY `idx_enabled` (`enabled`),
  KEY `idx_client_type` (`client_type`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='OIDC客户端表';

-- 用户表
CREATE TABLE IF NOT EXISTS `users` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名',
  `password` VARCHAR(255) NOT NULL COMMENT '密码（BCrypt加密）',
  `email` VARCHAR(100) COMMENT '邮箱',
  `phone` VARCHAR(20) COMMENT '手机号',
  `user_type` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '用户类型：1-门户用户 2-后台管理员',
  `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  `created_at` TIMESTAMP NULL COMMENT '创建时间',
  `updated_at` TIMESTAMP NULL COMMENT '更新时间',
  KEY `idx_username` (`username`),
  KEY `idx_email` (`email`),
  KEY `idx_phone` (`phone`),
  KEY `idx_user_type` (`user_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- OAuth2 授权表
CREATE TABLE IF NOT EXISTS `oauth2_authorization` (
  `id` VARCHAR(100) PRIMARY KEY,
  `registered_client_id` VARCHAR(100) NOT NULL,
  `principal_name` VARCHAR(200) NOT NULL,
  `authorization_grant_type` VARCHAR(100) NOT NULL,
  `authorized_scopes` VARCHAR(1000),
  `attributes` TEXT,
  `state` VARCHAR(500),
  `authorization_code_value` LONGBLOB,
  `authorization_code_issued_at` TIMESTAMP NULL,
  `authorization_code_expires_at` TIMESTAMP NULL,
  `authorization_code_metadata` TEXT,
  `access_token_value` LONGBLOB,
  `access_token_issued_at` TIMESTAMP NULL,
  `access_token_expires_at` TIMESTAMP NULL,
  `access_token_metadata` TEXT,
  `access_token_type` VARCHAR(100),
  `access_token_scopes` VARCHAR(1000),
  `refresh_token_value` LONGBLOB,
  `refresh_token_issued_at` TIMESTAMP NULL,
  `refresh_token_expires_at` TIMESTAMP NULL,
  `refresh_token_metadata` TEXT,
  `oidc_id_token_value` LONGBLOB,
  `oidc_id_token_issued_at` TIMESTAMP NULL,
  `oidc_id_token_expires_at` TIMESTAMP NULL,
  `oidc_id_token_metadata` TEXT,
  `oidc_id_token_claims` TEXT,
  `user_code_value` LONGBLOB,
  `user_code_issued_at` TIMESTAMP NULL,
  `user_code_expires_at` TIMESTAMP NULL,
  `user_code_metadata` TEXT,
  `device_code_value` LONGBLOB,
  `device_code_issued_at` TIMESTAMP NULL,
  `device_code_expires_at` TIMESTAMP NULL,
  `device_code_metadata` TEXT,
  KEY `idx_registered_client_id` (`registered_client_id`),
  KEY `idx_principal_name` (`principal_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='OAuth2授权表';

-- OAuth2 授权同意表
CREATE TABLE IF NOT EXISTS `oauth2_authorization_consent` (
  `registered_client_id` VARCHAR(100) NOT NULL,
  `principal_name` VARCHAR(200) NOT NULL,
  `authorities` VARCHAR(1000),
  PRIMARY KEY (`registered_client_id`, `principal_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='OAuth2授权同意表';
