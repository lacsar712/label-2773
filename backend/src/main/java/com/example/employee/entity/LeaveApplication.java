package com.example.employee.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("leave_application")
public class LeaveApplication {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String applicationNo;

    @NotNull(message = "申请人不能为空")
    private Long employeeId;

    private String employeeName;

    private Long departmentId;

    private String departmentName;

    @NotNull(message = "假期类型不能为空")
    private Integer leaveType;

    @NotNull(message = "开始日期不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @NotNull(message = "结束日期不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    private Integer startHalf;

    private Integer endHalf;

    private BigDecimal totalDays;

    @NotBlank(message = "请假事由不能为空")
    private String reason;

    private String attachment;

    private Integer status;

    private Integer currentNodeIndex;

    private Long currentApproverId;

    private String currentApproverName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime submitTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime approvalTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private List<LeaveApprovalNode> approvalNodes;

    @TableField(exist = false)
    private String leaveTypeName;

    @TableField(exist = false)
    private String statusName;
}
