package com.example.ffmpeg.controller;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.ffmpeg.dto.VideoRecord;

@RestController
@RequestMapping("/video")
public class VideoController {

    private static final String FILE_SEPARATOR = System.getProperty("file.separator");

    @Value("${app.video-folder}")
    private String videoFolder;

    @GetMapping("/getVideoList")
    public List<VideoRecord> getVideoList() throws IOException {

        Path videoPath = Paths.get(videoFolder);
        List<String> listDate = findDir(videoPath);

        List<VideoRecord> listVideoRecord = new ArrayList<>();
        listDate.forEach(date -> {
            Path videoList = Paths.get(videoFolder + FILE_SEPARATOR + date);
            VideoRecord videoRecord = new VideoRecord();
            videoRecord.setDate(date);
            videoRecord.setList(findDir(videoList));
            listVideoRecord.add(videoRecord);
        });
        return listVideoRecord;
    }

    public static List<String> findDir(Path dir) {
        List<String> list = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path e : stream) {
                if (Files.isDirectory(e)) {
                    list.add(e.getFileName().toString());
                }
            }
        } catch (IOException e) {
        }
        return list;
    }

}
