package com.example.employee.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("department")
public class Department {
    @TableId(type = IdType.AUTO)
    private Long id;

    @NotBlank(message = "部门名称不能为空")
    private String name;

    @NotBlank(message = "部门编码不能为空")
    private String code;

    private String description;

    private String leader;

    private String deputyLeader;

    private Long parentId;

    @NotNull(message = "编制人数上限不能为空")
    private Integer headcountLimit;

    private Integer levelType;

    @NotNull(message = "启用状态不能为空")
    private Boolean enabled;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private List<Department> children;

    @TableField(exist = false)
    private Integer employeeCount;

    @TableField(exist = false)
    private Integer activeEmployeeCount;

    @TableField(exist = false)
    private Boolean overHeadcount;

    @TableField(exist = false)
    private String parentName;
}
