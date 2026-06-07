package com.example.employee.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("leave_balance")
public class LeaveBalance {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long employeeId;

    private String employeeName;

    private Integer leaveType;

    private BigDecimal totalDays;

    private BigDecimal usedDays;

    private BigDecimal remainingDays;

    private Integer year;

    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
