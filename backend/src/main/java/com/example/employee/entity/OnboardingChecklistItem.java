package com.example.employee.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("onboarding_checklist_item")
public class OnboardingChecklistItem {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long checklistId;

    private Long employeeId;

    private Long templateItemId;

    @NotBlank(message = "待办事项名称不能为空")
    private String itemName;

    private String itemDescription;

    @NotNull(message = "阶段不能为空")
    private Integer stage;

    private Integer sortOrder;

    private Boolean isTemporary;

    private Long responsibleUserId;

    private String responsibleUserName;

    @NotNull(message = "截止日期不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dueDate;

    private Integer status;

    private Long completedUserId;

    private String completedUserName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime completedTime;

    private String remark;

    private Boolean notificationSent;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
