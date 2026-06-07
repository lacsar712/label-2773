package com.example.employee.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;

@Data
public class AttendanceRecordQueryDTO {
    private Long employeeId;

    private Long departmentId;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    private Integer status;

    private Integer exceptionFlag;

    private Integer pageNum = 1;

    private Integer pageSize = 10;
}
