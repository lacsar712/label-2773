package com.example.employee.dto;

import com.example.employee.entity.AnnouncementRead;
import lombok.Data;

import java.util.List;

@Data
public class AnnouncementDetailDTO {
    private Long id;
    private String title;
    private String content;
    private String coverImage;
    private Integer visibilityType;
    private String effectiveTime;
    private String expireTime;
    private Boolean isPinned;
    private Boolean isImportant;
    private Integer status;
    private String publishTime;
    private Long creatorId;
    private String creatorName;
    private Integer readCount;
    private Integer totalTargetCount;
    private String createTime;
    private Boolean hasRead;
    private List<AnnouncementRead> readList;
    private List<AnnouncementRead> unreadList;
    private java.util.List<com.example.employee.entity.AnnouncementVisibility> visibilityList;
}
