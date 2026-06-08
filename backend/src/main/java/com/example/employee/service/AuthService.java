package com.example.employee.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.employee.common.ResultCode;
import com.example.employee.config.JwtConfig;
import com.example.employee.context.UserContext;
import com.example.employee.dto.ChangePasswordRequestDTO;
import com.example.employee.dto.LoginRequestDTO;
import com.example.employee.dto.LoginResponseDTO;
import com.example.employee.dto.UserInfoDTO;
import com.example.employee.entity.Employee;
import com.example.employee.entity.SysRole;
import com.example.employee.entity.SysUser;
import com.example.employee.exception.BusinessException;
import com.example.employee.mapper.EmployeeMapper;
import com.example.employee.mapper.SysCaptchaMapper;
import com.example.employee.mapper.SysRoleMapper;
import com.example.employee.mapper.SysUserMapper;
import com.example.employee.utils.IpUtil;
import com.example.employee.utils.JwtUtil;
import com.example.employee.utils.UserAgentUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthService {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private SysRoleMapper sysRoleMapper;

    @Autowired
    private EmployeeMapper employeeMapper;

    @Autowired
    private SysCaptchaMapper sysCaptchaMapper;

    @Autowired
    private SysLoginLogService sysLoginLogService;

    @Autowired
    private CaptchaService captchaService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private JwtConfig jwtConfig;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private HttpServletRequest request;

    @Transactional
    public LoginResponseDTO login(LoginRequestDTO dto) {
        String ip = IpUtil.getIpAddr(request);
        String browser = UserAgentUtil.getBrowser(request);
        String os = UserAgentUtil.getOS(request);
        String device = UserAgentUtil.getDevice(request);

        if (!captchaService.validateCaptcha(dto.getUuid(), dto.getCaptcha())) {
            sysLoginLogService.recordLogin(null, dto.getUsername(), 0, ip, browser, os, device, "验证码错误");
            throw new BusinessException(ResultCode.CAPTCHA_ERROR.getCode(), "验证码错误");
        }

        LambdaQueryWrapper<SysUser> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.eq(SysUser::getUsername, dto.getUsername());
        SysUser user = sysUserMapper.selectOne(userWrapper);

        if (user == null) {
            sysLoginLogService.recordLogin(null, dto.getUsername(), 0, ip, browser, os, device, "用户不存在");
            throw new BusinessException(ResultCode.USER_NOT_FOUND.getCode(), "用户不存在");
        }

        if (user.getStatus() != null && user.getStatus() == 0) {
            sysLoginLogService.recordLogin(user.getId(), user.getUsername(), 0, ip, browser, os, device, "用户已禁用");
            throw new BusinessException(ResultCode.USER_DISABLED.getCode(), "用户已禁用");
        }

        if (user.getLockTime() != null && user.getLockTime().plusMinutes(30).isAfter(LocalDateTime.now())) {
            sysLoginLogService.recordLogin(user.getId(), user.getUsername(), 0, ip, browser, os, device, "用户已锁定");
            throw new BusinessException(ResultCode.USER_LOCKED.getCode(), "用户已锁定，请30分钟后再试");
        }

        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            int failCount = (user.getLoginFailCount() == null ? 0 : user.getLoginFailCount()) + 1;
            user.setLoginFailCount(failCount);

            String message;
            if (failCount >= 5) {
                user.setLockTime(LocalDateTime.now());
                message = "密码错误次数过多，账户已锁定30分钟";
                sysUserMapper.updateById(user);
                sysLoginLogService.recordLogin(user.getId(), user.getUsername(), 0, ip, browser, os, device, message);
                throw new BusinessException(ResultCode.USER_LOCKED.getCode(), message);
            } else {
                message = "密码错误，剩余尝试次数：" + (5 - failCount);
                sysUserMapper.updateById(user);
                sysLoginLogService.recordLogin(user.getId(), user.getUsername(), 0, ip, browser, os, device, message);
                throw new BusinessException(ResultCode.PASSWORD_ERROR.getCode(), message);
            }
        }

        user.setLoginFailCount(0);
        user.setLockTime(null);

        String sessionToken = UUID.randomUUID().toString().replace("-", "");
        user.setSessionToken(sessionToken);
        user.setLastLoginTime(LocalDateTime.now());
        user.setLastLoginIp(ip);
        sysUserMapper.updateById(user);

        SysRole role = sysRoleMapper.selectById(user.getRoleId());
        String roleCode = role != null ? role.getRoleCode() : null;
        String roleName = role != null ? role.getRoleName() : null;

        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername(), roleCode);
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername());

        sysLoginLogService.recordLogin(user.getId(), user.getUsername(), 1, ip, browser, os, device, "登录成功");

        LoginResponseDTO response = new LoginResponseDTO();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setTokenType("Bearer");
        response.setExpiresIn(jwtConfig.getAccessTokenExpire() / 1000);
        response.setUserId(user.getId());
        response.setEmployeeId(findEmployeeIdByUser(user));
        response.setUsername(user.getUsername());
        response.setNickname(user.getNickname());
        response.setRoleCode(roleCode);
        response.setRoleName(roleName);
        response.setAvatar(user.getAvatar());
        response.setIsFirstLogin(user.getIsFirstLogin() != null && user.getIsFirstLogin() == 1);
        return response;
    }

    private Long findEmployeeIdByUser(SysUser user) {
        if (user == null) return null;
        if (user.getEmployeeId() != null) {
            return user.getEmployeeId();
        }
        LambdaQueryWrapper<Employee> wrapper = new LambdaQueryWrapper<>();
        boolean hasCondition = false;
        if (user.getEmail() != null && !user.getEmail().isEmpty()) {
            wrapper.eq(Employee::getEmail, user.getEmail());
            hasCondition = true;
        }
        String nameKeyword = user.getNickname() != null && !user.getNickname().isEmpty()
                ? user.getNickname() : user.getUsername();
        if (nameKeyword != null && !nameKeyword.isEmpty()) {
            if (hasCondition) {
                wrapper.or();
            }
            wrapper.eq(Employee::getName, nameKeyword);
            hasCondition = true;
        }
        if (!hasCondition) {
            return null;
        }
        Employee emp = employeeMapper.selectOne(wrapper);
        if (emp != null) {
            user.setEmployeeId(emp.getId());
            sysUserMapper.updateById(user);
        }
        return emp != null ? emp.getId() : null;
    }

    public LoginResponseDTO refreshToken(String refreshToken) {
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new BusinessException(ResultCode.TOKEN_INVALID.getCode(), "Token无效");
        }

        String tokenType = jwtUtil.getTokenType(refreshToken);
        if (!"refresh".equals(tokenType)) {
            throw new BusinessException(ResultCode.TOKEN_INVALID.getCode(), "Token类型错误");
        }

        Long userId = jwtUtil.getUserIdFromToken(refreshToken);
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND.getCode(), "用户不存在");
        }

        Claims claims = jwtUtil.parseToken(refreshToken);
        String tokenUsername = claims.get("username", String.class);
        if (!user.getUsername().equals(tokenUsername)) {
            throw new BusinessException(ResultCode.TOKEN_INVALID.getCode(), "Token无效");
        }

        if (user.getSessionToken() == null) {
            throw new BusinessException(ResultCode.SESSION_KICKED.getCode(), "您已在其他地方登录，请重新登录");
        }

        SysRole role = sysRoleMapper.selectById(user.getRoleId());
        String roleCode = role != null ? role.getRoleCode() : null;
        String roleName = role != null ? role.getRoleName() : null;

        String newAccessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername(), roleCode);

        LoginResponseDTO response = new LoginResponseDTO();
        response.setAccessToken(newAccessToken);
        response.setRefreshToken(refreshToken);
        response.setTokenType("Bearer");
        response.setExpiresIn(jwtConfig.getAccessTokenExpire() / 1000);
        response.setUserId(user.getId());
        response.setEmployeeId(findEmployeeIdByUser(user));
        response.setUsername(user.getUsername());
        response.setNickname(user.getNickname());
        response.setRoleCode(roleCode);
        response.setRoleName(roleName);
        response.setAvatar(user.getAvatar());
        response.setIsFirstLogin(user.getIsFirstLogin() != null && user.getIsFirstLogin() == 1);
        return response;
    }

    @Transactional
    public void logout() {
        UserInfoDTO currentUser = UserContext.getCurrentUser();
        if (currentUser == null) {
            return;
        }

        SysUser user = sysUserMapper.selectById(currentUser.getUserId());
        if (user != null) {
            user.setSessionToken(null);
            sysUserMapper.updateById(user);
        }

        String ip = IpUtil.getIpAddr(request);
        String browser = UserAgentUtil.getBrowser(request);
        String os = UserAgentUtil.getOS(request);
        String device = UserAgentUtil.getDevice(request);

        sysLoginLogService.recordLogout(currentUser.getUserId(), currentUser.getUsername(), ip, browser, os, device);
    }

    @Transactional
    public void changePassword(ChangePasswordRequestDTO dto) {
        UserInfoDTO currentUser = UserContext.getCurrentUser();
        if (currentUser == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "未登录");
        }

        SysUser user = sysUserMapper.selectById(currentUser.getUserId());
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND.getCode(), "用户不存在");
        }

        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.PASSWORD_ERROR.getCode(), "原密码错误");
        }

        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        user.setIsFirstLogin(0);
        sysUserMapper.updateById(user);
    }

    public UserInfoDTO getUserInfo() {
        UserInfoDTO currentUser = UserContext.getCurrentUser();
        if (currentUser == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "未登录");
        }

        SysUser user = sysUserMapper.selectById(currentUser.getUserId());
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND.getCode(), "用户不存在");
        }

        SysRole role = sysRoleMapper.selectById(user.getRoleId());

        UserInfoDTO dto = new UserInfoDTO();
        dto.setUserId(user.getId());
        dto.setEmployeeId(findEmployeeIdByUser(user));
        dto.setUsername(user.getUsername());
        dto.setNickname(user.getNickname());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setAvatar(user.getAvatar());
        dto.setRoleId(user.getRoleId());
        if (role != null) {
            dto.setRoleCode(role.getRoleCode());
            dto.setRoleName(role.getRoleName());
        }
        dto.setIsFirstLogin(user.getIsFirstLogin() != null && user.getIsFirstLogin() == 1);
        return dto;
    }

    public void validateTokenAndSetContext(String token) {
        if (!jwtUtil.validateToken(token)) {
            throw new BusinessException(ResultCode.TOKEN_EXPIRED.getCode(), "Token已过期或无效");
        }

        String tokenType = jwtUtil.getTokenType(token);
        if (!"access".equals(tokenType)) {
            throw new BusinessException(ResultCode.TOKEN_INVALID.getCode(), "Token类型错误");
        }

        Long userId = jwtUtil.getUserIdFromToken(token);
        String username = jwtUtil.getUsernameFromToken(token);
        String roleCode = jwtUtil.getRoleCodeFromToken(token);

        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND.getCode(), "用户不存在");
        }

        if (user.getSessionToken() == null) {
            throw new BusinessException(ResultCode.SESSION_KICKED.getCode(), "您已在其他地方登录，请重新登录");
        }

        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(ResultCode.USER_DISABLED.getCode(), "用户已禁用");
        }

        UserInfoDTO userInfo = new UserInfoDTO();
        userInfo.setUserId(userId);
        userInfo.setUsername(username);
        userInfo.setRoleCode(roleCode);
        userInfo.setEmployeeId(findEmployeeIdByUser(user));
        UserContext.setCurrentUser(userInfo);
    }
}
