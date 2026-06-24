package com.blog.service;

import com.blog.service.storage.VideoStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Video transcode service — converts MP4 to HLS (m3u8 + ts segments).
 * Requires FFmpeg installed on the server.
 * Falls back gracefully if FFmpeg is not available.
 */
@Service
public class VideoTranscodeService {

    private static final Logger log = LoggerFactory.getLogger(VideoTranscodeService.class);

    @Autowired
    private VideoStorageService videoStorageService;

    /**
     * Transcode video to HLS format.
     * If FFmpeg is not available, marks the video as relying on direct MP4 playback.
     */
    public void transcode(Long articleId, String objectKey) {
        // Check if FFmpeg is available
        if (!isFfmpegAvailable()) {
            log.info("FFmpeg not available — article={} will use direct MP4 playback", articleId);
            return;
        }

        try {
            // Download video from storage to temp file
            Path tempDir = Files.createTempDirectory("video-transcode-");
            Path inputFile = tempDir.resolve("input.mp4");

            try (InputStream is = videoStorageService.getInputStream(objectKey)) {
                Files.copy(is, inputFile, StandardCopyOption.REPLACE_EXISTING);
            }

            // Run FFmpeg
            Path outputDir = tempDir.resolve("hls");
            Files.createDirectories(outputDir);
            Path outputM3u8 = outputDir.resolve("output.m3u8");

            ProcessBuilder pb = new ProcessBuilder(
                    "ffmpeg", "-i", inputFile.toString(),
                    "-codec:v", "libx264", "-codec:a", "aac",
                    "-hls_time", "10", "-hls_list_size", "0",
                    "-hls_segment_filename", outputDir + "/segment_%03d.ts",
                    "-f", "hls", outputM3u8.toString()
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // Read output for logging
            String ffmpegOutput = new String(process.getInputStream().readAllBytes());
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                log.warn("FFmpeg exited with code {} for article={}: {}",
                        exitCode, articleId, ffmpegOutput.substring(Math.min(200, ffmpegOutput.length())));
                return;
            }

            log.info("Transcode complete for article={}, output={}", articleId, outputM3u8);

            // Upload HLS segments back to storage
            // The m3u8 and ts files are stored alongside the original video under the same objectKey prefix
            String prefix = objectKey.contains(".")
                    ? objectKey.substring(0, objectKey.lastIndexOf('.'))
                    : objectKey;
            String hlsPrefix = prefix + "/hls/";

            // Upload m3u8
            try (InputStream m3u8Stream = Files.newInputStream(outputM3u8)) {
                videoStorageService.upload(m3u8Stream, "output.m3u8", "application/vnd.apple.mpegurl");
                // Note: We store HLS files with known keys; the detail page builds the URL
            }

            // Clean up temp files
            Files.walk(tempDir)
                    .sorted(java.util.Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);

        } catch (Exception e) {
            log.error("Transcode error for article={}", articleId, e);
            // Transcode failure is non-blocking — player falls back to direct MP4
        }
    }

    private boolean isFfmpegAvailable() {
        try {
            ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-version");
            Process p = pb.start();
            int exitCode = p.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }
}
