package com.example.employee.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.employee.entity.Announcement;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AnnouncementMapper extends BaseMapper<Announcement> {
}
