package com.blog.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * AI video analysis via Alibaba DashScope Qwen (multimodal video understanding).
 *
 * Strategy:
 *   - Use OpenAI-compatible endpoint with qwen3.7-plus (video-capable model)
 *   - If video URL is publicly accessible → pass directly to multimodal API
 *   - If video is local → upload to DashScope Files, then reference via fileid://
 *   - Fall back to text-only summary if video analysis fails
 */
@Service
public class DashScopeService {

    private static final Logger log = LoggerFactory.getLogger(DashScopeService.class);

    private static final String OPENAI_API_URL =
            "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";
    private static final String DASHSCOPE_FILES_URL =
            "https://dashscope.aliyuncs.com/api/v1/files";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.ai.dashscope.api-key:}")
    private String apiKey;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    /**
     * Analyze a video file using Qwen multimodal model.
     *
     * @param videoUrl    relative URL like "/uploads/videos/hash/uuid.mp4" or full HTTP URL
     * @param title       video title for fallback
     * @param description video description for fallback
     * @param category    video category for fallback
     * @return AI-generated summary, or null on failure
     */
    public String analyzeVideo(String videoUrl, String title, String description, String category) {
        if (apiKey == null || apiKey.isBlank()) {
            log.info("DashScope API key not configured — skipping AI analysis");
            return null;
        }

        // Determine the best video reference for DashScope
        String dashscopeVideoRef = resolveDashScopeRef(videoUrl);

        if (dashscopeVideoRef != null) {
            try {
                String summary = callVideoUnderstanding(dashscopeVideoRef, title, description, category);
                if (summary != null && !summary.isBlank()) {
                    return summary;
                }
            } catch (Exception e) {
                log.warn("Video understanding failed, falling back to text analysis: {}", e.getMessage());
            }
        }

        // Fallback: text-only summary
        return analyzeVideoByText(title, description, category);
    }

    // ====== Video reference resolution ======

    /**
     * Resolve a video reference that DashScope can access.
     * For local files: upload to DashScope Files and use fileid://
     * For HTTP URLs: use directly
     * Returns null if neither approach works.
     */
    private String resolveDashScopeRef(String videoUrl) {
        if (videoUrl == null) return null;

        // If it's already an HTTP/HTTPS URL, DashScope can try to download it
        if (videoUrl.startsWith("http://") || videoUrl.startsWith("https://")) {
            return videoUrl;
        }

        // Local file: upload to DashScope Files and get fileid:// reference
        Path localPath = resolveLocalPath(videoUrl);
        if (localPath != null && Files.exists(localPath)) {
            try {
                String fileId = uploadToDashScopeFiles(localPath);
                if (fileId != null) {
                    return "fileid://" + fileId;
                }
            } catch (Exception e) {
                log.warn("Failed to upload video to DashScope Files: {}", e.getMessage());
            }
        }

        return null;
    }

    /**
     * Convert a relative video URL to an absolute local path.
     */
    private Path resolveLocalPath(String videoUrl) {
        String prefix = "/uploads/videos/";
        if (videoUrl.startsWith(prefix)) {
            return Paths.get(uploadDir, "videos", videoUrl.substring(prefix.length()));
        }
        // Try as absolute path
        Path direct = Paths.get(videoUrl);
        if (Files.exists(direct)) {
            return direct;
        }
        return null;
    }

    // ====== DashScope Files upload ======

    /**
     * Upload a video file to DashScope's file service.
     * @return file_id on success, null on failure
     */
    private String uploadToDashScopeFiles(Path filePath) {
        try {
            long fileSize = Files.size(filePath);
            log.info("Uploading video to DashScope Files: {} ({} bytes)", filePath.getFileName(), fileSize);

            String boundary = "----DashScopeUpload" + UUID.randomUUID().toString().replace("-", "");
            byte[] fileBytes = Files.readAllBytes(filePath);
            String filename = filePath.getFileName().toString();
            String mimeType = Files.probeContentType(filePath);
            if (mimeType == null) mimeType = "video/mp4";

            ByteArrayOutputStream body = new ByteArrayOutputStream();
            writeMultipartField(body, boundary, "purpose", "multimodal");
            writeMultipartFile(body, boundary, "file", filename, mimeType, fileBytes);
            body.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(DASHSCOPE_FILES_URL))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .timeout(Duration.ofSeconds(120))
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                @SuppressWarnings("unchecked")
                Map<String, Object> result = objectMapper.readValue(response.body(), Map.class);
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) result.get("data");
                if (data != null) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, String>> files = (List<Map<String, String>>) data.get("uploaded_files");
                    if (files != null && !files.isEmpty()) {
                        String fileId = files.get(0).get("file_id");
                        log.info("Video uploaded to DashScope: file_id={}", fileId);
                        return fileId;
                    }
                }
            } else {
                log.warn("DashScope Files API returned {}: {}", response.statusCode(),
                        response.body().substring(0, Math.min(300, response.body().length())));
            }
            return null;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            log.error("Failed to upload video to DashScope Files", e);
            return null;
        }
    }

    // ====== Video understanding via OpenAI-compatible API ======

    /**
     * Call the video-capable multimodal model.
     * Uses the OpenAI-compatible endpoints with qwen3.7-plus (supports Video Understanding).
     */
    private String callVideoUnderstanding(String videoRef, String title, String description, String category) {
        try {
            String prompt = buildVideoPrompt(title, description, category);

            Map<String, Object> requestBody = Map.of(
                    "model", "qwen3.7-plus",
                    "messages", List.of(
                            Map.of("role", "user", "content", List.of(
                                    Map.of("type", "video_url",
                                            "video_url", Map.of("url", videoRef)),
                                    Map.of("type", "text",
                                            "text", prompt)
                            ))
                    )
            );

            String json = objectMapper.writeValueAsString(requestBody);
            log.debug("Video understanding request ({} chars)", json.length());

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OPENAI_API_URL))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(180))
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                @SuppressWarnings("unchecked")
                Map<String, Object> result = objectMapper.readValue(response.body(), Map.class);
                return extractOpenAiContent(result);
            } else {
                log.warn("Video understanding API returned {}: {}", response.statusCode(),
                        response.body().substring(0, Math.min(300, response.body().length())));
                return null;
            }
        } catch (Exception e) {
            log.error("Video understanding API call failed", e);
            return null;
        }
    }

    private String buildVideoPrompt(String title, String description, String category) {
        StringBuilder sb = new StringBuilder();
        sb.append("请仔细观看这个视频，然后完成以下任务：\n\n");
        sb.append("1. 用100-200字总结视频的核心内容\n");
        sb.append("2. 提取3-5个关键词标签\n\n");
        if (title != null && !title.isBlank()) {
            sb.append("参考——视频标题：").append(title).append("\n");
        }
        if (category != null && !category.isBlank()) {
            sb.append("分类：").append(category).append("\n");
        }
        if (description != null && !description.isBlank()) {
            sb.append("上传者描述：").append(description).append("\n");
        }
        sb.append("\n请按以下格式回复：\n");
        sb.append("摘要：<内容>\n");
        sb.append("标签：<标签1, 标签2, 标签3>");
        return sb.toString();
    }

    // ====== Text-only fallback ======

    private String analyzeVideoByText(String title, String description, String category) {
        if (title == null || title.isBlank()) {
            log.info("No title for text fallback, skipping");
            return null;
        }

        String prompt = buildTextPrompt(title, description, category);
        try {
            Map<String, Object> requestBody = Map.of(
                    "model", "qwen-plus",
                    "messages", List.of(
                            Map.of("role", "user", "content", prompt)
                    )
            );

            String json = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OPENAI_API_URL))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                @SuppressWarnings("unchecked")
                Map<String, Object> result = objectMapper.readValue(response.body(), Map.class);
                return extractOpenAiContent(result);
            }
            return null;
        } catch (Exception e) {
            log.error("Text-only AI fallback failed", e);
            return null;
        }
    }

    private String buildTextPrompt(String title, String description, String category) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一个视频内容编辑。请根据以下视频信息，生成一段吸引人的视频摘要（100-200字），");
        sb.append("并提取3-5个关键词标签。用中文回复。\n\n");
        sb.append("视频标题：").append(title != null ? title : "未命名").append("\n");
        if (category != null && !category.isBlank()) {
            sb.append("分类：").append(category).append("\n");
        }
        if (description != null && !description.isBlank()) {
            sb.append("描述：").append(description).append("\n");
        }
        sb.append("\n请按以下格式回复：\n");
        sb.append("摘要：<内容>\n");
        sb.append("标签：<标签1, 标签2, 标签3>");
        return sb.toString();
    }

    // ====== Response parsers ======

    @SuppressWarnings("unchecked")
    private String extractOpenAiContent(Map<String, Object> result) {
        try {
            List<Map<String, Object>> choices = (List<Map<String, Object>>) result.get("choices");
            if (choices == null || choices.isEmpty()) return null;
            Map<String, Object> choice = choices.get(0);
            Map<String, Object> message = (Map<String, Object>) choice.get("message");
            if (message == null) return null;
            String content = (String) message.get("content");
            return content != null ? content.trim() : null;
        } catch (Exception e) {
            log.warn("Failed to parse OpenAI-format response", e);
            return null;
        }
    }

    // ====== Multipart helpers ======

    private void writeMultipartField(ByteArrayOutputStream body, String boundary,
                                      String name, String value) throws IOException {
        body.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        body.write(("Content-Disposition: form-data; name=\"" + name + "\"\r\n").getBytes(StandardCharsets.UTF_8));
        body.write("\r\n".getBytes(StandardCharsets.UTF_8));
        body.write(value.getBytes(StandardCharsets.UTF_8));
        body.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    private void writeMultipartFile(ByteArrayOutputStream body, String boundary,
                                     String name, String filename, String mimeType,
                                     byte[] data) throws IOException {
        body.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        body.write(("Content-Disposition: form-data; name=\"" + name
                + "\"; filename=\"" + filename + "\"\r\n").getBytes(StandardCharsets.UTF_8));
        body.write(("Content-Type: " + mimeType + "\r\n").getBytes(StandardCharsets.UTF_8));
        body.write("\r\n".getBytes(StandardCharsets.UTF_8));
        body.write(data);
        body.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }
}
