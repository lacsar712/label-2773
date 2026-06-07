package com.example.employee.controller;

import com.example.employee.common.Result;
import com.example.employee.dto.CaptchaResponseDTO;
import com.example.employee.dto.ChangePasswordRequestDTO;
import com.example.employee.dto.LoginRequestDTO;
import com.example.employee.dto.LoginResponseDTO;
import com.example.employee.dto.RefreshTokenRequestDTO;
import com.example.employee.dto.UserInfoDTO;
import com.example.employee.service.AuthService;
import com.example.employee.service.CaptchaService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
@Tag(name = "认证接口")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private CaptchaService captchaService;

    @GetMapping("/captcha")
    public Result<CaptchaResponseDTO> getCaptcha() {
        return Result.success(captchaService.generateCaptcha());
    }

    @PostMapping("/login")
    public Result<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO dto) {
        return Result.success(authService.login(dto));
    }

    @PostMapping("/refresh-token")
    public Result<LoginResponseDTO> refreshToken(@Valid @RequestBody RefreshTokenRequestDTO dto) {
        return Result.success(authService.refreshToken(dto.getRefreshToken()));
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.success("登出成功");
    }

    @PostMapping("/change-password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequestDTO dto) {
        authService.changePassword(dto);
        return Result.success("密码修改成功");
    }

    @GetMapping("/user-info")
    public Result<UserInfoDTO> getUserInfo() {
        return Result.success(authService.getUserInfo());
    }
}
