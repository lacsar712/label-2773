package com.example.employee.dto;

import lombok.Data;

@Data
public class LoginResponseDTO {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresIn;
    private Long userId;
    private Long employeeId;
    private String username;
    private String nickname;
    private String roleCode;
    private String roleName;
    private String avatar;
    private Boolean isFirstLogin;
}
