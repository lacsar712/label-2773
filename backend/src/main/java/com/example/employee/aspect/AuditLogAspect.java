package com.example.employee.aspect;

import com.example.employee.annotation.AuditLog;
import com.example.employee.context.UserContext;
import com.example.employee.dto.UserInfoDTO;
import com.example.employee.entity.SysAuditLog;
import com.example.employee.service.SysAuditLogService;
import com.example.employee.utils.AuditLogUtil;
import com.example.employee.utils.IpUtil;
import com.example.employee.utils.UserAgentUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.time.LocalDateTime;

@Slf4j
@Aspect
@Component
public class AuditLogAspect {

    @Autowired
    private SysAuditLogService sysAuditLogService;

    @Around("@annotation(auditLogAnnotation)")
    public Object around(ProceedingJoinPoint joinPoint, AuditLog auditLogAnnotation) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Parameter[] parameters = method.getParameters();
        Object[] args = joinPoint.getArgs();

        String targetRecordId = extractRecordId(parameters, args, auditLogAnnotation.recordIdParam());
        String recordNameField = auditLogAnnotation.recordNameField();

        Object beforeSnapshot = null;
        if (auditLogAnnotation.operation().getCode() == 2 || auditLogAnnotation.operation().getCode() == 3) {
            beforeSnapshot = getTargetEntity(parameters, args, targetRecordId);
        }

        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Throwable throwable) {
            throw throwable;
        }

        try {
            SysAuditLog auditLogEntity = new SysAuditLog();

            UserInfoDTO currentUser = UserContext.getCurrentUser();
            if (currentUser != null) {
                auditLogEntity.setOperatorId(currentUser.getUserId());
                auditLogEntity.setOperatorName(currentUser.getNickname());
                auditLogEntity.setOperatorUsername(currentUser.getUsername());
                auditLogEntity.setOperatorRoleId(currentUser.getRoleId());
                auditLogEntity.setOperatorRoleCode(currentUser.getRoleCode());
                auditLogEntity.setOperatorRoleName(currentUser.getRoleName());
            }

            auditLogEntity.setOperationType(auditLogAnnotation.operation().getCode());
            auditLogEntity.setTargetModule(auditLogAnnotation.module().getCode());
            auditLogEntity.setTargetModuleName(auditLogAnnotation.module().getDesc());

            if (targetRecordId != null) {
                auditLogEntity.setTargetRecordId(targetRecordId);
            } else if (result != null) {
                Object extractedId = AuditLogUtil.getFieldValue(result, "id");
                if (extractedId != null) {
                    auditLogEntity.setTargetRecordId(extractedId.toString());
                }
            }

            Object targetNameValue = null;
            for (Object arg : args) {
                if (arg != null && !isSimpleType(arg.getClass())) {
                    Object nameVal = AuditLogUtil.getFieldValue(arg, recordNameField);
                    if (nameVal != null) {
                        targetNameValue = nameVal;
                        break;
                    }
                }
            }
            if (targetNameValue == null && result != null && !isSimpleType(result.getClass())) {
                targetNameValue = AuditLogUtil.getFieldValue(result, recordNameField);
            }
            if (targetNameValue != null) {
                auditLogEntity.setTargetRecordName(targetNameValue.toString());
            }

            Object afterSnapshot = null;
            if (auditLogAnnotation.operation().getCode() == 1 || auditLogAnnotation.operation().getCode() == 2) {
                afterSnapshot = getTargetEntity(parameters, args, targetRecordId);
                if (afterSnapshot == null && result != null && !isSimpleType(result.getClass())) {
                    afterSnapshot = result;
                }
            }

            auditLogEntity.setBeforeSnapshot(AuditLogUtil.toJson(beforeSnapshot));
            auditLogEntity.setAfterSnapshot(AuditLogUtil.toJson(afterSnapshot));

            fillRequestInfo(auditLogEntity);

            auditLogEntity.setOperationTime(LocalDateTime.now());
            auditLogEntity.setArchived(false);

            sysAuditLogService.saveLog(auditLogEntity);
        } catch (Exception e) {
            log.error("记录审计日志失败", e);
        }

        return result;
    }

    private String extractRecordId(Parameter[] parameters, Object[] args, String recordIdParam) {
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].getName().equals(recordIdParam) && args[i] != null) {
                return args[i].toString();
            }
            Object arg = args[i];
            if (arg != null && !isSimpleType(arg.getClass())) {
                Object idVal = AuditLogUtil.getFieldValue(arg, "id");
                if (idVal != null) {
                    return idVal.toString();
                }
            }
        }
        return null;
    }

    private Object getTargetEntity(Parameter[] parameters, Object[] args, String recordId) {
        for (Object arg : args) {
            if (arg != null && !isSimpleType(arg.getClass())) {
                return arg;
            }
        }
        return null;
    }

    private void fillRequestInfo(SysAuditLog auditLog) {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                auditLog.setRequestIp(IpUtil.getIpAddr(request));
                String ua = request.getHeader("User-Agent");
                auditLog.setUserAgent(ua);
                auditLog.setBrowser(UserAgentUtil.getBrowser(request));
                auditLog.setOs(UserAgentUtil.getOS(request));
                auditLog.setDevice(UserAgentUtil.getDevice(request));
            }
        } catch (Exception e) {
            log.error("获取请求信息失败", e);
        }
    }

    private boolean isSimpleType(Class<?> clazz) {
        return clazz.isPrimitive()
                || clazz == String.class
                || Number.class.isAssignableFrom(clazz)
                || Boolean.class == clazz
                || Character.class == clazz
                || clazz.isEnum();
    }
}
