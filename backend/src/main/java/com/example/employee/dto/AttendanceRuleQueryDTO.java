package com.example.employee.dto;

import lombok.Data;

@Data
public class AttendanceRuleQueryDTO {
    private String ruleName;

    private Integer ruleType;

    private Integer enabled;

    private Integer isDefault;

    private Integer pageNum = 1;

    private Integer pageSize = 10;
}
