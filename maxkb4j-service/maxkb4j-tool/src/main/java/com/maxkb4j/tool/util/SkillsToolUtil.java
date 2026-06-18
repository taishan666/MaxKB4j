package com.maxkb4j.tool.util;

import com.maxkb4j.common.exception.ApiException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Set;
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
            throw new ApiException("tool.skill.directory.create.failed", e.getMessage());
        }
        byte[] zipBytes;
        try {
            zipBytes = is.readAllBytes();
        } catch (IOException e) {
            throw new ApiException("tool.skill.zip.extract.failed");
        }

        String rootFolderName = null;
        Set<String> topLevelFolders = new HashSet<>();
        Set<String> topLevelFiles = new HashSet<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String entryName = entry.getName();
                int separatorIndex = entryName.indexOf('/');
                if (separatorIndex > 0) {
                    topLevelFolders.add(entryName.substring(0, separatorIndex));
                } else if (!entry.isDirectory()) {
                    topLevelFiles.add(entryName);
                }
                zis.closeEntry();
            }
        } catch (IOException e) {
            throw new ApiException("tool.skill.zip.extract.failed");
        }
        if (topLevelFolders.size() == 1 && topLevelFiles.isEmpty()) {
            rootFolderName = topLevelFolders.iterator().next();
        }

        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String entryName = entry.getName();
                if (toolId != null) {
                    if (rootFolderName != null) {
                        entryName = toolId + entryName.substring(rootFolderName.length());
                    } else {
                        entryName = Paths.get(toolId, entryName).toString();
                    }
                }
                // 防止 zip slip 漏洞：确保解压路径在目标目录内
                Path targetPath = skillsFolder.resolve(entryName).normalize();
                if (!targetPath.startsWith(skillsFolder)) {
                    throw new ApiException("tool.skill.zip.entry.outside.target", entry.getName());
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
            throw new ApiException("tool.skill.zip.extract.failed");
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
