package com.example.employee.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("leave_approval_node")
public class LeaveApprovalNode {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long applicationId;

    private Integer nodeIndex;

    private String nodeName;

    private Long approverId;

    private String approverName;

    private Integer nodeType;

    private Integer status;

    private String approvalRemark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime approvalTime;

    private Long originalApproverId;

    private String originalApproverName;

    private Long addSignApproverId;

    private String addSignApproverName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private String statusName;
}
