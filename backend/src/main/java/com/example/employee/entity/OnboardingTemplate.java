package com.example.employee.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("onboarding_template")
public class OnboardingTemplate {
    @TableId(type = IdType.AUTO)
    private Long id;

    @NotBlank(message = "模板名称不能为空")
    private String templateName;

    private Long departmentId;

    private String departmentName;

    private String position;

    private String description;

    private Boolean enabled;

    private Boolean isDefault;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private List<OnboardingTemplateItem> items;
}
