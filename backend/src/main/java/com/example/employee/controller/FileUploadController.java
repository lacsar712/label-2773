package com.example.employee.controller;

import com.example.employee.common.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/upload")
@CrossOrigin(origins = "*")
public class FileUploadController {

    @Value("${file.upload.path:./uploads}")
    private String uploadPath;

    @Value("${file.upload.url-prefix:/uploads}")
    private String urlPrefix;

    private static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024;

    @PostMapping("/image")
    @PreAuthorize("hasAnyRole('ADMIN','HR','EMPLOYEE')")
    public Result<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Result.error("请选择要上传的文件");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            return Result.error("文件名无效");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return Result.error("只支持上传图片文件");
        }

        if (file.getSize() > MAX_IMAGE_SIZE) {
            return Result.error("图片大小不能超过10MB");
        }

        String extension = getFileExtension(originalFilename);
        if (!isValidImageExtension(extension)) {
            return Result.error("不支持的图片格式，仅支持 JPG、PNG、GIF、WebP、BMP");
        }

        try {
            String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            File dir = new File(uploadPath + File.separator + dateDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String newFilename = UUID.randomUUID().toString().replace("-", "") + extension;
            File destFile = new File(dir, newFilename);
            file.transferTo(destFile);

            String fileUrl = urlPrefix + "/" + dateDir + "/" + newFilename;

            Map<String, String> data = new HashMap<>();
            data.put("url", fileUrl);
            data.put("filename", originalFilename);
            data.put("size", String.valueOf(file.getSize()));

            return Result.success(data);
        } catch (IOException e) {
            return Result.error("文件上传失败: " + e.getMessage());
        }
    }

    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex >= 0) {
            return filename.substring(lastDotIndex).toLowerCase();
        }
        return "";
    }

    private boolean isValidImageExtension(String extension) {
        String[] validExtensions = {".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp"};
        for (String validExt : validExtensions) {
            if (validExt.equals(extension)) {
                return true;
            }
        }
        return false;
    }
}
