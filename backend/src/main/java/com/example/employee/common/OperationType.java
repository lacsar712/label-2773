package com.example.employee.common;

public enum OperationType {

    CREATE(1, "新增"),
    UPDATE(2, "修改"),
    DELETE(3, "删除");

    private final Integer code;
    private final String desc;

    OperationType(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
