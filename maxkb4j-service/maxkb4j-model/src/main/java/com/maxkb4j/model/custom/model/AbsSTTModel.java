package com.maxkb4j.model.custom.model;

import com.maxkb4j.model.service.STTModel;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.TagException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

@Slf4j
@Data
public abstract class AbsSTTModel implements STTModel {

    protected int getSampleRate(byte[] audioBytes, String extension) {
        int sampleRate;
        Path tempFile=null;
        try {
            tempFile = Files.createTempFile("audio_temp_", extension);
            // 2. 将 byte[] 写入临时文件
            // 使用 WRITE 和 TRUNCATE_EXISTING 确保覆盖写入
            Files.write(tempFile, audioBytes, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
            // 3. 读取音频文件
            // 注意：AudioFileIO.read 通常接受 File 对象，所以需要将 Path 转为 File
            File fileObj = tempFile.toFile();
            AudioFile audioFile = AudioFileIO.read(fileObj);
            sampleRate = audioFile.getAudioHeader().getSampleRateAsNumber();

        } catch (ReadOnlyFileException | IOException | CannotReadException | TagException | InvalidAudioFrameException e) {
            throw new RuntimeException(e);
        } finally {
            // 4. 删除临时文件
            // 无论成功还是失败，都在 finally 块中尝试删除文件，防止磁盘垃圾堆积
            try {
                if (tempFile!=null&&Files.exists(tempFile)) {
                    Files.delete(tempFile);
                }
            } catch (IOException e) {
                log.error("删除临时文件失败: {}", e.getMessage());
            }
        }
        return sampleRate;
    }

}
