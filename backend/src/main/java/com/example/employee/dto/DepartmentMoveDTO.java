package com.example.employee.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DepartmentMoveDTO {
    @NotNull(message = "移动的部门ID不能为空")
    private Long deptId;

    private Long targetParentId;

    private Integer targetSort;
}
