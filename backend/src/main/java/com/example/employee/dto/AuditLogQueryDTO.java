package com.example.employee.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AuditLogQueryDTO {

    private String targetModule;

    private Integer operationType;

    private Long operatorId;

    private String operatorName;

    private String targetRecordId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    private String keyword;

    private Integer pageNum = 1;

    private Integer pageSize = 20;
}
