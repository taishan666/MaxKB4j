package com.maxkb4j.tool.util;

import com.maxkb4j.common.exception.ApiException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
public class SkillsToolUtil {

    @Getter
    private final static Path skillsFolder = Paths.get("skills");


    public static Path getSkillFolder(String toolId) {
        return skillsFolder.resolve(toolId);
    }

    public static void unzipSkill(InputStream is, String toolId) {
        try {
            Files.createDirectories(skillsFolder); // 自动创建多级目录
        } catch (IOException e) {
            throw new ApiException("Failed to create skill directory: " + e.getMessage());
        }
        String rootFolderName = null;
        try (ZipInputStream zis = new ZipInputStream(is)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (rootFolderName == null && entry.isDirectory()) {
                    rootFolderName = entry.getName().substring(0, entry.getName().indexOf('/'));
                }
                String entryName = entry.getName();
                if (toolId != null && rootFolderName != null) {
                    entryName = toolId + entryName.substring(rootFolderName.length());
                }
                // 防止 zip slip 漏洞：确保解压路径在目标目录内
                Path targetPath = skillsFolder.resolve(entryName).normalize();
                if (!targetPath.startsWith(skillsFolder)) {
                    throw new ApiException("Zip entry is outside target directory: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(targetPath);
                } else {
                    Files.createDirectories(targetPath.getParent());
                    Files.copy(zis, targetPath, StandardCopyOption.REPLACE_EXISTING);
                }
                zis.closeEntry();
            }
        } catch (IOException e) {
            throw new ApiException("Failed to extract skill zip file ");
        }
    }


    /**
     * 递归删除目录及其内容
     */
    public static void deleteDirectory(String toolId) {
        Path skillFolder = skillsFolder.resolve(toolId);
        if (!Files.exists(skillFolder)) {
            return;
        }
        try {
            Files.walkFileTree(skillFolder, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            // 可选：记录警告日志
            log.warn("Failed to delete directory: {}", skillFolder, e);
        }
    }


}
