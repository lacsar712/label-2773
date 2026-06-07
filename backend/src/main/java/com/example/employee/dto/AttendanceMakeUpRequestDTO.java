package com.example.employee.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class AttendanceMakeUpRequestDTO {
    @NotNull(message = "员工ID不能为空")
    private Long employeeId;

    @NotNull(message = "考勤日期不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate attendanceDate;

    @NotNull(message = "补卡类型不能为空")
    private Integer makeupType;

    @NotNull(message = "补卡时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime makeupTime;

    @NotBlank(message = "补卡原因不能为空")
    private String reason;

    private String location;

    private String ipAddress;

    private BigDecimal lng;

    private BigDecimal lat;
}
