package com.example.employee.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LeaveApprovalDTO {
    @NotNull(message = "申请ID不能为空")
    private Long applicationId;

    @NotNull(message = "审批结果不能为空")
    private Integer status;

    private String approvalRemark;

    private Long transferToApproverId;

    private String transferToApproverName;

    private Long addSignApproverId;

    private String addSignApproverName;

    private Long approverId;

    private String approverRoleCode;
}
