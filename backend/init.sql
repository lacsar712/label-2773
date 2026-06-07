SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS employee_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE employee_db;

CREATE TABLE IF NOT EXISTS department (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '部门名称',
    code VARCHAR(50) NOT NULL UNIQUE COMMENT '部门编码',
    description VARCHAR(500) DEFAULT NULL COMMENT '部门描述',
    leader VARCHAR(100) DEFAULT NULL COMMENT '负责人',
    deputy_leader VARCHAR(100) DEFAULT NULL COMMENT '副负责人',
    parent_id BIGINT DEFAULT NULL COMMENT '上级部门ID',
    headcount_limit INT NOT NULL DEFAULT 10 COMMENT '编制人数上限',
    level_type TINYINT DEFAULT NULL COMMENT '层级类型：1-公司，2-事业部，3-小组，4-项目组',
    enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '启用状态：1-启用，0-停用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
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

INSERT INTO department (name, code, description, leader, deputy_leader, parent_id, headcount_limit, level_type, enabled) VALUES
('集团总部', 'HQ', '集团总部', '张总', '刘副总', NULL, 50, 1, 1),
('技术中心', 'TECH', '技术研发中心', '李技术长', '陈架构师', 1, 100, 2, 1),
('产品中心', 'PROD', '产品设计中心', '王产品', '李产品', 1, 50, 2, 1),
('市场中心', 'MKT', '市场营销中心', '赵市场', '孙市场', 1, 30, 2, 1),
('后端开发部', 'TECH-BE', '后端开发部门', '孙后端', '钱后端', 2, 30, 3, 1),
('前端开发部', 'TECH-FE', '前端开发部门', '周前端', '吴前端', 2, 25, 3, 1),
('测试部', 'TECH-QA', '质量测试部门', '吴测试', '郑测试', 2, 15, 3, 1),
('产品设计部', 'PROD-PD', '产品设计部门', '郑产品', '冯产品', 3, 20, 3, 1),
('UI设计部', 'PROD-UI', 'UI设计部门', '冯设计', '陈设计', 3, 15, 3, 1),
('市场推广部', 'MKT-PROMO', '市场推广部门', '陈推广', '褚推广', 4, 10, 3, 1),
('品牌部', 'MKT-BRAND', '品牌建设部门', '褚品牌', '卫品牌', 4, 8, 3, 1),
('电商项目组', 'TECH-BE-ECOM', '电商平台后端项目组', '蒋电商', '沈电商', 5, 8, 4, 1),
('OA项目组', 'TECH-BE-OA', 'OA系统后端项目组', '韩OA', '杨OA', 5, 6, 4, 1);

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

-- ==================== 请假模块 ====================

-- 节假日表
CREATE TABLE IF NOT EXISTS holiday (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    holiday_date DATE NOT NULL COMMENT '节假日日期',
    holiday_name VARCHAR(100) NOT NULL COMMENT '节假日名称',
    holiday_type TINYINT NOT NULL DEFAULT 1 COMMENT '类型：1-法定假日，2-周末调休补班',
    year INT NOT NULL COMMENT '年份',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_holiday_date (holiday_date),
    INDEX idx_year (year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='节假日表';

-- 假期余额表
CREATE TABLE IF NOT EXISTS leave_balance (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL COMMENT '员工ID',
    employee_name VARCHAR(100) DEFAULT NULL COMMENT '员工姓名',
    leave_type TINYINT NOT NULL COMMENT '假期类型：1-年假，2-事假，3-病假，4-调休',
    total_days DECIMAL(8,2) NOT NULL DEFAULT 0 COMMENT '总天数',
    used_days DECIMAL(8,2) NOT NULL DEFAULT 0 COMMENT '已用天数',
    remaining_days DECIMAL(8,2) NOT NULL DEFAULT 0 COMMENT '剩余天数',
    year INT NOT NULL COMMENT '年度',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_employee_type_year (employee_id, leave_type, year),
    INDEX idx_employee_id (employee_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='假期余额表';

-- 请假申请表
CREATE TABLE IF NOT EXISTS leave_application (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    application_no VARCHAR(50) NOT NULL UNIQUE COMMENT '申请单号',
    employee_id BIGINT NOT NULL COMMENT '申请人ID',
    employee_name VARCHAR(100) DEFAULT NULL COMMENT '申请人姓名',
    department_id BIGINT DEFAULT NULL COMMENT '部门ID',
    department_name VARCHAR(100) DEFAULT NULL COMMENT '部门名称',
    leave_type TINYINT NOT NULL COMMENT '假期类型：1-年假，2-事假，3-病假，4-调休',
    start_date DATE NOT NULL COMMENT '开始日期',
    end_date DATE NOT NULL COMMENT '结束日期',
    start_half TINYINT DEFAULT 0 COMMENT '开始日期半天标记：0-全天，1-上午，2-下午',
    end_half TINYINT DEFAULT 0 COMMENT '结束日期半天标记：0-全天，1-上午，2-下午',
    total_days DECIMAL(8,2) NOT NULL COMMENT '实际请假天数（扣减节假日）',
    reason VARCHAR(1000) NOT NULL COMMENT '请假事由',
    attachment VARCHAR(500) DEFAULT NULL COMMENT '附件路径',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-待提交，1-审批中，2-已通过，3-已驳回，4-已撤销',
    current_node_index INT DEFAULT 0 COMMENT '当前审批节点索引',
    current_approver_id BIGINT DEFAULT NULL COMMENT '当前审批人ID',
    current_approver_name VARCHAR(100) DEFAULT NULL COMMENT '当前审批人姓名',
    submit_time DATETIME DEFAULT NULL COMMENT '提交时间',
    approval_time DATETIME DEFAULT NULL COMMENT '最终审批时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_employee_id (employee_id),
    INDEX idx_status (status),
    INDEX idx_current_approver (current_approver_id),
    INDEX idx_department_id (department_id),
    INDEX idx_start_date (start_date),
    INDEX idx_end_date (end_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='请假申请表';

-- 请假审批节点表
CREATE TABLE IF NOT EXISTS leave_approval_node (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    application_id BIGINT NOT NULL COMMENT '请假申请ID',
    node_index INT NOT NULL COMMENT '节点顺序（从0开始）',
    node_name VARCHAR(100) NOT NULL COMMENT '节点名称',
    approver_id BIGINT NOT NULL COMMENT '审批人ID',
    approver_name VARCHAR(100) DEFAULT NULL COMMENT '审批人姓名',
    node_type TINYINT NOT NULL DEFAULT 1 COMMENT '节点类型：1-正常审批，2-加签',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '节点状态：0-待审批，1-已通过，2-已驳回，3-已转交，4-已跳过',
    approval_remark VARCHAR(1000) DEFAULT NULL COMMENT '审批意见',
    approval_time DATETIME DEFAULT NULL COMMENT '审批时间',
    original_approver_id BIGINT DEFAULT NULL COMMENT '转交前原审批人ID',
    original_approver_name VARCHAR(100) DEFAULT NULL COMMENT '转交前原审批人姓名',
    add_sign_approver_id BIGINT DEFAULT NULL COMMENT '加签人ID',
    add_sign_approver_name VARCHAR(100) DEFAULT NULL COMMENT '加签人姓名',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_application_id (application_id),
    INDEX idx_approver_id (approver_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='请假审批节点表';

-- 请假审批流程配置表
CREATE TABLE IF NOT EXISTS leave_approval_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    leave_type TINYINT NOT NULL COMMENT '假期类型：1-年假，2-事假，3-病假，4-调休，0-全部',
    min_days DECIMAL(8,2) DEFAULT 0 COMMENT '最小请假天数（含）',
    max_days DECIMAL(8,2) DEFAULT 9999 COMMENT '最大请假天数（含）',
    department_id BIGINT DEFAULT NULL COMMENT '部门ID（NULL表示全局配置）',
    node_index INT NOT NULL COMMENT '节点顺序（从0开始）',
    node_name VARCHAR(100) NOT NULL COMMENT '节点名称',
    approver_role VARCHAR(50) NOT NULL COMMENT '审批人角色：DIRECT_MANAGER-直属主管，DEPARTMENT_HEAD-部门负责人，HR-HR，SPECIFIC-指定人员',
    approver_id BIGINT DEFAULT NULL COMMENT '指定审批人ID（当approver_role=SPECIFIC时）',
    skip_condition VARCHAR(200) DEFAULT NULL COMMENT '跳过条件表达式（预留）',
    enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_leave_type (leave_type),
    INDEX idx_department (department_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='请假审批流程配置表';

-- 初始化请假审批流程配置（全局默认：直属主管 -> 部门负责人 -> HR）
INSERT INTO leave_approval_config (leave_type, min_days, max_days, department_id, node_index, node_name, approver_role, approver_id, skip_condition, enabled) VALUES
(0, 0, 9999, NULL, 0, '直属主管审批', 'DIRECT_MANAGER', NULL, NULL, 1),
(0, 0, 9999, NULL, 1, '部门负责人审批', 'DEPARTMENT_HEAD', NULL, NULL, 1),
(0, 0, 9999, NULL, 2, 'HR审批', 'HR', NULL, NULL, 1);

-- 初始化2026年部分节假日示例数据
INSERT INTO holiday (holiday_date, holiday_name, holiday_type, year) VALUES
('2026-01-01', '元旦', 1, 2026),
('2026-01-02', '元旦', 1, 2026),
('2026-01-03', '元旦', 1, 2026),
('2026-02-16', '春节', 1, 2026),
('2026-02-17', '春节', 1, 2026),
('2026-02-18', '春节', 1, 2026),
('2026-02-19', '春节', 1, 2026),
('2026-02-20', '春节', 1, 2026),
('2026-02-21', '春节', 1, 2026),
('2026-02-22', '春节', 1, 2026),
('2026-04-04', '清明节', 1, 2026),
('2026-04-05', '清明节', 1, 2026),
('2026-04-06', '清明节', 1, 2026),
('2026-05-01', '劳动节', 1, 2026),
('2026-05-02', '劳动节', 1, 2026),
('2026-05-03', '劳动节', 1, 2026),
('2026-05-04', '劳动节', 1, 2026),
('2026-05-05', '劳动节', 1, 2026),
('2026-06-19', '端午节', 1, 2026),
('2026-06-20', '端午节', 1, 2026),
('2026-06-21', '端午节', 1, 2026),
('2026-09-25', '中秋节', 1, 2026),
('2026-09-26', '中秋节', 1, 2026),
('2026-09-27', '中秋节', 1, 2026),
('2026-10-01', '国庆节', 1, 2026),
('2026-10-02', '国庆节', 1, 2026),
('2026-10-03', '国庆节', 1, 2026),
('2026-10-04', '国庆节', 1, 2026),
('2026-10-05', '国庆节', 1, 2026),
('2026-10-06', '国庆节', 1, 2026),
('2026-10-07', '国庆节', 1, 2026);

-- 初始化员工假期余额（为现有在职员工初始化2026年度年假）
INSERT INTO leave_balance (employee_id, employee_name, leave_type, total_days, used_days, remaining_days, year, remark)
SELECT e.id, e.name, 1, 
       CASE 
           WHEN DATEDIFF('2026-12-31', e.hire_date) >= 3650 THEN 15
           WHEN DATEDIFF('2026-12-31', e.hire_date) >= 3650 THEN 10
           WHEN DATEDIFF('2026-12-31', e.hire_date) >= 365 THEN 5
           ELSE 0
       END,
       0,
       CASE 
           WHEN DATEDIFF('2026-12-31', e.hire_date) >= 3650 THEN 15
           WHEN DATEDIFF('2026-12-31', e.hire_date) >= 365 THEN 10
           WHEN DATEDIFF('2026-12-31', e.hire_date) >= 365 THEN 5
           ELSE 0
       END,
       2026,
       '系统初始化'
FROM employee e WHERE e.status IN (1, 2, 3)
ON DUPLICATE KEY UPDATE remaining_days = VALUES(remaining_days);

-- ==================== 薪资模块 ====================

-- 薪资模板表
CREATE TABLE IF NOT EXISTS salary_template (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_name VARCHAR(100) NOT NULL COMMENT '模板名称',
    job_level VARCHAR(50) NOT NULL COMMENT '适用职级',
    base_salary DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '基本工资',
    post_allowance DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '岗位津贴',
    performance_coefficient DECIMAL(5,2) NOT NULL DEFAULT 1.00 COMMENT '绩效系数',
    performance_bonus DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '绩效奖金',
    social_insurance_personal_rate DECIMAL(5,4) NOT NULL DEFAULT 0.1050 COMMENT '社保个人比例(养老8%+医疗2%+失业0.5%)',
    social_insurance_company_rate DECIMAL(5,4) NOT NULL DEFAULT 0.2716 COMMENT '社保公司比例(养老16%+医疗9.5%+失业0.5%+工伤0.16%+生育1%)',
    housing_fund_personal_rate DECIMAL(5,4) NOT NULL DEFAULT 0.0700 COMMENT '公积金个人比例',
    housing_fund_company_rate DECIMAL(5,4) NOT NULL DEFAULT 0.0700 COMMENT '公积金公司比例',
    social_insurance_base DECIMAL(12,2) DEFAULT NULL COMMENT '社保公积金基数(NULL表示按基本工资)',
    description VARCHAR(500) DEFAULT NULL COMMENT '备注说明',
    enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_job_level (job_level),
    INDEX idx_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='薪资模板表';

-- 薪资记录表
CREATE TABLE IF NOT EXISTS salary_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    record_no VARCHAR(50) NOT NULL UNIQUE COMMENT '薪资单编号',
    employee_id BIGINT NOT NULL COMMENT '员工ID',
    employee_name VARCHAR(100) DEFAULT NULL COMMENT '员工姓名',
    department_id BIGINT DEFAULT NULL COMMENT '部门ID',
    department_name VARCHAR(100) DEFAULT NULL COMMENT '部门名称',
    job_level VARCHAR(50) DEFAULT NULL COMMENT '职级',
    template_id BIGINT DEFAULT NULL COMMENT '套用的薪资模板ID',
    salary_year INT NOT NULL COMMENT '薪资年份',
    salary_month INT NOT NULL COMMENT '薪资月份',
    base_salary DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '基本工资',
    post_allowance DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '岗位津贴',
    performance_coefficient DECIMAL(5,2) NOT NULL DEFAULT 1.00 COMMENT '绩效系数',
    performance_bonus DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '绩效奖金',
    overtime_pay DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '加班费',
    other_allowance DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '其他补贴',
    social_insurance_personal DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '社保个人部分',
    social_insurance_company DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '社保公司部分',
    housing_fund_personal DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '公积金个人部分',
    housing_fund_company DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '公积金公司部分',
    income_tax DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '个人所得税',
    other_deduction DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '其他扣款',
    gross_salary DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '应发合计(税前)',
    total_deduction DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '扣款合计(个人部分+税)',
    net_salary DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '实发金额',
    total_company_cost DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '公司总成本(应发+公司社保+公司公积金)',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-草稿，1-已确认，2-已发放',
    issue_time DATETIME DEFAULT NULL COMMENT '发放时间',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_employee_month (employee_id, salary_year, salary_month),
    INDEX idx_department (department_id),
    INDEX idx_year_month (salary_year, salary_month),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='薪资记录表';

-- 薪资调整留痕表
CREATE TABLE IF NOT EXISTS salary_adjust_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    salary_record_id BIGINT NOT NULL COMMENT '薪资记录ID',
    employee_id BIGINT NOT NULL COMMENT '员工ID',
    employee_name VARCHAR(100) DEFAULT NULL COMMENT '员工姓名',
    field_name VARCHAR(50) NOT NULL COMMENT '调整字段名',
    field_label VARCHAR(100) NOT NULL COMMENT '调整字段标签',
    old_value DECIMAL(12,2) DEFAULT NULL COMMENT '调整前值',
    new_value DECIMAL(12,2) DEFAULT NULL COMMENT '调整后值',
    adjust_reason VARCHAR(500) DEFAULT NULL COMMENT '调整原因',
    operator_id BIGINT DEFAULT NULL COMMENT '操作人ID',
    operator_name VARCHAR(100) DEFAULT NULL COMMENT '操作人姓名',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    INDEX idx_salary_record (salary_record_id),
    INDEX idx_employee (employee_id),
    INDEX idx_operator (operator_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='薪资调整留痕表';

-- 初始化薪资模板示例数据
INSERT INTO salary_template (template_name, job_level, base_salary, post_allowance, performance_coefficient, performance_bonus, description, enabled) VALUES
('初级工程师模板', 'P4', 12000.00, 1500.00, 1.00, 2000.00, '适用于初级工程师职级', 1),
('中级工程师模板', 'P5', 18000.00, 2500.00, 1.00, 4000.00, '适用于中级工程师职级', 1),
('高级工程师模板', 'P6', 28000.00, 4000.00, 1.00, 8000.00, '适用于高级工程师职级', 1),
('专家模板', 'P7', 40000.00, 6000.00, 1.00, 15000.00, '适用于专家/架构师职级', 1),
('产品经理初级模板', 'P4', 13000.00, 1500.00, 1.00, 2500.00, '适用于初级产品经理', 1),
('产品经理高级模板', 'P6', 30000.00, 4500.00, 1.00, 9000.00, '适用于高级产品经理', 1),
('设计师模板', 'P5', 16000.00, 2000.00, 1.00, 3500.00, '适用于设计师职级', 1),
('市场专员模板', 'P4', 10000.00, 1200.00, 1.00, 1800.00, '适用于市场专员职级', 1),
('管理层模板', 'M2', 50000.00, 10000.00, 1.00, 20000.00, '适用于部门总监级', 1);

-- ==================== 操作审计日志模块 ====================

CREATE TABLE IF NOT EXISTS sys_audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    operator_id BIGINT DEFAULT NULL COMMENT '操作人ID',
    operator_name VARCHAR(100) DEFAULT NULL COMMENT '操作人姓名',
    operator_username VARCHAR(50) DEFAULT NULL COMMENT '操作人用户名',
    operator_role_id BIGINT DEFAULT NULL COMMENT '操作人角色ID',
    operator_role_code VARCHAR(50) DEFAULT NULL COMMENT '操作人角色编码',
    operator_role_name VARCHAR(100) DEFAULT NULL COMMENT '操作人角色名称',
    operation_type TINYINT NOT NULL COMMENT '操作类型：1-新增，2-修改，3-删除',
    target_module VARCHAR(50) NOT NULL COMMENT '目标模块：EMPLOYEE-员工，DEPARTMENT-部门，SALARY-薪资，ATTENDANCE-考勤，LEAVE-请假',
    target_module_name VARCHAR(100) NOT NULL COMMENT '目标模块名称',
    target_record_id VARCHAR(100) DEFAULT NULL COMMENT '目标记录ID',
    target_record_name VARCHAR(200) DEFAULT NULL COMMENT '目标记录名称/描述',
    before_snapshot JSON DEFAULT NULL COMMENT '变更前JSON快照',
    after_snapshot JSON DEFAULT NULL COMMENT '变更后JSON快照',
    request_ip VARCHAR(50) DEFAULT NULL COMMENT '请求IP',
    user_agent VARCHAR(500) DEFAULT NULL COMMENT '请求User-Agent',
    browser VARCHAR(100) DEFAULT NULL COMMENT '浏览器',
    os VARCHAR(100) DEFAULT NULL COMMENT '操作系统',
    device VARCHAR(100) DEFAULT NULL COMMENT '设备信息',
    operation_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    archived TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已归档：0-否，1-是',
    INDEX idx_operator (operator_id),
    INDEX idx_operation_type (operation_type),
    INDEX idx_target_module (target_module),
    INDEX idx_target_record (target_record_id),
    INDEX idx_operation_time (operation_time),
    INDEX idx_archived (archived)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作审计日志表';

-- ==================== 组织架构扩展模块 ====================

-- 负责人变更历史表
CREATE TABLE IF NOT EXISTS department_leader_change_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    department_id BIGINT NOT NULL COMMENT '部门ID',
    department_name VARCHAR(100) NOT NULL COMMENT '部门名称',
    change_type TINYINT NOT NULL COMMENT '变更类型：1-负责人变更，2-副负责人变更',
    old_leader VARCHAR(100) DEFAULT NULL COMMENT '原负责人',
    new_leader VARCHAR(100) DEFAULT NULL COMMENT '新负责人',
    operator_id BIGINT DEFAULT NULL COMMENT '操作人ID',
    operator_name VARCHAR(100) DEFAULT NULL COMMENT '操作人姓名',
    remark VARCHAR(500) DEFAULT NULL COMMENT '变更原因/备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_department_id (department_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='负责人变更历史表';

-- 组织架构版本快照表
CREATE TABLE IF NOT EXISTS department_version_snapshot (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    snapshot_name VARCHAR(200) NOT NULL COMMENT '快照名称',
    snapshot_type TINYINT NOT NULL DEFAULT 1 COMMENT '快照类型：1-手动创建，2-变更自动创建',
    tree_snapshot JSON NOT NULL COMMENT '部门树JSON快照',
    description VARCHAR(500) DEFAULT NULL COMMENT '快照描述',
    operator_id BIGINT DEFAULT NULL COMMENT '操作人ID',
    operator_name VARCHAR(100) DEFAULT NULL COMMENT '操作人姓名',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_create_time (create_time),
    INDEX idx_snapshot_type (snapshot_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='组织架构版本快照表';

-- 用户树展开状态记忆表
CREATE TABLE IF NOT EXISTS user_tree_state (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    tree_key VARCHAR(50) NOT NULL DEFAULT 'department' COMMENT '树标识',
    expanded_ids JSON NOT NULL COMMENT '展开的节点ID列表',
    selected_id BIGINT DEFAULT NULL COMMENT '选中的节点ID',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_tree (user_id, tree_key),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户树状态记忆表';

-- ==================== 入职清单模块 ====================

-- 入职清单模板表
CREATE TABLE IF NOT EXISTS onboarding_template (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_name VARCHAR(200) NOT NULL COMMENT '模板名称',
    department_id BIGINT DEFAULT NULL COMMENT '适用部门ID（NULL表示通用）',
    department_name VARCHAR(100) DEFAULT NULL COMMENT '适用部门名称',
    position VARCHAR(100) DEFAULT NULL COMMENT '适用岗位（NULL表示通用）',
    description VARCHAR(500) DEFAULT NULL COMMENT '模板描述',
    enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    is_default TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否默认模板',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_department (department_id),
    INDEX idx_position (position),
    INDEX idx_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入职清单模板表';

-- 入职清单模板待办项表
CREATE TABLE IF NOT EXISTS onboarding_template_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_id BIGINT NOT NULL COMMENT '模板ID',
    item_name VARCHAR(200) NOT NULL COMMENT '待办事项名称',
    item_description VARCHAR(500) DEFAULT NULL COMMENT '事项描述',
    stage TINYINT NOT NULL COMMENT '阶段：1-入职前，2-首日，3-首周，4-首月',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序序号',
    due_days INT NOT NULL DEFAULT 0 COMMENT '相对入职日期的截止天数（0=入职当天，负数=入职前）',
    responsible_role VARCHAR(50) NOT NULL COMMENT '责任人角色：NEW_EMPLOYEE-新员工，MENTOR-导师，HR-HR，DEPARTMENT_HEAD-部门负责人，SPECIFIC-指定人员',
    responsible_user_id BIGINT DEFAULT NULL COMMENT '指定责任人ID（当responsible_role=SPECIFIC时）',
    responsible_user_name VARCHAR(100) DEFAULT NULL COMMENT '指定责任人姓名',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_template_id (template_id),
    INDEX idx_stage (stage)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入职清单模板待办项表';

-- 员工入职清单表
CREATE TABLE IF NOT EXISTS onboarding_checklist (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL COMMENT '员工ID',
    employee_name VARCHAR(100) NOT NULL COMMENT '员工姓名',
    department_id BIGINT DEFAULT NULL COMMENT '部门ID',
    department_name VARCHAR(100) DEFAULT NULL COMMENT '部门名称',
    position VARCHAR(100) DEFAULT NULL COMMENT '岗位',
    hire_date DATE NOT NULL COMMENT '入职日期',
    template_id BIGINT DEFAULT NULL COMMENT '使用的模板ID',
    template_name VARCHAR(200) DEFAULT NULL COMMENT '使用的模板名称',
    mentor_id BIGINT DEFAULT NULL COMMENT '导师ID',
    mentor_name VARCHAR(100) DEFAULT NULL COMMENT '导师姓名',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-进行中，1-已完成，2-已归档',
    progress INT NOT NULL DEFAULT 0 COMMENT '整体进度百分比(0-100)',
    pre_join_progress INT NOT NULL DEFAULT 0 COMMENT '入职前进度(%)',
    first_day_progress INT NOT NULL DEFAULT 0 COMMENT '首日进度(%)',
    first_week_progress INT NOT NULL DEFAULT 0 COMMENT '首周进度(%)',
    first_month_progress INT NOT NULL DEFAULT 0 COMMENT '首月进度(%)',
    archived_time DATETIME DEFAULT NULL COMMENT '归档时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_employee (employee_id),
    INDEX idx_status (status),
    INDEX idx_department (department_id),
    INDEX idx_hire_date (hire_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工入职清单表';

-- 员工入职清单待办项表
CREATE TABLE IF NOT EXISTS onboarding_checklist_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    checklist_id BIGINT NOT NULL COMMENT '入职清单ID',
    employee_id BIGINT NOT NULL COMMENT '员工ID',
    template_item_id BIGINT DEFAULT NULL COMMENT '来源模板项ID（临时待办为NULL）',
    item_name VARCHAR(200) NOT NULL COMMENT '待办事项名称',
    item_description VARCHAR(500) DEFAULT NULL COMMENT '事项描述',
    stage TINYINT NOT NULL COMMENT '阶段：1-入职前，2-首日，3-首周，4-首月',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序序号',
    is_temporary TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否临时追加：0-否，1-是',
    responsible_user_id BIGINT DEFAULT NULL COMMENT '责任人ID',
    responsible_user_name VARCHAR(100) DEFAULT NULL COMMENT '责任人姓名',
    due_date DATE NOT NULL COMMENT '截止日期',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-未开始，1-进行中，2-已完成，3-已逾期',
    completed_user_id BIGINT DEFAULT NULL COMMENT '完成人ID',
    completed_user_name VARCHAR(100) DEFAULT NULL COMMENT '完成人姓名',
    completed_time DATETIME DEFAULT NULL COMMENT '完成时间',
    remark VARCHAR(1000) DEFAULT NULL COMMENT '备注',
    notification_sent TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逾期通知是否已发送：0-否，1-是',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_checklist_id (checklist_id),
    INDEX idx_employee_id (employee_id),
    INDEX idx_status (status),
    INDEX idx_due_date (due_date),
    INDEX idx_responsible (responsible_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工入职清单待办项表';

-- 入职消息通知表
CREATE TABLE IF NOT EXISTS onboarding_notification (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    checklist_id BIGINT DEFAULT NULL COMMENT '入职清单ID',
    checklist_item_id BIGINT DEFAULT NULL COMMENT '待办项ID',
    recipient_id BIGINT NOT NULL COMMENT '接收人ID',
    recipient_name VARCHAR(100) NOT NULL COMMENT '接收人姓名',
    notification_type TINYINT NOT NULL COMMENT '通知类型：1-待办分配，2-即将逾期，3-已逾期，4-完成提醒，5-清单完成',
    title VARCHAR(200) NOT NULL COMMENT '通知标题',
    content VARCHAR(1000) NOT NULL COMMENT '通知内容',
    is_read TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已读：0-否，1-是',
    read_time DATETIME DEFAULT NULL COMMENT '阅读时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_recipient (recipient_id),
    INDEX idx_is_read (is_read),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入职消息通知表';

-- 初始化入职清单模板示例数据
INSERT INTO onboarding_template (template_name, department_id, department_name, position, description, enabled, is_default) VALUES
('通用入职模板', NULL, NULL, NULL, '适用于所有岗位的通用入职流程', 1, 1),
('技术研发入职模板', 2, '技术中心', NULL, '技术中心所有研发岗位通用入职模板', 1, 0),
('产品设计入职模板', 3, '产品中心', NULL, '产品中心所有岗位通用入职模板', 1, 0),
('后端开发专项模板', 5, '后端开发部', '后端开发工程师', '后端开发工程师专项入职流程', 1, 0);

-- 初始化通用入职模板待办项
INSERT INTO onboarding_template_item (template_id, item_name, item_description, stage, sort_order, due_days, responsible_role, responsible_user_id, responsible_user_name) VALUES
(1, '发送入职Offer及资料包', '向新员工发送正式Offer、入职须知、需准备材料清单', 1, 1, -7, 'HR', NULL, NULL),
(1, '确认入职材料齐全', '确认身份证、学历证明、离职证明等材料是否准备齐全', 1, 2, -3, 'HR', NULL, NULL),
(1, '开通公司邮箱及系统账号', '为新员工开通企业邮箱、OA系统、VPN等基础账号', 1, 3, -1, 'HR', NULL, NULL),
(1, '准备工位及办公设备', '安排工位，准备电脑、显示器、键盘鼠标等办公设备', 1, 4, -1, 'DEPARTMENT_HEAD', NULL, NULL),
(1, '签署劳动合同及保密协议', '完成劳动合同、保密协议、竞业协议等文件签署', 2, 1, 0, 'HR', NULL, NULL),
(1, '领取办公设备', '领取电脑、工牌、门禁卡等办公设备并登记', 2, 2, 0, 'HR', NULL, NULL),
(1, '公司介绍与文化培训', '公司发展历程、组织架构、企业文化、规章制度培训', 2, 3, 0, 'HR', NULL, NULL),
(1, '部门及团队介绍', '部门负责人介绍部门职责、团队成员及工作流程', 2, 4, 0, 'DEPARTMENT_HEAD', NULL, NULL),
(1, '导师见面会', '与分配的导师进行一对一沟通，明确导师职责', 3, 1, 3, 'MENTOR', NULL, NULL),
(1, '业务知识系统培训', '产品介绍、业务流程、技术架构等系统性培训', 3, 2, 5, 'MENTOR', NULL, NULL),
(1, '开发环境搭建与权限申请', '申请代码库、开发环境、测试环境等相关权限', 3, 3, 5, 'MENTOR', NULL, NULL),
(1, '参与第一个小任务', '在导师指导下完成第一个简单的工作任务', 3, 4, 7, 'MENTOR', NULL, NULL),
(1, '试用期目标制定', '与导师和主管共同制定试用期工作目标和考核标准', 4, 1, 14, 'DEPARTMENT_HEAD', NULL, NULL),
(1, '独立承担模块工作', '开始独立承担一个模块的开发/设计工作', 4, 2, 21, 'MENTOR', NULL, NULL),
(1, '首月工作总结与沟通', '与HR、导师进行首月工作回顾和沟通', 4, 3, 30, 'HR', NULL, NULL);

-- 初始化技术研发入职模板待办项（在通用基础上增加专项）
INSERT INTO onboarding_template_item (template_id, item_name, item_description, stage, sort_order, due_days, responsible_role, responsible_user_id, responsible_user_name) VALUES
(2, '代码规范与Git流程培训', '公司代码规范、分支策略、Code Review流程培训', 3, 5, 5, 'MENTOR', NULL, NULL),
(2, '技术架构与中间件培训', '公司整体技术架构、中间件使用规范培训', 3, 6, 7, 'MENTOR', NULL, NULL),
(2, '安全规范与数据权限培训', '代码安全、数据安全、权限管理规范培训', 4, 4, 15, 'MENTOR', NULL, NULL);

