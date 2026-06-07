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
    hire_date DATE DEFAULT NULL COMMENT '入职日期',
    leave_date DATE DEFAULT NULL COMMENT '离职日期',
    contract_end_date DATE DEFAULT NULL COMMENT '合同到期日期',
    status INT NOT NULL DEFAULT 1 COMMENT '状态：1-在职，2-试用期，3-待转正，4-已离职',
    INDEX idx_department_id (department_id),
    INDEX idx_status (status),
    INDEX idx_hire_date (hire_date),
    INDEX idx_leave_date (leave_date)
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

INSERT INTO employee (name, email, department_id, role, hire_date, leave_date, contract_end_date, status) VALUES
('张三', 'zhangsan@example.com', 5, '后端开发工程师', '2024-03-15', NULL, '2027-03-14', 1),
('李四', 'lisi@example.com', 8, '产品经理', '2024-06-01', NULL, '2027-05-31', 1),
('王五', 'wangwu@example.com', 9, 'UI设计师', '2025-04-10', NULL, '2028-04-09', 2),
('赵六', 'zhaoliu@example.com', 6, '前端开发工程师', '2023-11-20', NULL, '2026-11-19', 1),
('钱七', 'qianqi@example.com', 7, '测试工程师', '2024-09-01', NULL, '2027-08-31', 3),
('孙八', 'sunba@example.com', 5, '后端开发工程师', '2025-05-01', NULL, '2028-04-30', 2),
('周九', 'zhoujiu@example.com', 10, '市场专员', '2024-01-10', NULL, '2027-01-09', 1),
('吴十', 'wushi@example.com', 6, '前端开发工程师', '2022-06-15', '2025-03-31', '2025-06-14', 4),
('郑十一', 'zheng11@example.com', 8, '高级产品经理', '2023-02-01', NULL, '2026-07-31', 1),
('王十二', 'wang12@example.com', 5, '后端架构师', '2021-08-10', NULL, '2026-08-09', 1),
('刘十三', 'liu13@example.com', 7, '测试主管', '2023-05-20', NULL, '2026-05-19', 1),
('陈十四', 'chen14@example.com', 11, '品牌经理', '2024-07-01', NULL, '2027-06-30', 1),
('杨十五', 'yang15@example.com', 2, '技术总监', '2020-03-01', NULL, '2026-02-28', 1),
('黄十六', 'huang16@example.com', 5, '后端开发工程师', '2024-11-15', NULL, '2027-11-14', 3),
('林十七', 'lin17@example.com', 9, '高级UI设计师', '2023-04-10', '2025-05-15', '2026-04-09', 4),
('徐十八', 'xu18@example.com', 6, '前端开发工程师', '2025-06-01', NULL, '2028-05-31', 2),
('朱十九', 'zhu19@example.com', 10, '市场经理', '2024-02-01', NULL, '2027-01-31', 1),
('马二十', 'ma20@example.com', 8, '产品经理', '2025-03-15', NULL, '2028-03-14', 2),
('胡二一', 'hu21@example.com', 7, '测试工程师', '2024-08-01', NULL, '2027-07-31', 1),
('郭二二', 'guo22@example.com', 5, '后端开发工程师', '2023-01-10', '2024-12-31', '2026-01-09', 4);

-- 角色表
CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_code VARCHAR(50) NOT NULL UNIQUE COMMENT '角色编码：ADMIN-管理员，HR-人力资源，EMPLOYEE-普通员工',
    role_name VARCHAR(100) NOT NULL COMMENT '角色名称',
    description VARCHAR(500) DEFAULT NULL COMMENT '角色描述',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码（BCrypt加密）',
    nickname VARCHAR(100) DEFAULT NULL COMMENT '昵称',
    email VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    phone VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    avatar VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    status TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态：1-正常，0-禁用',
    is_first_login TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否首次登录：1-是，0-否',
    login_fail_count INT NOT NULL DEFAULT 0 COMMENT '连续登录失败次数',
    lock_time DATETIME DEFAULT NULL COMMENT '锁定时间',
    last_login_time DATETIME DEFAULT NULL COMMENT '最后登录时间',
    last_login_ip VARCHAR(50) DEFAULT NULL COMMENT '最后登录IP',
    session_token VARCHAR(255) DEFAULT NULL COMMENT '当前会话Token（用于异地登录强制下线）',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_username (username),
    INDEX idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 登录日志表
CREATE TABLE IF NOT EXISTS sys_login_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT DEFAULT NULL COMMENT '用户ID',
    username VARCHAR(50) DEFAULT NULL COMMENT '用户名',
    login_type TINYINT NOT NULL DEFAULT 1 COMMENT '登录类型：1-登录，2-登出',
    status TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态：1-成功，0-失败',
    ip_address VARCHAR(50) DEFAULT NULL COMMENT 'IP地址',
    login_location VARCHAR(200) DEFAULT NULL COMMENT '登录地点',
    browser VARCHAR(100) DEFAULT NULL COMMENT '浏览器',
    os VARCHAR(100) DEFAULT NULL COMMENT '操作系统',
    device VARCHAR(100) DEFAULT NULL COMMENT '设备信息',
    message VARCHAR(500) DEFAULT NULL COMMENT '提示消息',
    login_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
    INDEX idx_user_id (user_id),
    INDEX idx_login_time (login_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录日志表';

-- 图形验证码缓存表
CREATE TABLE IF NOT EXISTS sys_captcha (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid VARCHAR(100) NOT NULL UNIQUE COMMENT '验证码唯一标识',
    code VARCHAR(10) NOT NULL COMMENT '验证码',
    expire_time DATETIME NOT NULL COMMENT '过期时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_uuid (uuid),
    INDEX idx_expire_time (expire_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图形验证码表';

-- 插入角色数据
INSERT INTO sys_role (role_code, role_name, description) VALUES
('ADMIN', '系统管理员', '拥有系统所有权限'),
('HR', '人力资源', '管理员工和部门信息'),
('EMPLOYEE', '普通员工', '查看个人信息和数据概览');

-- 插入默认管理员账号（用户名: admin, 密码: admin123，使用BCrypt加密）
-- BCrypt加密后的 admin123: $2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2
INSERT INTO sys_user (username, password, nickname, email, role_id, status, is_first_login) VALUES
('admin', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '系统管理员', 'admin@example.com', 1, 1, 1);

-- 插入测试HR账号（用户名: hr, 密码: hr123456）
INSERT INTO sys_user (username, password, nickname, email, role_id, status, is_first_login) VALUES
('hr', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'HR管理员', 'hr@example.com', 2, 1, 1);

-- 插入测试普通员工账号（用户名: employee, 密码: emp123456）
INSERT INTO sys_user (username, password, nickname, email, role_id, status, is_first_login) VALUES
('employee', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '普通员工', 'employee@example.com', 3, 1, 1);

-- ==================== 考勤模块 ====================

-- 考勤规则表
CREATE TABLE IF NOT EXISTS attendance_rule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_name VARCHAR(100) NOT NULL COMMENT '规则名称',
    rule_type TINYINT NOT NULL DEFAULT 1 COMMENT '规则类型：1-标准工时，2-弹性工时',
    work_start_time TIME NOT NULL COMMENT '上班时间',
    work_end_time TIME NOT NULL COMMENT '下班时间',
    flex_start_time TIME DEFAULT NULL COMMENT '弹性上班开始时间（弹性工时有效）',
    flex_end_time TIME DEFAULT NULL COMMENT '弹性上班结束时间（弹性工时有效）',
    work_minutes INT NOT NULL DEFAULT 480 COMMENT '要求工作时长（分钟），弹性工时有效',
    late_grace_minutes INT NOT NULL DEFAULT 0 COMMENT '迟到宽限分钟数',
    early_grace_minutes INT NOT NULL DEFAULT 0 COMMENT '早退宽限分钟数',
    allowed_ip_ranges VARCHAR(1000) DEFAULT NULL COMMENT '允许打卡的IP范围（逗号分隔，如192.168.1.0/24,10.0.0.0/8）',
    allowed_gps_radius INT DEFAULT 500 COMMENT '允许打卡GPS半径（米）',
    work_location_name VARCHAR(200) DEFAULT NULL COMMENT '工作地点名称',
    work_location_lng DECIMAL(10, 7) DEFAULT NULL COMMENT '工作地点经度',
    work_location_lat DECIMAL(10, 7) DEFAULT NULL COMMENT '工作地点纬度',
    enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '启用状态',
    is_default TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否默认规则',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考勤规则表';

-- 员工考勤规则关联表
CREATE TABLE IF NOT EXISTS employee_attendance_rule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL COMMENT '员工ID',
    rule_id BIGINT NOT NULL COMMENT '考勤规则ID',
    effective_date DATE NOT NULL COMMENT '生效日期',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_employee_date (employee_id, effective_date),
    INDEX idx_rule_id (rule_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工考勤规则关联表';

-- 考勤打卡记录表
CREATE TABLE IF NOT EXISTS attendance_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL COMMENT '员工ID',
    attendance_date DATE NOT NULL COMMENT '考勤日期',
    check_in_time DATETIME DEFAULT NULL COMMENT '上班打卡时间',
    check_out_time DATETIME DEFAULT NULL COMMENT '下班打卡时间',
    check_in_ip VARCHAR(50) DEFAULT NULL COMMENT '上班打卡IP',
    check_out_ip VARCHAR(50) DEFAULT NULL COMMENT '下班打卡IP',
    check_in_location VARCHAR(200) DEFAULT NULL COMMENT '上班打卡地点',
    check_out_location VARCHAR(200) DEFAULT NULL COMMENT '下班打卡地点',
    check_in_lng DECIMAL(10, 7) DEFAULT NULL COMMENT '上班打卡经度',
    check_in_lat DECIMAL(10, 7) DEFAULT NULL COMMENT '上班打卡纬度',
    check_out_lng DECIMAL(10, 7) DEFAULT NULL COMMENT '下班打卡经度',
    check_out_lat DECIMAL(10, 7) DEFAULT NULL COMMENT '下班打卡纬度',
    check_in_type TINYINT DEFAULT 1 COMMENT '上班打卡来源：1-正常打卡，2-补卡',
    check_out_type TINYINT DEFAULT 1 COMMENT '下班打卡来源：1-正常打卡，2-补卡',
    makeup_in_id BIGINT DEFAULT NULL COMMENT '上班补卡申请ID',
    makeup_out_id BIGINT DEFAULT NULL COMMENT '下班补卡申请ID',
    work_minutes INT DEFAULT 0 COMMENT '实际工作时长（分钟）',
    late_minutes INT DEFAULT 0 COMMENT '迟到分钟数',
    early_minutes INT DEFAULT 0 COMMENT '早退分钟数',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-正常，1-迟到，2-早退，3-缺卡，4-迟到且早退，5-请假，6-出差',
    exception_flag TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否异常：1-是，0-否',
    rule_id BIGINT DEFAULT NULL COMMENT '应用的考勤规则ID',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_employee_date (employee_id, attendance_date),
    INDEX idx_attendance_date (attendance_date),
    INDEX idx_status (status),
    INDEX idx_exception (exception_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考勤打卡记录表';

-- 补卡申请表
CREATE TABLE IF NOT EXISTS attendance_makeup (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL COMMENT '申请人ID',
    employee_name VARCHAR(100) DEFAULT NULL COMMENT '申请人姓名',
    department_id BIGINT DEFAULT NULL COMMENT '部门ID',
    attendance_date DATE NOT NULL COMMENT '补卡日期',
    makeup_type TINYINT NOT NULL COMMENT '补卡类型：1-上班补卡，2-下班补卡',
    makeup_time DATETIME NOT NULL COMMENT '补卡时间',
    ip_address VARCHAR(50) DEFAULT NULL COMMENT '打卡IP',
    location VARCHAR(200) DEFAULT NULL COMMENT '打卡地点',
    lng DECIMAL(10, 7) DEFAULT NULL COMMENT '经度',
    lat DECIMAL(10, 7) DEFAULT NULL COMMENT '纬度',
    reason VARCHAR(500) NOT NULL COMMENT '补卡原因',
    approver_id BIGINT DEFAULT NULL COMMENT '审批人ID',
    approver_name VARCHAR(100) DEFAULT NULL COMMENT '审批人姓名',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-待审批，1-已通过，2-已拒绝',
    approval_time DATETIME DEFAULT NULL COMMENT '审批时间',
    approval_remark VARCHAR(500) DEFAULT NULL COMMENT '审批意见',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_employee_id (employee_id),
    INDEX idx_approver_id (approver_id),
    INDEX idx_status (status),
    INDEX idx_attendance_date (attendance_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='补卡申请表';

-- 考勤异常记录表
CREATE TABLE IF NOT EXISTS attendance_exception (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL COMMENT '员工ID',
    employee_name VARCHAR(100) DEFAULT NULL COMMENT '员工姓名',
    department_id BIGINT DEFAULT NULL COMMENT '部门ID',
    attendance_date DATE NOT NULL COMMENT '考勤日期',
    exception_type TINYINT NOT NULL COMMENT '异常类型：1-迟到，2-早退，3-缺卡，4-迟到且早退',
    late_minutes INT DEFAULT 0 COMMENT '迟到分钟数',
    early_minutes INT DEFAULT 0 COMMENT '早退分钟数',
    record_id BIGINT DEFAULT NULL COMMENT '关联打卡记录ID',
    salary_deduction DECIMAL(10, 2) DEFAULT 0 COMMENT '薪资扣款金额（预留）',
    deduction_rule_id BIGINT DEFAULT NULL COMMENT '扣款规则ID（预留）',
    handled TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已处理：1-是，0-否',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_employee_id (employee_id),
    INDEX idx_attendance_date (attendance_date),
    INDEX idx_exception_type (exception_type),
    INDEX idx_handled (handled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考勤异常记录表';

-- 月度考勤汇总表
CREATE TABLE IF NOT EXISTS attendance_monthly (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL COMMENT '员工ID',
    employee_name VARCHAR(100) DEFAULT NULL COMMENT '员工姓名',
    department_id BIGINT DEFAULT NULL COMMENT '部门ID',
    department_name VARCHAR(100) DEFAULT NULL COMMENT '部门名称',
    stat_year INT NOT NULL COMMENT '统计年份',
    stat_month INT NOT NULL COMMENT '统计月份',
    work_days INT NOT NULL DEFAULT 0 COMMENT '应出勤天数',
    actual_days INT NOT NULL DEFAULT 0 COMMENT '实际出勤天数',
    late_count INT NOT NULL DEFAULT 0 COMMENT '迟到次数',
    early_count INT NOT NULL DEFAULT 0 COMMENT '早退次数',
    absent_count INT NOT NULL DEFAULT 0 COMMENT '缺卡次数',
    exception_count INT NOT NULL DEFAULT 0 COMMENT '异常次数',
    total_work_minutes BIGINT NOT NULL DEFAULT 0 COMMENT '总工作时长（分钟）',
    total_late_minutes INT NOT NULL DEFAULT 0 COMMENT '总迟到分钟数',
    total_early_minutes INT NOT NULL DEFAULT 0 COMMENT '总早退分钟数',
    exception_rate DECIMAL(5, 2) NOT NULL DEFAULT 0 COMMENT '异常率(%)',
    salary_deduction DECIMAL(10, 2) DEFAULT 0 COMMENT '薪资扣款总额（预留）',
    leave_days INT NOT NULL DEFAULT 0 COMMENT '请假天数',
    business_trip_days INT NOT NULL DEFAULT 0 COMMENT '出差天数',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_employee_month (employee_id, stat_year, stat_month),
    INDEX idx_department (department_id),
    INDEX idx_year_month (stat_year, stat_month)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='月度考勤汇总表';

-- 初始化默认考勤规则
INSERT INTO attendance_rule (rule_name, rule_type, work_start_time, work_end_time, flex_start_time, flex_end_time, work_minutes, late_grace_minutes, early_grace_minutes, work_location_name, is_default, enabled) VALUES
('标准工时（9:00-18:00）', 1, '09:00:00', '18:00:00', NULL, NULL, 480, 5, 5, '公司总部', 1, 1),
('弹性工时（核心10:00-16:00）', 2, '10:00:00', '16:00:00', '07:00:00', '20:00:00', 480, 5, 5, '公司总部', 0, 1);

