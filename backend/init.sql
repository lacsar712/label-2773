SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS employee_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE employee_db;

CREATE TABLE IF NOT EXISTS department (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '部门名称',
    code VARCHAR(50) NOT NULL UNIQUE COMMENT '部门编码',
    description VARCHAR(500) DEFAULT NULL COMMENT '部门描述',
    leader VARCHAR(100) DEFAULT NULL COMMENT '负责人',
    parent_id BIGINT DEFAULT NULL COMMENT '上级部门ID',
    headcount_limit INT NOT NULL DEFAULT 10 COMMENT '编制人数上限',
    enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '启用状态：1-启用，0-停用',
    INDEX idx_parent_id (parent_id),
    INDEX idx_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门表';

CREATE TABLE IF NOT EXISTS department_notification (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    department_id BIGINT NOT NULL COMMENT '部门ID',
    department_name VARCHAR(100) NOT NULL COMMENT '部门名称',
    old_leader VARCHAR(100) DEFAULT NULL COMMENT '原负责人',
    new_leader VARCHAR(100) DEFAULT NULL COMMENT '新负责人',
    content VARCHAR(500) NOT NULL COMMENT '通知内容',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_department_id (department_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门变更通知表';

CREATE TABLE IF NOT EXISTS employee (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL,
    department_id BIGINT NOT NULL COMMENT '部门ID',
    role VARCHAR(100) NOT NULL,
    INDEX idx_department_id (department_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO department (name, code, description, leader, parent_id, headcount_limit, enabled) VALUES
('集团总部', 'HQ', '集团总部', '张总', NULL, 50, 1),
('技术中心', 'TECH', '技术研发中心', '李技术长', 1, 100, 1),
('产品中心', 'PROD', '产品设计中心', '王产品', 1, 50, 1),
('市场中心', 'MKT', '市场营销中心', '赵市场', 1, 30, 1),
('后端开发部', 'TECH-BE', '后端开发部门', '孙后端', 2, 30, 1),
('前端开发部', 'TECH-FE', '前端开发部门', '周前端', 2, 25, 1),
('测试部', 'TECH-QA', '质量测试部门', '吴测试', 2, 15, 1),
('产品设计部', 'PROD-PD', '产品设计部门', '郑产品', 3, 20, 1),
('UI设计部', 'PROD-UI', 'UI设计部门', '冯设计', 3, 15, 1),
('市场推广部', 'MKT-PROMO', '市场推广部门', '陈推广', 4, 10, 1),
('品牌部', 'MKT-BRAND', '品牌建设部门', '褚品牌', 4, 8, 1);

INSERT INTO employee (name, email, department_id, role) VALUES
('张三', 'zhangsan@example.com', 5, '后端开发工程师'),
('李四', 'lisi@example.com', 8, '产品经理'),
('王五', 'wangwu@example.com', 9, 'UI设计师'),
('赵六', 'zhaoliu@example.com', 6, '前端开发工程师'),
('钱七', 'qianqi@example.com', 7, '测试工程师'),
('孙八', 'sunba@example.com', 5, '后端开发工程师'),
('周九', 'zhoujiu@example.com', 10, '市场专员');
