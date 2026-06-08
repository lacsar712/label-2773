package com.example.employee.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
@TableName("employee")
public class Employee {
    @TableId(type = IdType.AUTO)
    private Long id;

    @NotBlank(message = "姓名不能为空")
    private String name;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @NotNull(message = "部门不能为空")
    private Long departmentId;

    @NotBlank(message = "职位不能为空")
    private String role;

    private String jobLevel;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate hireDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate leaveDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate contractEndDate;

    private Integer status;

    @TableField(exist = false)
    private String departmentName;
}
