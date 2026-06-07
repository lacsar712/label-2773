package com.example.employee.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class LeaveDaysCalculateDTO {
    @NotNull(message = "员工ID不能为空")
    private Long employeeId;

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

    private BigDecimal workDays;
}
