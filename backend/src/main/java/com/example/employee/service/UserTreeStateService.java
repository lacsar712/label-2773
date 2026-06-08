package com.example.employee.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.employee.dto.TreeStateDTO;
import com.example.employee.entity.UserTreeState;
import com.example.employee.mapper.UserTreeStateMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserTreeStateService extends ServiceImpl<UserTreeStateMapper, UserTreeState> {

    @Autowired
    private ObjectMapper objectMapper;

    public TreeStateDTO getTreeState(Long userId, String treeKey) {
        LambdaQueryWrapper<UserTreeState> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserTreeState::getUserId, userId)
                .eq(UserTreeState::getTreeKey, treeKey);
        UserTreeState state = getOne(wrapper);

        TreeStateDTO dto = new TreeStateDTO();
        dto.setTreeKey(treeKey);
        if (state != null) {
            try {
                List<Long> expandedIds = objectMapper.readValue(state.getExpandedIds(),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, Long.class));
                dto.setExpandedIds(expandedIds);
            } catch (JsonProcessingException e) {
                dto.setExpandedIds(new ArrayList<>());
            }
            dto.setSelectedId(state.getSelectedId());
        } else {
            dto.setExpandedIds(new ArrayList<>());
        }
        return dto;
    }

    @Transactional
    public boolean saveTreeState(Long userId, TreeStateDTO dto) {
        LambdaQueryWrapper<UserTreeState> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserTreeState::getUserId, userId)
                .eq(UserTreeState::getTreeKey, dto.getTreeKey());
        UserTreeState existing = getOne(wrapper);

        String expandedIdsJson;
        try {
            expandedIdsJson = objectMapper.writeValueAsString(dto.getExpandedIds() != null ? dto.getExpandedIds() : new ArrayList<>());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("序列化展开节点失败", e);
        }

        if (existing != null) {
            existing.setExpandedIds(expandedIdsJson);
            existing.setSelectedId(dto.getSelectedId());
            return updateById(existing);
        } else {
            UserTreeState state = new UserTreeState();
            state.setUserId(userId);
            state.setTreeKey(dto.getTreeKey());
            state.setExpandedIds(expandedIdsJson);
            state.setSelectedId(dto.getSelectedId());
            return save(state);
        }
    }
}
