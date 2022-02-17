package com.example.ffmpeg.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.ffmpeg.utils.FFmpegUtils;

@RestController
@RequestMapping("/upload")
public class UploadController {

    private static final Logger log = LoggerFactory.getLogger(UploadController.class);

    private static final Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"));

    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Value("${app.video-folder}")
    private String videoFolder;

    /**
     * 
     * @param video
     * @param transcodeConfig
     * @return
     * @throws IOException
     */
    @PostMapping
    public Object upload(@RequestPart(name = "file", required = true) MultipartFile video) throws IOException {

        log.info("影片內容：title={}, size={}", video.getOriginalFilename(), video.getSize());

        String title = video.getOriginalFilename();

        // 複製到臨時目錄
        Path tempFile = tempDir.resolve(title);
        log.info("tmpdir : {}", tempFile);

        try {

            video.transferTo(tempFile);

            if (title == null) {
                throw new RuntimeException("OriginalFilename should not be null.");
            }

            title = title.substring(0, title.lastIndexOf("."));

            String today = dtf.format(LocalDate.now());

            Path videoPath = Paths.get(videoFolder, today, title);
            if (videoPath.toFile().exists()) {
                // 若有檔案則清空
                deleteDir(videoPath);
            }
            Path targetFolder = Files.createDirectories(videoPath);

            log.info("root path ：{}", targetFolder);
            Files.createDirectories(targetFolder);

            log.info("start to turn video to M3u8 ts file");
            try {
                FFmpegUtils.transcodeToM3u8(tempFile.toString(), targetFolder.toAbsolutePath().toString());
            } catch (Exception e) {
                log.error("error. ", e);
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("message", e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "success");
            return result;
        } finally {
            Files.delete(tempFile);
        }
    }

    public static void deleteDir(Path pathToBeDeleted) {
        try (Stream<Path> walk = Files.walk(pathToBeDeleted)) {
            walk.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        } catch (IOException e) {
            log.error("error.", e);
        }
    }
}
