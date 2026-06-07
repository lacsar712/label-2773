package com.example.employee.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("onboarding_notification")
public class OnboardingNotification {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long checklistId;

    private Long checklistItemId;

    private Long recipientId;

    private String recipientName;

    private Integer notificationType;

    private String title;

    private String content;

    private Boolean isRead;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime readTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
