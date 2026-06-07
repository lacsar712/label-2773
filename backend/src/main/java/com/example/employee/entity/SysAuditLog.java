package com.example.employee.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_audit_log")
public class SysAuditLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long operatorId;

    private String operatorName;

    private String operatorUsername;

    private Long operatorRoleId;

    private String operatorRoleCode;

    private String operatorRoleName;

    private Integer operationType;

    private String targetModule;

    private String targetModuleName;

    private String targetRecordId;

    private String targetRecordName;

    private String beforeSnapshot;

    private String afterSnapshot;

    private String requestIp;

    private String userAgent;

    private String browser;

    private String os;

    private String device;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime operationTime;

    private Boolean archived;
}
