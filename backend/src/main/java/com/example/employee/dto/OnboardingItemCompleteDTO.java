package com.example.employee.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OnboardingItemCompleteDTO {
    @NotNull(message = "待办项ID不能为空")
    private Long itemId;

    private String remark;
}
