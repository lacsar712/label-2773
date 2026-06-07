package com.example.employee.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.employee.entity.Holiday;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HolidayMapper extends BaseMapper<Holiday> {
}
