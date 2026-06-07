package com.example.employee.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("onboarding_checklist")
public class OnboardingChecklist {
    @TableId(type = IdType.AUTO)
    private Long id;

    @NotNull(message = "员工ID不能为空")
    private Long employeeId;

    private String employeeName;

    private Long departmentId;

    private String departmentName;

    private String position;

    @NotNull(message = "入职日期不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate hireDate;

    private Long templateId;

    private String templateName;

    private Long mentorId;

    private String mentorName;

    private Integer status;

    private Integer progress;

    private Integer preJoinProgress;

    private Integer firstDayProgress;

    private Integer firstWeekProgress;

    private Integer firstMonthProgress;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime archivedTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private List<OnboardingChecklistItem> items;
}
