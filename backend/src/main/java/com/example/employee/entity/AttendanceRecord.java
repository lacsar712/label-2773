package com.example.employee.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("attendance_record")
public class AttendanceRecord {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long employeeId;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate attendanceDate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime checkInTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime checkOutTime;

    private String checkInIp;

    private String checkOutIp;

    private String checkInLocation;

    private String checkOutLocation;

    private BigDecimal checkInLng;

    private BigDecimal checkInLat;

    private BigDecimal checkOutLng;

    private BigDecimal checkOutLat;

    private Integer checkInType;

    private Integer checkOutType;

    private Long makeupInId;

    private Long makeupOutId;

    private Integer workMinutes;

    private Integer lateMinutes;

    private Integer earlyMinutes;

    private Integer status;

    private Integer exceptionFlag;

    private Long ruleId;

    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private String employeeName;

    @TableField(exist = false)
    private String departmentName;

    @TableField(exist = false)
    private Long departmentId;
}
