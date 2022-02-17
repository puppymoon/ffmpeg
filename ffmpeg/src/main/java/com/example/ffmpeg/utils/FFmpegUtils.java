package com.example.ffmpeg.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.KeyGenerator;

import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.ffmpeg.dto.MediaInfo;
import com.google.gson.Gson;

public class FFmpegUtils {

    private static final Logger log = LoggerFactory.getLogger(FFmpegUtils.class);

    private static final String FILE_SEPARATOR = System.getProperty("file.separator");

    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    private FFmpegUtils() {

    }

    private static byte[] genAesKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(128);
            return keyGenerator.generateKey().getEncoded();
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    private static Path genKeyInfo(String folder) throws IOException {

        byte[] aesKey = genAesKey();
        String iv = Hex.encodeHexString(genAesKey());
        Path keyFile = Paths.get(folder, "key");
        Files.write(keyFile, aesKey, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("key").append(LINE_SEPARATOR);
        stringBuilder.append(keyFile.toString()).append(LINE_SEPARATOR);
        stringBuilder.append(iv);

        Path keyInfo = Paths.get(folder, "key_info");

        Files.write(keyInfo, stringBuilder.toString().getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        return keyInfo;
    }

    public static void transcodeToM3u8(String source, String destFolder) throws IOException, InterruptedException {

        if (!Files.exists(Paths.get(source))) {
            throw new IllegalArgumentException("file is not exist：" + source);
        }

        Path workDir = Paths.get(destFolder, "ts");
        Files.createDirectories(workDir);

        Path keyInfo = genKeyInfo(workDir.toString());

        // 轉換影片
        if (copyVideoToTs(source, destFolder, keyInfo.toString(), workDir.toFile())) {
            List<String> errorStringList = Files.readAllLines(Paths.get(destFolder + FILE_SEPARATOR + "VideoError.txt"));
            for (String errorlog : errorStringList) {
                log.error(errorlog);
            }
        }
        // 擷取封面
        if (screenShots(source, destFolder, workDir.toFile())) {
            List<String> errorStringList = Files.readAllLines(Paths.get(destFolder + FILE_SEPARATOR + "CoverError.txt"));
            for (String errorlog : errorStringList) {
                log.error(errorlog);
            }
        }

        // 讀取影片資訊
        MediaInfo mediaInfo = getMediaInfo(source);
        if (mediaInfo == null) {
            throw new RuntimeException("getMediaInfo fail.");
        }

        // 生成index.m3u8文件
        genIndex(destFolder, "ts/index.m3u8", mediaInfo.getFormat().getBitRate());

    }

    public static boolean copyVideoToTs(String source, String destFolder, String keyInfo, File workDir)
            throws IOException, InterruptedException {

        List<String> commands = new ArrayList<>();
        // cmd 名稱
        commands.add("ffmpeg");
        // 檔案來源
        commands.add("-i");
        commands.add(source);
        // 複製影片並將設定編碼且直接複製聲音
        commands.add("-c:v");
        commands.add("libx264");
        commands.add("-c:a");
        commands.add("copy");
        // 指定key的路徑
        commands.add("-hls_key_info_file");
        commands.add(keyInfo);
        // 指定每個ts的秒數與清單的數量
        commands.add("-hls_time");
        commands.add("4");
        commands.add("-hls_list_size");
        commands.add("5");
        commands.add("-hls_playlist_type");
        commands.add("vod");
        commands.add("-hls_segment_filename");
        commands.add("%06d.ts");
        commands.add("index.m3u8");

        Process process = new ProcessBuilder(commands).directory(workDir)
                .redirectOutput(new File(destFolder + FILE_SEPARATOR + "VideoOutput.txt"))
                .redirectError(new File(destFolder + FILE_SEPARATOR + "VideoError.txt")).start();

        return process.waitFor() != 0;
    }

    public static boolean screenShots(String source, String destFolder, File workDir) throws IOException, InterruptedException {

        List<String> commands = new ArrayList<>();
        commands.add("ffmpeg");
        commands.add("-i");
        commands.add(source);
        commands.add("-ss");
        commands.add("00:00:00.001");
        commands.add("-y");
        commands.add("-q:v");
        commands.add("1");
        commands.add("-frames:v");
        commands.add("1");
        commands.add("-f");
        commands.add("image2");
        commands.add(destFolder + FILE_SEPARATOR + "poster.jpg");

        Process process = new ProcessBuilder(commands).directory(workDir)
                .redirectOutput(new File(destFolder + FILE_SEPARATOR + "CoverOutput.txt"))
                .redirectError(new File(destFolder + FILE_SEPARATOR + "CoverError.txt")).start();
        
        return process.waitFor() != 0;
    }

    public static MediaInfo getMediaInfo(String source) throws IOException, InterruptedException {
        List<String> commands = new ArrayList<>();
        commands.add("ffprobe");
        commands.add("-i");
        commands.add(source);
        commands.add("-show_format");
        commands.add("-show_streams");
        commands.add("-print_format");
        commands.add("json");

        Process process = new ProcessBuilder(commands).start();

        MediaInfo mediaInfo = null;

        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            mediaInfo = new Gson().fromJson(bufferedReader, MediaInfo.class);
        } catch (IOException e) {
            log.error("error. ", e);
        }

        if (process.waitFor() != 0) {
            return null;
        }

        return mediaInfo;
    }

    private static void genIndex(String destFolder, String indexPath, String bandWidth) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("#EXTM3U").append(LINE_SEPARATOR);
        stringBuilder.append("#EXT-X-STREAM-INF:BANDWIDTH=" + bandWidth).append(LINE_SEPARATOR);
        stringBuilder.append(indexPath);
        Files.write(Paths.get(destFolder + FILE_SEPARATOR + "index.m3u8"), stringBuilder.toString().getBytes(StandardCharsets.UTF_8),
            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }
}
