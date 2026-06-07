package com.example.employee.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.*;

@Slf4j
public class AuditLogUtil {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.findAndRegisterModules();
    }

    public static String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.error("对象序列化失败", e);
            return null;
        }
    }

    public static Map<String, Object> fromJson(String json) {
        if (json == null || json.isEmpty()) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, Object>>() {});
        } catch (Exception e) {
            log.error("JSON反序列化失败", e);
            return new LinkedHashMap<>();
        }
    }

    public static List<Map<String, Object>> compareJson(String beforeJson, String afterJson) {
        Map<String, Object> beforeMap = fromJson(beforeJson);
        Map<String, Object> afterMap = fromJson(afterJson);
        return compareMaps(beforeMap, afterMap, "");
    }

    private static List<Map<String, Object>> compareMaps(Map<String, Object> before, Map<String, Object> after, String parentPath) {
        List<Map<String, Object>> diffs = new ArrayList<>();
        Set<String> allKeys = new LinkedHashSet<>();
        allKeys.addAll(before.keySet());
        allKeys.addAll(after.keySet());

        for (String key : allKeys) {
            String fullPath = parentPath.isEmpty() ? key : parentPath + "." + key;
            Object oldValue = before.get(key);
            Object newValue = after.get(key);

            if (!before.containsKey(key)) {
                diffs.add(buildDiff(fullPath, null, newValue, "ADD"));
            } else if (!after.containsKey(key)) {
                diffs.add(buildDiff(fullPath, oldValue, null, "REMOVE"));
            } else if (oldValue == null && newValue == null) {
                continue;
            } else if (oldValue == null || newValue == null) {
                diffs.add(buildDiff(fullPath, oldValue, newValue, "MODIFY"));
            } else if (oldValue instanceof Map && newValue instanceof Map) {
                diffs.addAll(compareMaps(
                        (Map<String, Object>) oldValue,
                        (Map<String, Object>) newValue,
                        fullPath
                ));
            } else if (!Objects.equals(oldValue, newValue)) {
                diffs.add(buildDiff(fullPath, oldValue, newValue, "MODIFY"));
            }
        }
        return diffs;
    }

    private static Map<String, Object> buildDiff(String fieldName, Object oldValue, Object newValue, String diffType) {
        Map<String, Object> diff = new LinkedHashMap<>();
        diff.put("fieldName", fieldName);
        diff.put("fieldLabel", fieldName);
        diff.put("oldValue", oldValue);
        diff.put("newValue", newValue);
        diff.put("diffType", diffType);
        return diff;
    }

    public static Object getFieldValue(Object obj, String fieldName) {
        if (obj == null || fieldName == null) {
            return null;
        }
        try {
            Field field = findField(obj.getClass(), fieldName);
            if (field != null) {
                field.setAccessible(true);
                return field.get(obj);
            }
        } catch (Exception e) {
            log.error("获取字段值失败: {}", fieldName, e);
        }
        return null;
    }

    private static Field findField(Class<?> clazz, String fieldName) {
        while (clazz != null && clazz != Object.class) {
            try {
                return clazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }

    public static String convertToFieldLabel(String fieldName) {
        if (fieldName == null || fieldName.isEmpty()) {
            return fieldName;
        }
        Map<String, String> labelMap = new HashMap<>();
        labelMap.put("id", "ID");
        labelMap.put("name", "名称");
        labelMap.put("code", "编码");
        labelMap.put("email", "邮箱");
        labelMap.put("departmentId", "部门ID");
        labelMap.put("departmentName", "部门名称");
        labelMap.put("role", "职位");
        labelMap.put("hireDate", "入职日期");
        labelMap.put("leaveDate", "离职日期");
        labelMap.put("contractEndDate", "合同到期日期");
        labelMap.put("status", "状态");
        labelMap.put("description", "描述");
        labelMap.put("leader", "负责人");
        labelMap.put("parentId", "上级部门ID");
        labelMap.put("headcountLimit", "编制人数上限");
        labelMap.put("enabled", "启用状态");

        for (Map.Entry<String, String> entry : labelMap.entrySet()) {
            if (fieldName.endsWith(entry.getKey())) {
                return fieldName.substring(0, fieldName.length() - entry.getKey().length()) + entry.getValue();
            }
        }
        return fieldName;
    }
}
