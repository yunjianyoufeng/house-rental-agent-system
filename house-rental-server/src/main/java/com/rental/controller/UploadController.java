package com.rental.controller;

import com.rental.common.RequestUserUtil;
import com.rental.common.Result;
import com.rental.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping
public class UploadController {

    private static final long MAX_IMAGE_SIZE = 5L * 1024 * 1024;
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    private static final Set<String> IMAGE_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".gif", ".webp");
    private static final Set<String> FILE_EXTENSIONS = Set.of(".pdf", ".doc", ".docx", ".jpg", ".jpeg", ".png");

    @Value("${app.upload.base-dir:./uploads}")
    private String uploadBaseDir;

    @PostMapping(value = "/landlord/upload/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        RequestUserUtil.checkLandlordRole(request);
        return Result.success(store(file, "images", IMAGE_EXTENSIONS, MAX_IMAGE_SIZE));
    }

    @PostMapping(value = {"/landlord/upload/contract", "/admin/upload/contract"}, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<String> uploadContract(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        String role = RequestUserUtil.getCurrentRole(request);
        if (!"LANDLORD".equals(role) && !"ADMIN".equals(role)) {
            throw new BusinessException("当前角色不允许上传合同附件");
        }
        return Result.success(store(file, "contracts", FILE_EXTENSIONS, MAX_FILE_SIZE));
    }

    private String store(MultipartFile file, String folder, Set<String> allowedExtensions, long maxSize) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的文件");
        }
        if (file.getSize() > maxSize) {
            throw new BusinessException("上传文件大小超出限制");
        }

        String originalName = file.getOriginalFilename();
        String extension = getExtension(originalName);
        if (!allowedExtensions.contains(extension)) {
            throw new BusinessException("文件类型不支持");
        }

        String dateFolder = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        Path targetDir = Paths.get(uploadBaseDir, folder, dateFolder).toAbsolutePath().normalize();

        try {
            Files.createDirectories(targetDir);
            String filename = UUID.randomUUID().toString().replace("-", "") + extension;
            Path targetFile = targetDir.resolve(filename);
            Files.copy(file.getInputStream(), targetFile, StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/" + folder + "/" + dateFolder + "/" + filename;
        } catch (IOException e) {
            throw new BusinessException("文件上传失败");
        }
    }

    private String getExtension(String filename) {
        if (!StringUtils.hasText(filename) || !filename.contains(".")) {
            throw new BusinessException("文件名非法");
        }
        return filename.substring(filename.lastIndexOf('.')).toLowerCase(Locale.ROOT);
    }
}
