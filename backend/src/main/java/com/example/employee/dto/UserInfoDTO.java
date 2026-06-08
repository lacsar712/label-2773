package com.example.employee.dto;

import lombok.Data;

@Data
public class UserInfoDTO {
    private Long userId;
    private Long employeeId;
    private String username;
    private String nickname;
    private String email;
    private String phone;
    private String avatar;
    private Long roleId;
    private String roleCode;
    private String roleName;
    private Boolean isFirstLogin;
}
