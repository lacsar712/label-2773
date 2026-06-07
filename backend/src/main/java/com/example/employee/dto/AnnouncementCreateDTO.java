package com.example.employee.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AnnouncementCreateDTO {
    private Long id;

    @NotBlank(message = "公告标题不能为空")
    private String title;

    private String content;

    private String coverImage;

    @NotNull(message = "可见范围不能为空")
    private Integer visibilityType;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime effectiveTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expireTime;

    private Boolean isPinned;

    private Boolean isImportant;

    private List<VisibilityTargetDTO> visibilityTargets;

    @Data
    public static class VisibilityTargetDTO {
        private Integer targetType;
        private Long targetId;
        private String targetName;
    }
}
