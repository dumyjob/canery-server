-- 项目主表
CREATE TABLE `projects` (
  `id`  BIGINT(20) NOT NULL COMMENT '主键ID（注意：GenerationType.IDENTITY需配合数值类型主键，当前设计建议改为UUID或数值类型）',
  `name` VARCHAR(255) NOT NULL COMMENT '项目名称(zh/en)',
  `description` TEXT COMMENT '项目描述',
  `git_repos` VARCHAR(500) COMMENT 'Git仓库地址',
  `web_hook` VARCHAR(255) COMMENT 'WebHook地址',
  `project_type` VARCHAR(50) NOT NULL COMMENT '项目类型(boot-jar/dubbo)',
  `cloud_config` JSON COMMENT '云资源模板（ROS/Terraform）',
  `cpu` INT DEFAULT 1 COMMENT 'CPU核数',
  `memory` INT DEFAULT 512 COMMENT '内存大小(MB)',
  `pods` INT DEFAULT 1 COMMENT 'Pod数量',
  `jvm_args` TEXT COMMENT 'JVM参数',

  `create_dt` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by` VARCHAR(255) NOT NULL COMMENT '创建人',
  `update_dt` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `update_by` VARCHAR(255) NOT NULL COMMENT '修改人',
  `is_delete` TINYINT(1) DEFAULT 0 COMMENT '逻辑删除标志',
  PRIMARY KEY (`id`),
  INDEX `idx_project_type` (`project_type`),
  INDEX `idx_create_dt` (`create_dt`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- 环境变量关联表（@ElementCollection映射）
CREATE TABLE `project_env_vars` (
  `project_id` VARCHAR(255) NOT NULL,
  `var_key` VARCHAR(255) NOT NULL COMMENT '变量名',
  `var_value` TEXT COMMENT '变量值',
  PRIMARY KEY (`project_id`, `var_key`),
  CONSTRAINT `fk_env_project` FOREIGN KEY (`project_id`) REFERENCES `projects` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;


-- 发布单
CREATE TABLE release_orders (
  id BIGINT AUTO_INCREMENT COMMENT '发布单ID',
  release_name VARCHAR(255) NOT NULL COMMENT '发布单名称',
  release_type ENUM('canary','rolling','normal','gray') NOT NULL COMMENT '发布类型(金丝雀|滚动|常规|灰度)',
  traffic_rule ENUM('canary','rolling','normal','gray')
  status ENUM('draft','in_progress','completed','rollback') DEFAULT 'draft',
  gray_version varchar(50) COMMENT '灰度版本',
   `create_dt` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by` VARCHAR(255) NOT NULL COMMENT '创建人',
    `update_dt` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `update_by` VARCHAR(255) NOT NULL COMMENT '修改人',
    `is_delete` TINYINT(1) DEFAULT 0 COMMENT '逻辑删除标志',
  PRIMARY KEY (id),
  UNIQUE KEY uniq_order_name (release_name),
  INDEX idx_git_branch (git_branch)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- 发布单关联项目
CREATE TABLE release_projects (
  release_id BIGINT NOT NULL COMMENT '发布单ID',
  project_id BIGINT NOT NULL COMMENT '项目ID',
  deploy_version VARCHAR(50) NOT NULL COMMENT '待发布版本号',
  deploy_branch VARCHAR(100) NOT NULL COMMENT '部署分支（如feature/login-api）[7](@ref)',
  rollback_version VARCHAR(50) COMMENT '回滚版本号',
  rollback_branch VARCHAR(100) COMMENT '回滚分支',
  PRIMARY KEY (release_id, project_id),
  FOREIGN KEY (release_id) REFERENCES release_orders(order_id) ON DELETE CASCADE,
  INDEX idx_deploy_branch (deploy_branch)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- 流量策略表（traffic_strategies)
CREATE TABLE traffic_strategies (
  strategy_id BIGINT AUTO_INCREMENT COMMENT '策略ID',
  release_order_id BIGINT NOT NULL COMMENT '关联发布单ID',
  strategy_type ENUM('allow','deny','limit') NOT NULL COMMENT '策略类型',
  match_condition JSON COMMENT '匹配规则（如{"ip_range":"192.168.1.0/24"}）',
  qps_limit INT DEFAULT 1000 COMMENT 'QPS限制',
  priority TINYINT NOT NULL DEFAULT 5 COMMENT '优先级（1-10）',
  PRIMARY KEY (strategy_id),
  FOREIGN KEY (release_order_id) REFERENCES release_orders(order_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流量策略表';

-- 灰度策略表（gray_strategies）
CREATE TABLE gray_strategies (
  strategy_id BIGINT AUTO_INCREMENT COMMENT '策略ID',
  strategy_type ENUM('percentage','user_group','header_rule') NOT NULL COMMENT '策略类型',
  strategy_config JSON NOT NULL COMMENT '策略配置（如{"percentage":10,"user_ids":[1001,1002]}）',
  description TEXT COMMENT '策略描述',
  PRIMARY KEY (strategy_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='灰度策略表';


-- 发布部署任务
CREATE TABLE deploy_tasks (
    id BIGINT AUTO_INCREMENT COMMENT '部署id',
    project_id BIGINT NOT NULL COMMENT '项目ID',
    status VARCHAR(20) NOT NULL,
    git_repo VARCHAR(255),
    branch VARCHAR(50),
    commit_id varchar(255) COMMENT '部署任务branch上的commit-id'
    logs TEXT,
    start_time TIMESTAMP,
    end_time TIMESTAMP,
     PRIMARY KEY (id)
)ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部署任务表';;