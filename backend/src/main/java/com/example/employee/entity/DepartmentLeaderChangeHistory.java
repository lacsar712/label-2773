package com.example.employee.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("department_leader_change_history")
public class DepartmentLeaderChangeHistory {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long departmentId;

    private String departmentName;

    private Integer changeType;

    private String oldLeader;

    private String newLeader;

    private Long operatorId;

    private String operatorName;

    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
