package com.maxkb4j.model.custom.model;

import com.maxkb4j.model.base.STTModel;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Locale;

import static com.maxkb4j.model.consts.ModelConstants.*;

@Slf4j
@Data
public abstract class AbsSTTModel implements STTModel {

    /**
     * 采样率解析失败时的默认值，与识别参数构建时的初始值保持一致。
     */
    private static final int DEFAULT_SAMPLE_RATE = 16000;

    /**
     * jaudiotagger 按扩展名选择解析器，给出的扩展名无法识别时按此顺序兜底尝试
     * （各解析器均校验文件头，不会误读格式不符的内容）。
     */
    private static final List<String> SAMPLE_RATE_FALLBACK_EXTENSIONS = List.of(".mp3", ".wav", ".ogg", ".flac", ".m4a");

    /**
     * 读取音频采样率。suffix 可为空、不带点或干脆无法识别（如 "mpeg"、"octet-stream"）：
     * 先按给定扩展名解析，失败后依次尝试常见容器扩展名，仍失败则返回默认采样率，
     * 不再向上抛出解析异常。
     */
    protected int getSampleRate(byte[] audioBytes, String suffix) {
        String extension = toExtension(suffix);
        Integer sampleRate = tryReadSampleRate(audioBytes, extension);
        if (sampleRate != null) {
            return sampleRate;
        }
        for (String fallback : SAMPLE_RATE_FALLBACK_EXTENSIONS) {
            if (fallback.equals(extension)) {
                continue;
            }
            sampleRate = tryReadSampleRate(audioBytes, fallback);
            if (sampleRate != null) {
                log.warn("扩展名 [{}] 无法解析采样率，按 [{}] 解析成功", extension, fallback);
                return sampleRate;
            }
        }
        log.warn("无法解析音频采样率（扩展名: {}），使用默认采样率 {}", extension, DEFAULT_SAMPLE_RATE);
        return DEFAULT_SAMPLE_RATE;
    }

    /**
     * 将 MIME 子类型或文件扩展名归一化为语音识别支持的音频格式
     * （'pcm'、'wav'、'mp3'、'opus'、'speex'、'aac'、'amr'）。
     * 兼容常见非标情况：mpeg/mpga→mp3、x-wav/vnd.wave→wav、webm/ogg→opus（浏览器录音典型编码）、
     * m4a/mp4→aac；无法识别时回退为 mp3。
     */
    protected String normalizeFormat(String suffix) {
        if (suffix == null || suffix.isBlank()) {
            return Value.MP3;
        }
        String s = suffix.toLowerCase(Locale.ROOT).trim();
        // 形如 "...;codecs=opus" 的参数部分优先：codecs 标识最能反映真实编码
        int codecs = s.indexOf("codecs=");
        if (codecs >= 0) {
            String codec = s.substring(codecs + "codecs=".length());
            int end = codec.indexOf(';');
            if (end >= 0) {
                codec = codec.substring(0, end);
            }
            String mapped = mapFormat(codec.trim().replace("\"", ""));
            if (mapped != null) {
                return mapped;
            }
        }
        // 去掉 MIME 前缀与多级扩展名，仅保留最后一段，如 "audio/mpeg"→"mpeg"、".mp3"→"mp3"
        int slash = s.lastIndexOf('/');
        if (slash >= 0) {
            s = s.substring(slash + 1).trim();
        }
        int dot = s.lastIndexOf('.');
        if (dot >= 0) {
            s = s.substring(dot + 1).trim();
        }
        String mapped = mapFormat(s);
        return mapped != null ? mapped : Value.MP3;
    }

    private String mapFormat(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        if (token.startsWith("mp4a")) {
            // AAC 编码标识形如 mp4a.40.2
            return Format.AAC;
        }
        return switch (token) {
            case "mp3", "mpeg", "mpga" -> Format.MP3;
            case "wav", "x-wav", "wave", "vnd.wave" -> Format.WAV;
            case "pcm" -> Format.PCM;
            case "opus", "webm", "ogg" -> Format.OPUS;
            case "aac", "m4a", "x-m4a", "mp4" -> Format.AAC;
            case "amr" -> Format.AMR;
            case "speex" -> Format.SPEEX;
            default -> null;
        };
    }

    private String toExtension(String suffix) {
        if (suffix == null || suffix.isBlank()) {
            return "";
        }
        String s = suffix.trim().toLowerCase(Locale.ROOT);
        return s.startsWith(".") ? s : FileToken.DOT + s;
    }

    private Integer tryReadSampleRate(byte[] audioBytes, String extension) {
        Path tempFile = null;
        try {
            tempFile = Files.createTempFile(FileToken.AUDIO_TEMP_PREFIX, extension);
            Files.write(tempFile, audioBytes, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
            AudioFile audioFile = AudioFileIO.read(tempFile.toFile());
            int sampleRate = audioFile.getAudioHeader().getSampleRateAsNumber();
            return sampleRate > 0 ? sampleRate : null;
        } catch (Exception e) {
            // 扩展名无对应解析器、字节内容与扩展名不符或系统临时文件创建失败，
            // 记录后返回 null，由调用方尝试其他扩展名
            log.debug("按扩展名 [{}] 解析采样率失败: {}", extension, e.getMessage());
            return null;
        } finally {
            try {
                if (tempFile != null && Files.exists(tempFile)) {
                    Files.delete(tempFile);
                }
            } catch (IOException e) {
                log.error("删除临时文件失败: {}", e.getMessage());
            }
        }
    }

}
