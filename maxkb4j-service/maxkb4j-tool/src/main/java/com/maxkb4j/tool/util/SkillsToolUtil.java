package com.maxkb4j.tool.util;

import com.maxkb4j.common.exception.ApiException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
public class SkillsToolUtil {

    /**
     * Skill 描述文件名
     */
    public static final String SKILL_MD = "skill.md";

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
     * 校验压缩包格式（zip 魔数），并从包内的 SKILL.md（位于根目录或唯一一级目录下）的
     * YAML front matter 中提取 skill 的 name / description。
     *
     * @param zipBytes 压缩包字节内容
     * @return 仅包含 SKILL.md 中存在的 name / description 键
     * @throws ApiException 非 zip 格式、解压失败或缺少 SKILL.md 时抛出
     */
    public static Map<String, String> parseSkillMeta(byte[] zipBytes) {
        // zip 魔数校验：PK\x03\x04（空 zip 为 PK\x05\x06）
        if (zipBytes == null || zipBytes.length < 4 || zipBytes[0] != 'P' || zipBytes[1] != 'K') {
            throw new ApiException("tool.skill.file.format.invalid");
        }
        // entry 名编码兼容：先按 UTF-8 尝试，失败（如 Windows 中文环境压缩的 GBK 文件名）再回退 GBK
        String skillMd = readSkillMd(zipBytes, StandardCharsets.UTF_8);
        if (skillMd == null && !StandardCharsets.UTF_8.equals(Charset.forName("GBK"))) {
            skillMd = readSkillMd(zipBytes, Charset.forName("GBK"));
        }
        if (skillMd == null) {
            throw new ApiException("tool.skill.file.missing.skill.md");
        }
        return parseFrontMatter(skillMd);
    }

    /**
     * 用指定字符集遍历 zip entry，查找并读取 SKILL.md 内容。
     *
     * @return SKILL.md 内容；未找到返回 null；entry 名无法按该字符集解码时返回 null（由调用方回退重试）
     */
    private static String readSkillMd(byte[] zipBytes, Charset charset) {
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes), charset)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory() && isSkillMdEntry(entry.getName())) {
                    return new String(zis.readAllBytes(), StandardCharsets.UTF_8);
                }
                zis.closeEntry();
            }
        } catch (IOException e) {
            throw new ApiException("tool.skill.file.format.invalid");
        } catch (IllegalArgumentException e) {
            // entry 名字符集不匹配，交给调用方用其他编码重试
            return null;
        }
        return null;
    }

    /**
     * 判断 entry 是否为 SKILL.md（根目录或唯一一级目录下）。
     */
    private static boolean isSkillMdEntry(String entryName) {
        String normalized = entryName.replace('\\', '/').toLowerCase(Locale.ROOT);
        return normalized.equals(SKILL_MD) || normalized.matches("[^/]+/" + SKILL_MD);
    }

    /**
     * 解析 Markdown 的 YAML front matter（首行 --- 与结束 --- 之间），
     * 提取简单的 name: / description: 键值（支持引号包裹）。
     */
    private static Map<String, String> parseFrontMatter(String content) {
        Map<String, String> meta = new HashMap<>();
        String[] lines = content.split("\r?\n");
        int i = 0;
        while (i < lines.length && lines[i].isBlank()) {
            i++;
        }
        if (i >= lines.length || !lines[i].trim().equals("---")) {
            return meta;
        }
        i++;
        while (i < lines.length) {
            String line = lines[i].trim();
            i++;
            if ("---".equals(line)) {
                break;
            }
            int idx = line.indexOf(':');
            if (idx <= 0) {
                continue;
            }
            String key = line.substring(0, idx).trim();
            String value = line.substring(idx + 1).trim();
            if (value.length() >= 2
                    && ((value.startsWith("\"") && value.endsWith("\""))
                    || (value.startsWith("'") && value.endsWith("'")))) {
                value = value.substring(1, value.length() - 1);
            }
            if ("name".equals(key) || "description".equals(key)) {
                meta.putIfAbsent(key, value);
            }
        }
        return meta;
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
