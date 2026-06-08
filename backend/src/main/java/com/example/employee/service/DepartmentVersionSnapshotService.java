package com.example.employee.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.employee.dto.CreateSnapshotDTO;
import com.example.employee.entity.Department;
import com.example.employee.entity.DepartmentVersionSnapshot;
import com.example.employee.mapper.DepartmentVersionSnapshotMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class DepartmentVersionSnapshotService extends ServiceImpl<DepartmentVersionSnapshotMapper, DepartmentVersionSnapshot> {

    @Autowired
    @Lazy
    private DepartmentService departmentService;

    @Autowired
    private ObjectMapper objectMapper;

    public List<DepartmentVersionSnapshot> listAll() {
        LambdaQueryWrapper<DepartmentVersionSnapshot> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(DepartmentVersionSnapshot::getCreateTime);
        return list(wrapper);
    }

    @Transactional
    public DepartmentVersionSnapshot createSnapshot(CreateSnapshotDTO dto, Long operatorId, String operatorName) {
        List<Department> tree = departmentService.listWithTree();
        String snapshotJson;
        try {
            snapshotJson = objectMapper.writeValueAsString(tree);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("序列化部门树失败", e);
        }

        DepartmentVersionSnapshot snapshot = new DepartmentVersionSnapshot();
        snapshot.setSnapshotName(dto.getSnapshotName());
        snapshot.setSnapshotType(1);
        snapshot.setTreeSnapshot(snapshotJson);
        snapshot.setDescription(dto.getDescription());
        snapshot.setOperatorId(operatorId);
        snapshot.setOperatorName(operatorName);
        snapshot.setCreateTime(LocalDateTime.now());
        save(snapshot);
        return snapshot;
    }

    @Transactional
    public DepartmentVersionSnapshot createAutoSnapshot(Long operatorId, String operatorName, String description) {
        List<Department> tree = departmentService.listWithTree();
        String snapshotJson;
        try {
            snapshotJson = objectMapper.writeValueAsString(tree);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("序列化部门树失败", e);
        }

        DepartmentVersionSnapshot snapshot = new DepartmentVersionSnapshot();
        snapshot.setSnapshotName("自动快照-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        snapshot.setSnapshotType(2);
        snapshot.setTreeSnapshot(snapshotJson);
        snapshot.setDescription(description);
        snapshot.setOperatorId(operatorId);
        snapshot.setOperatorName(operatorName);
        snapshot.setCreateTime(LocalDateTime.now());
        save(snapshot);
        return snapshot;
    }

    public DepartmentVersionSnapshot getSnapshot(Long id) {
        return getById(id);
    }

    public List<Department> restoreSnapshot(Long id) {
        DepartmentVersionSnapshot snapshot = getById(id);
        if (snapshot == null) {
            throw new RuntimeException("快照不存在");
        }
        try {
            return objectMapper.readValue(snapshot.getTreeSnapshot(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Department.class));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("反序列化部门树失败", e);
        }
    }

    @Transactional
    public boolean deleteSnapshot(Long id) {
        return removeById(id);
    }
}
