package com.example.employee.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.employee.entity.OnboardingNotification;
import com.example.employee.mapper.OnboardingNotificationMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OnboardingNotificationService extends ServiceImpl<OnboardingNotificationMapper, OnboardingNotification> {

    public List<OnboardingNotification> listByRecipient(Long recipientId) {
        LambdaQueryWrapper<OnboardingNotification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OnboardingNotification::getRecipientId, recipientId)
                .orderByDesc(OnboardingNotification::getCreateTime);
        return list(wrapper);
    }

    public long countUnreadByRecipient(Long recipientId) {
        LambdaQueryWrapper<OnboardingNotification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OnboardingNotification::getRecipientId, recipientId)
                .eq(OnboardingNotification::getIsRead, false);
        return count(wrapper);
    }

    public boolean markAsRead(Long id) {
        OnboardingNotification notification = getById(id);
        if (notification == null) {
            return false;
        }
        notification.setIsRead(true);
        notification.setReadTime(LocalDateTime.now());
        return updateById(notification);
    }

    public boolean markAllAsRead(Long recipientId) {
        LambdaQueryWrapper<OnboardingNotification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OnboardingNotification::getRecipientId, recipientId)
                .eq(OnboardingNotification::getIsRead, false);
        List<OnboardingNotification> unread = list(wrapper);
        for (OnboardingNotification n : unread) {
            n.setIsRead(true);
            n.setReadTime(LocalDateTime.now());
            updateById(n);
        }
        return true;
    }
}
