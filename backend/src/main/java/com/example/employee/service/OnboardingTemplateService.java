package com.example.employee.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.employee.entity.OnboardingTemplate;
import com.example.employee.entity.OnboardingTemplateItem;
import com.example.employee.mapper.OnboardingTemplateItemMapper;
import com.example.employee.mapper.OnboardingTemplateMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OnboardingTemplateService extends ServiceImpl<OnboardingTemplateMapper, OnboardingTemplate> {

    @Autowired
    private OnboardingTemplateItemMapper templateItemMapper;

    public List<OnboardingTemplate> listWithItems() {
        List<OnboardingTemplate> templates = list();
        for (OnboardingTemplate template : templates) {
            enrichTemplateWithItems(template);
        }
        return templates;
    }

    public OnboardingTemplate getDetailWithItems(Long id) {
        OnboardingTemplate template = getById(id);
        if (template != null) {
            enrichTemplateWithItems(template);
        }
        return template;
    }

    private void enrichTemplateWithItems(OnboardingTemplate template) {
        LambdaQueryWrapper<OnboardingTemplateItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OnboardingTemplateItem::getTemplateId, template.getId())
                .orderByAsc(OnboardingTemplateItem::getStage, OnboardingTemplateItem::getSortOrder);
        List<OnboardingTemplateItem> items = templateItemMapper.selectList(wrapper);
        template.setItems(items);
    }

    public List<OnboardingTemplate> listEnabled() {
        LambdaQueryWrapper<OnboardingTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OnboardingTemplate::getEnabled, true);
        return list(wrapper);
    }

    public OnboardingTemplate findMatchingTemplate(Long departmentId, String position) {
        LambdaQueryWrapper<OnboardingTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OnboardingTemplate::getEnabled, true);

        if (departmentId != null && position != null) {
            wrapper.and(w -> w.eq(OnboardingTemplate::getDepartmentId, departmentId)
                    .eq(OnboardingTemplate::getPosition, position));
            wrapper.or(w -> w.eq(OnboardingTemplate::getDepartmentId, departmentId)
                    .isNull(OnboardingTemplate::getPosition));
            wrapper.or(w -> w.isNull(OnboardingTemplate::getDepartmentId)
                    .isNull(OnboardingTemplate::getPosition)
                    .eq(OnboardingTemplate::getIsDefault, true));
        } else if (departmentId != null) {
            wrapper.and(w -> w.eq(OnboardingTemplate::getDepartmentId, departmentId)
                    .isNull(OnboardingTemplate::getPosition));
            wrapper.or(w -> w.isNull(OnboardingTemplate::getDepartmentId)
                    .isNull(OnboardingTemplate::getPosition)
                    .eq(OnboardingTemplate::getIsDefault, true));
        } else {
            wrapper.isNull(OnboardingTemplate::getDepartmentId)
                    .isNull(OnboardingTemplate::getPosition)
                    .eq(OnboardingTemplate::getIsDefault, true);
        }

        List<OnboardingTemplate> templates = list(wrapper);
        if (templates.isEmpty()) {
            return null;
        }

        return templates.stream()
                .sorted(Comparator.comparingInt((OnboardingTemplate t) -> {
                    int score = 0;
                    if (t.getDepartmentId() != null && t.getDepartmentId().equals(departmentId)) score += 10;
                    if (t.getPosition() != null && t.getPosition().equals(position)) score += 5;
                    if (Boolean.TRUE.equals(t.getIsDefault())) score += 1;
                    return -score;
                }))
                .findFirst()
                .orElse(null);
    }

    @Transactional
    public boolean createTemplateWithItems(OnboardingTemplate template) {
        boolean result = save(template);
        if (result && template.getItems() != null) {
            for (OnboardingTemplateItem item : template.getItems()) {
                item.setTemplateId(template.getId());
                templateItemMapper.insert(item);
            }
        }
        return result;
    }

    @Transactional
    public boolean updateTemplateWithItems(OnboardingTemplate template) {
        boolean result = updateById(template);
        if (result && template.getItems() != null) {
            LambdaQueryWrapper<OnboardingTemplateItem> deleteWrapper = new LambdaQueryWrapper<>();
            deleteWrapper.eq(OnboardingTemplateItem::getTemplateId, template.getId());
            templateItemMapper.delete(deleteWrapper);

            for (OnboardingTemplateItem item : template.getItems()) {
                item.setId(null);
                item.setTemplateId(template.getId());
                templateItemMapper.insert(item);
            }
        }
        return result;
    }

    @Transactional
    public boolean deleteTemplateWithItems(Long id) {
        LambdaQueryWrapper<OnboardingTemplateItem> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(OnboardingTemplateItem::getTemplateId, id);
        templateItemMapper.delete(deleteWrapper);
        return removeById(id);
    }
}
