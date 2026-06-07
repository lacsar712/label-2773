package com.example.employee.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.employee.entity.Holiday;
import com.example.employee.mapper.HolidayMapper;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class HolidayService extends ServiceImpl<HolidayMapper, Holiday> {

    public List<Holiday> getHolidaysByYear(Integer year) {
        LambdaQueryWrapper<Holiday> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Holiday::getYear, year);
        return list(wrapper);
    }

    public Set<LocalDate> getHolidayDates(Integer year) {
        List<Holiday> holidays = getHolidaysByYear(year);
        return holidays.stream()
                .filter(h -> h.getHolidayType() == 1)
                .map(Holiday::getHolidayDate)
                .collect(Collectors.toSet());
    }

    public Set<LocalDate> getWorkExtraDates(Integer year) {
        List<Holiday> holidays = getHolidaysByYear(year);
        return holidays.stream()
                .filter(h -> h.getHolidayType() == 2)
                .map(Holiday::getHolidayDate)
                .collect(Collectors.toSet());
    }

    public boolean isWorkDay(LocalDate date) {
        Set<LocalDate> holidays = getHolidayDates(date.getYear());
        Set<LocalDate> workExtras = getWorkExtraDates(date.getYear());

        if (workExtras.contains(date)) {
            return true;
        }
        if (holidays.contains(date)) {
            return false;
        }
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY;
    }

    public long countWorkDays(LocalDate startDate, LocalDate endDate) {
        long count = 0;
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            if (isWorkDay(current)) {
                count++;
            }
            current = current.plusDays(1);
        }
        return count;
    }
}
