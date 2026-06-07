package com.example.employee.service;

import com.example.employee.entity.SysLoginLog;
import com.example.employee.mapper.SysLoginLogMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class SysLoginLogService {

    @Autowired
    private SysLoginLogMapper sysLoginLogMapper;

    public void recordLogin(Long userId, String username, Integer status, String ip, String browser, String os, String device, String message) {
        SysLoginLog log = new SysLoginLog();
        log.setUserId(userId);
        log.setUsername(username);
        log.setLoginType("1");
        log.setStatus(status);
        log.setIpAddress(ip);
        log.setBrowser(browser);
        log.setOs(os);
        log.setDevice(device);
        log.setMessage(message);
        log.setLoginTime(LocalDateTime.now());
        sysLoginLogMapper.insert(log);
    }

    public void recordLogout(Long userId, String username, String ip, String browser, String os, String device) {
        SysLoginLog log = new SysLoginLog();
        log.setUserId(userId);
        log.setUsername(username);
        log.setLoginType("2");
        log.setStatus(1);
        log.setIpAddress(ip);
        log.setBrowser(browser);
        log.setOs(os);
        log.setDevice(device);
        log.setMessage("登出成功");
        log.setLoginTime(LocalDateTime.now());
        sysLoginLogMapper.insert(log);
    }
}
