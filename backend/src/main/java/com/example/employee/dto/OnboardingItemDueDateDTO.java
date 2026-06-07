package com.example.employee.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class OnboardingItemDueDateDTO {
    @NotNull(message = "待办项ID不能为空")
    private Long itemId;

    @NotNull(message = "新截止日期不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dueDate;
}
