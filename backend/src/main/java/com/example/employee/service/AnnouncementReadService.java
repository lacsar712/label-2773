package com.example.employee.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.employee.entity.AnnouncementRead;
import com.example.employee.mapper.AnnouncementReadMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AnnouncementReadService extends ServiceImpl<AnnouncementReadMapper, AnnouncementRead> {

    public List<AnnouncementRead> listByAnnouncementId(Long announcementId) {
        return lambdaQuery()
                .eq(AnnouncementRead::getAnnouncementId, announcementId)
                .orderByDesc(AnnouncementRead::getReadTime)
                .list();
    }

    public boolean hasRead(Long announcementId, Long userId) {
        return lambdaQuery()
                .eq(AnnouncementRead::getAnnouncementId, announcementId)
                .eq(AnnouncementRead::getUserId, userId)
                .count() > 0;
    }

    public boolean markAsRead(Long announcementId, Long userId, String userName, Long employeeId, Long departmentId) {
        if (hasRead(announcementId, userId)) {
            return true;
        }
        AnnouncementRead read = new AnnouncementRead();
        read.setAnnouncementId(announcementId);
        read.setUserId(userId);
        read.setUserName(userName);
        read.setEmployeeId(employeeId);
        read.setDepartmentId(departmentId);
        read.setReadTime(LocalDateTime.now());
        return save(read);
    }

    public long countReadByAnnouncementId(Long announcementId) {
        return lambdaQuery()
                .eq(AnnouncementRead::getAnnouncementId, announcementId)
                .count();
    }
}
