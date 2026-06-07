package com.example.employee.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ClockInRequestDTO {
    @NotNull(message = "员工ID不能为空")
    private Long employeeId;

    @NotNull(message = "打卡类型不能为空")
    private Integer clockType;

    private String ipAddress;

    private String location;

    private BigDecimal lng;

    private BigDecimal lat;
}
