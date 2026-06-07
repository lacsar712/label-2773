package com.example.employee.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("onboarding_template_item")
public class OnboardingTemplateItem {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long templateId;

    @NotBlank(message = "待办事项名称不能为空")
    private String itemName;

    private String itemDescription;

    @NotNull(message = "阶段不能为空")
    private Integer stage;

    private Integer sortOrder;

    @NotNull(message = "截止天数不能为空")
    private Integer dueDays;

    @NotBlank(message = "责任人角色不能为空")
    private String responsibleRole;

    private Long responsibleUserId;

    private String responsibleUserName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
