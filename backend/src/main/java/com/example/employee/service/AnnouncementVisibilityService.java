package com.example.employee.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.employee.entity.AnnouncementVisibility;
import com.example.employee.mapper.AnnouncementVisibilityMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AnnouncementVisibilityService extends ServiceImpl<AnnouncementVisibilityMapper, AnnouncementVisibility> {

    public List<AnnouncementVisibility> listByAnnouncementId(Long announcementId) {
        return lambdaQuery()
                .eq(AnnouncementVisibility::getAnnouncementId, announcementId)
                .list();
    }

    public void removeByAnnouncementId(Long announcementId) {
        lambdaUpdate()
                .eq(AnnouncementVisibility::getAnnouncementId, announcementId)
                .remove();
    }
}
