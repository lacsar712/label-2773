package com.example.employee.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_login_log")
public class SysLoginLog {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("username")
    private String username;

    @TableField("login_type")
    private String loginType;

    @TableField("status")
    private Integer status;

    @TableField("ip_address")
    private String ipAddress;

    @TableField("login_location")
    private String loginLocation;

    @TableField("browser")
    private String browser;

    @TableField("os")
    private String os;

    @TableField("device")
    private String device;

    @TableField("message")
    private String message;

    @TableField("login_time")
    private LocalDateTime loginTime;
}
