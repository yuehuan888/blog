package com.blog.controller;

import com.blog.dto.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class UploadController {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @PostMapping("/upload/avatar")
    public Result<Map<String, String>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.fail(400, "File is empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return Result.fail(400, "Only image files are allowed");
        }

        if (file.getSize() > 2 * 1024 * 1024) {
            return Result.fail(400, "File size must be less than 2MB");
        }

        try {
            Path avatarDir = Paths.get(uploadDir, "avatars");
            Files.createDirectories(avatarDir);

            String originalName = file.getOriginalFilename();
            String ext = "";
            if (originalName != null && originalName.contains(".")) {
                ext = originalName.substring(originalName.lastIndexOf("."));
            }
            String filename = "avatar_" + UUID.randomUUID().toString().substring(0, 8) + ext;

            Path filePath = avatarDir.resolve(filename);
            file.transferTo(filePath.toFile());

            String url = "/uploads/avatars/" + filename;
            return Result.ok(Map.of("url", url));
        } catch (IOException e) {
            return Result.fail(500, "Failed to save file: " + e.getMessage());
        }
    }

    @PostMapping("/upload/article-image")
    public Result<Map<String, String>> uploadArticleImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.fail(400, "File is empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return Result.fail(400, "Only image files are allowed");
        }

        if (file.getSize() > 5 * 1024 * 1024) {
            return Result.fail(400, "File size must be less than 5MB");
        }

        try {
            Path articleDir = Paths.get(uploadDir, "articles");
            Files.createDirectories(articleDir);

            String originalName = file.getOriginalFilename();
            String ext = "";
            if (originalName != null && originalName.contains(".")) {
                ext = originalName.substring(originalName.lastIndexOf("."));
            }
            String filename = "article_" + UUID.randomUUID().toString().substring(0, 8) + ext;

            Path filePath = articleDir.resolve(filename);
            file.transferTo(filePath.toFile());

            String url = "/uploads/articles/" + filename;
            return Result.ok(Map.of("url", url));
        } catch (IOException e) {
            return Result.fail(500, "Failed to save file: " + e.getMessage());
        }
    }
}
