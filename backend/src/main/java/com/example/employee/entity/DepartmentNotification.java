package com.example.employee.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("department_notification")
public class DepartmentNotification {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long departmentId;

    private String departmentName;

    private String oldLeader;

    private String newLeader;

    private String content;

    private LocalDateTime createTime;
}
