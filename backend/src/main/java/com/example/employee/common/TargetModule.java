package com.example.employee.common;

public enum TargetModule {

    EMPLOYEE("EMPLOYEE", "员工管理"),
    DEPARTMENT("DEPARTMENT", "部门管理"),
    SALARY("SALARY", "薪资管理"),
    ATTENDANCE("ATTENDANCE", "考勤管理"),
    LEAVE("LEAVE", "请假管理");

    private final String code;
    private final String desc;

    TargetModule(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
