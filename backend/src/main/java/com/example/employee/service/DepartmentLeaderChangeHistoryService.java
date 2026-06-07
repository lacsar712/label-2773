package com.example.employee.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.employee.entity.DepartmentLeaderChangeHistory;
import com.example.employee.mapper.DepartmentLeaderChangeHistoryMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DepartmentLeaderChangeHistoryService extends ServiceImpl<DepartmentLeaderChangeHistoryMapper, DepartmentLeaderChangeHistory> {

    public List<DepartmentLeaderChangeHistory> listByDepartmentId(Long departmentId) {
        LambdaQueryWrapper<DepartmentLeaderChangeHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DepartmentLeaderChangeHistory::getDepartmentId, departmentId)
                .orderByDesc(DepartmentLeaderChangeHistory::getCreateTime);
        return list(wrapper);
    }
}
