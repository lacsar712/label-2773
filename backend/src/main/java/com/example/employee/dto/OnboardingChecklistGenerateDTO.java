package com.example.employee.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class OnboardingChecklistGenerateDTO {
    @NotNull(message = "员工ID不能为空")
    private Long employeeId;

    private Long templateId;

    private Long mentorId;

    private String mentorName;

    @NotNull(message = "入职日期不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate hireDate;
}
