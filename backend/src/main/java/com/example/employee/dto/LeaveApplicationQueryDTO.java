package com.example.employee.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class LeaveApplicationQueryDTO {
    private Long employeeId;
    private Long approverId;
    private Long departmentId;
    private Integer leaveType;
    private Integer status;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer pageNum = 1;
    private Integer pageSize = 10;
}
