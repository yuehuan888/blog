package com.blog.service.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Local filesystem video storage — development default.
 * Videos stored under {uploadDir}/videos/.
 * Parts stored as {objectKey}.part{N}, merged on complete.
 */
@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalVideoStorageService implements VideoStorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalVideoStorageService.class);

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    private Path videoDir() {
        Path dir = Paths.get(uploadDir, "videos");
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create video directory: " + dir, e);
        }
        return dir;
    }

    private Path resolve(String objectKey) {
        return videoDir().resolve(objectKey);
    }

    // ====== Helpers for objectKeys that may contain subdirectories ======

    /** Resolve the directory where a given objectKey lives. */
    private Path partDir(String objectKey) {
        Path target = resolve(objectKey);
        Path parent = target.getParent();
        return parent != null ? parent : videoDir();
    }

    /** Extract just the filename portion of an objectKey. */
    private String partFilename(String objectKey) {
        return resolve(objectKey).getFileName().toString();
    }

    // ====== Interface Methods ======

    @Override
    public String upload(InputStream data, String filename, String contentType) {
        String ext = "";
        int dot = filename.lastIndexOf('.');
        if (dot > 0) ext = filename.substring(dot);
        String objectKey = UUID.randomUUID().toString().substring(0, 8) + ext;

        Path target = resolve(objectKey);
        try {
            Files.createDirectories(target.getParent());
            Files.copy(data, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save video: " + objectKey, e);
        }
        log.info("Video uploaded: {} ({} bytes)", objectKey, target.toFile().length());
        return objectKey;
    }

    @Override
    public String initMultipartUpload(String objectKey, String contentType) {
        // For local storage, uploadId is just a UUID
        String uploadId = UUID.randomUUID().toString().substring(0, 8);
        log.info("Multipart upload init: objectKey={}, uploadId={}", objectKey, uploadId);
        return uploadId;
    }

    @Override
    public String uploadPart(String uploadId, String objectKey, int partNumber, InputStream data, long size) {
        Path partPath = partDir(objectKey).resolve(partFilename(objectKey) + ".part" + partNumber);
        try {
            Files.createDirectories(partPath.getParent());
            Files.copy(data, partPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save part " + partNumber + " for " + objectKey, e);
        }
        String etag = objectKey + ".part" + partNumber + ":" + size;
        log.debug("Part {} uploaded: {}", partNumber, etag);
        return etag;
    }

    @Override
    public void completeMultipartUpload(String uploadId, String objectKey, Map<Integer, String> parts) {
        Path target = resolve(objectKey);
        try {
            Files.createDirectories(target.getParent());
        } catch (IOException e) {
            throw new RuntimeException("Failed to create directory for " + objectKey, e);
        }
        try (FileOutputStream fos = new FileOutputStream(target.toFile());
             BufferedOutputStream bos = new BufferedOutputStream(fos)) {

            List<Integer> sortedParts = new ArrayList<>(parts.keySet());
            Collections.sort(sortedParts);

            for (int partNumber : sortedParts) {
                Path partPath = partDir(objectKey).resolve(partFilename(objectKey) + ".part" + partNumber);
                if (!Files.exists(partPath)) {
                    throw new RuntimeException("Missing part " + partNumber + " for " + objectKey);
                }
                Files.copy(partPath, bos);
                // Delete part after merging
                Files.deleteIfExists(partPath);
            }
            bos.flush();
        } catch (IOException e) {
            throw new RuntimeException("Failed to merge parts for " + objectKey, e);
        }
        log.info("Multipart upload complete: objectKey={}, parts={}", objectKey, parts.size());
    }

    @Override
    public void abortMultipartUpload(String uploadId, String objectKey) {
        // Clean up all .part files in the objectKey's directory
        Path dir = partDir(objectKey);
        String fnPrefix = partFilename(objectKey) + ".part";
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, fnPrefix + "*")) {
            for (Path partPath : stream) {
                Files.deleteIfExists(partPath);
                log.debug("Deleted partial part: {}", partPath.getFileName());
            }
        } catch (IOException e) {
            log.warn("Failed to clean up parts for {}", objectKey, e);
        }
    }

    @Override
    public List<Integer> listUploadedParts(String uploadId, String objectKey) {
        List<Integer> parts = new ArrayList<>();
        Path dir = partDir(objectKey);
        String fnPrefix = partFilename(objectKey) + ".part";
        try {
            Files.createDirectories(dir);
        } catch (IOException ignored) {}
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, fnPrefix + "*")) {
            for (Path partPath : stream) {
                String filename = partPath.getFileName().toString();
                String numStr = filename.substring(fnPrefix.length());
                try {
                    parts.add(Integer.parseInt(numStr));
                } catch (NumberFormatException ignored) {
                }
            }
        } catch (IOException e) {
            log.warn("Failed to list parts for {}", objectKey, e);
        }
        Collections.sort(parts);
        return parts;
    }

    @Override
    public String getPresignedUrl(String objectKey, int expiryMinutes) {
        // Local storage — return HTTP path directly (no signing)
        return "/uploads/videos/" + objectKey;
    }

    @Override
    public InputStream getInputStream(String objectKey) {
        try {
            return new FileInputStream(resolve(objectKey).toFile());
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Video not found: " + objectKey, e);
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            Files.deleteIfExists(resolve(objectKey));
            log.info("Video deleted: {}", objectKey);
        } catch (IOException e) {
            log.warn("Failed to delete video: {}", objectKey, e);
        }
    }
}
