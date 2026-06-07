package com.example.employee.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AttendanceMakeUpApprovalDTO {
    @NotNull(message = "补卡申请ID不能为空")
    private Long id;

    @NotNull(message = "审批状态不能为空")
    private Integer status;

    private String approvalRemark;
}
