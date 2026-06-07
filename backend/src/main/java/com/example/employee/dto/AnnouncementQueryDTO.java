package com.example.employee.dto;

import lombok.Data;

@Data
public class AnnouncementQueryDTO {
    private String title;
    private Integer status;
    private Integer pageNum = 1;
    private Integer pageSize = 10;
}
