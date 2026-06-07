package com.example.employee.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.employee.dto.CaptchaResponseDTO;
import com.example.employee.entity.SysCaptcha;
import com.example.employee.mapper.SysCaptchaMapper;
import com.google.code.kaptcha.impl.DefaultKaptcha;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

@Service
public class CaptchaService {

    @Resource
    private DefaultKaptcha captchaProducer;

    @Autowired
    private SysCaptchaMapper sysCaptchaMapper;

    public CaptchaResponseDTO generateCaptcha() {
        String capText = captchaProducer.createText();
        String uuid = UUID.randomUUID().toString().replace("-", "");

        SysCaptcha captcha = new SysCaptcha();
        captcha.setUuid(uuid);
        captcha.setCode(capText);
        captcha.setExpireTime(LocalDateTime.now().plusMinutes(5));
        captcha.setCreateTime(LocalDateTime.now());
        sysCaptchaMapper.insert(captcha);

        BufferedImage bi = captchaProducer.createImage(capText);
        String imgBase64 = null;
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(bi, "jpg", out);
            imgBase64 = "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (Exception ignored) {
        }

        CaptchaResponseDTO dto = new CaptchaResponseDTO();
        dto.setUuid(uuid);
        dto.setImg(imgBase64);
        return dto;
    }

    public boolean validateCaptcha(String uuid, String code) {
        if (uuid == null || code == null) {
            return false;
        }
        SysCaptcha captcha = sysCaptchaMapper.selectOne(
                new LambdaQueryWrapper<SysCaptcha>().eq(SysCaptcha::getUuid, uuid)
        );
        if (captcha == null) {
            return false;
        }
        sysCaptchaMapper.deleteById(captcha.getId());
        if (captcha.getExpireTime().isBefore(LocalDateTime.now())) {
            return false;
        }
        return code.equalsIgnoreCase(captcha.getCode());
    }
}
