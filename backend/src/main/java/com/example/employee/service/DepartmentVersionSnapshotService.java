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
import java.util.ArrayList;
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

    public List<Department> getSnapshotTree(Long id) {
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

    private List<Department> flattenTree(List<Department> tree) {
        List<Department> result = new ArrayList<>();
        flattenTreeRecursive(tree, result);
        return result;
    }

    private void flattenTreeRecursive(List<Department> nodes, List<Department> result) {
        if (nodes == null) return;
        for (Department dept : nodes) {
            result.add(dept);
            if (dept.getChildren() != null && !dept.getChildren().isEmpty()) {
                flattenTreeRecursive(dept.getChildren(), result);
            }
        }
    }

    @Transactional
    public boolean applySnapshot(Long id, Long operatorId, String operatorName) {
        DepartmentVersionSnapshot snapshot = getById(id);
        if (snapshot == null) {
            throw new RuntimeException("快照不存在");
        }

        createAutoSnapshot(operatorId, operatorName, "恢复快照前自动备份 - " + snapshot.getSnapshotName());

        List<Department> snapshotTree;
        try {
            snapshotTree = objectMapper.readValue(snapshot.getTreeSnapshot(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Department.class));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("反序列化部门树失败", e);
        }

        List<Department> flatSnapshot = flattenTree(snapshotTree);

        for (Department snapDept : flatSnapshot) {
            Department existing = departmentService.getById(snapDept.getId());
            if (existing != null) {
                existing.setName(snapDept.getName());
                existing.setCode(snapDept.getCode());
                existing.setDescription(snapDept.getDescription());
                existing.setLeader(snapDept.getLeader());
                existing.setDeputyLeader(snapDept.getDeputyLeader());
                existing.setParentId(snapDept.getParentId());
                existing.setHeadcountLimit(snapDept.getHeadcountLimit());
                existing.setLevelType(snapDept.getLevelType());
                existing.setEnabled(snapDept.getEnabled());
                departmentService.updateById(existing);
            } else {
                Department newDept = new Department();
                newDept.setId(snapDept.getId());
                newDept.setName(snapDept.getName());
                newDept.setCode(snapDept.getCode());
                newDept.setDescription(snapDept.getDescription());
                newDept.setLeader(snapDept.getLeader());
                newDept.setDeputyLeader(snapDept.getDeputyLeader());
                newDept.setParentId(snapDept.getParentId());
                newDept.setHeadcountLimit(snapDept.getHeadcountLimit());
                newDept.setLevelType(snapDept.getLevelType());
                newDept.setEnabled(snapDept.getEnabled());
                departmentService.save(newDept);
            }
        }

        createAutoSnapshot(operatorId, operatorName, "恢复快照后 - " + snapshot.getSnapshotName());
        return true;
    }

    @Transactional
    public boolean deleteSnapshot(Long id) {
        return removeById(id);
    }
}
