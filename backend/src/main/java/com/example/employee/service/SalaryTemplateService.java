package com.example.employee.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.employee.entity.SalaryTemplate;
import com.example.employee.mapper.SalaryTemplateMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SalaryTemplateService extends ServiceImpl<SalaryTemplateMapper, SalaryTemplate> {

    public List<SalaryTemplate> listEnabled() {
        LambdaQueryWrapper<SalaryTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SalaryTemplate::getEnabled, 1)
                .orderByDesc(SalaryTemplate::getCreateTime);
        return list(wrapper);
    }

    public List<SalaryTemplate> listAll(String keyword) {
        LambdaQueryWrapper<SalaryTemplate> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(SalaryTemplate::getTemplateName, keyword)
                    .or().like(SalaryTemplate::getJobLevel, keyword));
        }
        wrapper.orderByDesc(SalaryTemplate::getCreateTime);
        return list(wrapper);
    }

    public SalaryTemplate createTemplate(SalaryTemplate template) {
        if (template.getEnabled() == null) {
            template.setEnabled(1);
        }
        if (template.getPerformanceCoefficient() == null) {
            template.setPerformanceCoefficient(new java.math.BigDecimal("1.00"));
        }
        if (template.getSocialInsurancePersonalRate() == null) {
            template.setSocialInsurancePersonalRate(new java.math.BigDecimal("0.1050"));
        }
        if (template.getSocialInsuranceCompanyRate() == null) {
            template.setSocialInsuranceCompanyRate(new java.math.BigDecimal("0.2716"));
        }
        if (template.getHousingFundPersonalRate() == null) {
            template.setHousingFundPersonalRate(new java.math.BigDecimal("0.0700"));
        }
        if (template.getHousingFundCompanyRate() == null) {
            template.setHousingFundCompanyRate(new java.math.BigDecimal("0.0700"));
        }
        save(template);
        return template;
    }

    public SalaryTemplate updateTemplate(SalaryTemplate template) {
        SalaryTemplate existing = getById(template.getId());
        if (existing == null) {
            throw new RuntimeException("薪资模板不存在");
        }
        updateById(template);
        return getById(template.getId());
    }

    public void toggleEnabled(Long id) {
        SalaryTemplate template = getById(id);
        if (template == null) {
            throw new RuntimeException("薪资模板不存在");
        }
        template.setEnabled(template.getEnabled() == 1 ? 0 : 1);
        updateById(template);
    }
}
