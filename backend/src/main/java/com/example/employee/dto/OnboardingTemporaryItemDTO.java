package com.example.employee.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class OnboardingTemporaryItemDTO {
    @NotNull(message = "入职清单ID不能为空")
    private Long checklistId;

    @NotBlank(message = "待办事项名称不能为空")
    private String itemName;

    private String itemDescription;

    @NotNull(message = "阶段不能为空")
    private Integer stage;

    private Integer sortOrder;

    private Long responsibleUserId;

    private String responsibleUserName;

    @NotNull(message = "截止日期不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dueDate;
}
