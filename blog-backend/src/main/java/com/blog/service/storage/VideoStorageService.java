package com.blog.service.storage;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Video storage abstraction — business code depends only on this interface.
 * Dev:  LocalVideoStorageService (local filesystem, zero dependencies)
 * Prod: MinioVideoStorageService (MinIO SDK, add minio dependency + config)
 */
public interface VideoStorageService {

    /** Upload a complete video file. Returns objectKey (relative path). */
    String upload(InputStream data, String filename, String contentType);

    /** Init multipart upload. Returns storage-native uploadId. */
    String initMultipartUpload(String objectKey, String contentType);

    /** Upload one part. Returns etag. */
    String uploadPart(String uploadId, String objectKey, int partNumber, InputStream data, long size);

    /** Complete multipart upload — merge all parts. */
    void completeMultipartUpload(String uploadId, String objectKey, Map<Integer, String> parts);

    /** Abort multipart upload — clean up partial parts. */
    void abortMultipartUpload(String uploadId, String objectKey);

    /** List already uploaded part numbers (for resume). */
    List<Integer> listUploadedParts(String uploadId, String objectKey);

    /** Get a publicly-accessible URL for the video. */
    String getPresignedUrl(String objectKey, int expiryMinutes);

    /** Open an InputStream to read the stored file (for SHA256 verification). */
    InputStream getInputStream(String objectKey);

    /** Delete the video file. */
    void delete(String objectKey);
}
