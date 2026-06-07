package com.example.employee.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("leave_approval_config")
public class LeaveApprovalConfig {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Integer leaveType;

    private BigDecimal minDays;

    private BigDecimal maxDays;

    private Long departmentId;

    private Integer nodeIndex;

    private String nodeName;

    private String approverRole;

    private Long approverId;

    private String skipCondition;

    private Integer enabled;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
