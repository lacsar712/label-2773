package com.example.employee.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@TableName("attendance_rule")
public class AttendanceRule {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String ruleName;

    private Integer ruleType;

    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime workStartTime;

    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime workEndTime;

    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime flexStartTime;

    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime flexEndTime;

    private Integer workMinutes;

    private Integer lateGraceMinutes;

    private Integer earlyGraceMinutes;

    private String allowedIpRanges;

    private Integer allowedGpsRadius;

    private String workLocationName;

    private BigDecimal workLocationLng;

    private BigDecimal workLocationLat;

    private Integer enabled;

    private Integer isDefault;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
