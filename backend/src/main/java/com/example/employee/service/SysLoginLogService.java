package com.example.employee.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.employee.dto.LoginLogQueryDTO;
import com.example.employee.dto.PageResultDTO;
import com.example.employee.entity.SysLoginLog;
import com.example.employee.mapper.SysLoginLogMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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

    public PageResultDTO<SysLoginLog> getLoginLogPage(LoginLogQueryDTO query, Long currentUserId, String currentUserRoleCode) {
        Page<SysLoginLog> page = new Page<>(query.getPageNum(), query.getPageSize());

        LambdaQueryWrapper<SysLoginLog> wrapper = new LambdaQueryWrapper<>();

        if (!"ADMIN".equals(currentUserRoleCode)) {
            wrapper.eq(SysLoginLog::getUserId, currentUserId);
        }

        if (StringUtils.hasText(query.getUsername())) {
            wrapper.like(SysLoginLog::getUsername, query.getUsername());
        }

        if (query.getStatus() != null) {
            wrapper.eq(SysLoginLog::getStatus, query.getStatus());
        }

        if (StringUtils.hasText(query.getLoginType())) {
            wrapper.eq(SysLoginLog::getLoginType, query.getLoginType());
        }

        if (query.getStartTime() != null) {
            wrapper.ge(SysLoginLog::getLoginTime, query.getStartTime());
        }

        if (query.getEndTime() != null) {
            wrapper.le(SysLoginLog::getLoginTime, query.getEndTime());
        }

        wrapper.orderByDesc(SysLoginLog::getLoginTime);

        Page<SysLoginLog> resultPage = sysLoginLogMapper.selectPage(page, wrapper);

        PageResultDTO<SysLoginLog> result = new PageResultDTO<>();
        result.setList(resultPage.getRecords());
        result.setRecords(resultPage.getRecords());
        result.setTotal(resultPage.getTotal());
        result.setPageNum(query.getPageNum());
        result.setPageSize(query.getPageSize());
        result.setCurrent(query.getPageNum());
        result.setSize(query.getPageSize());
        result.setPages(resultPage.getPages());
        return result;
    }
}
