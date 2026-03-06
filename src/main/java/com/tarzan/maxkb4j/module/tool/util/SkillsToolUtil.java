package com.tarzan.maxkb4j.module.tool.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tarzan.maxkb4j.common.exception.ApiException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class SkillsToolUtil {

    private static final String MANIFEST_FILE_NAME = "manifest.json";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static Path getManifestPath(String appSkillPath) {
        return Paths.get(appSkillPath, MANIFEST_FILE_NAME);
    }

    public static List<String> getAddShills(String appSkillPath, List<String> toolIds,Map<String, String> existingManifest ) {
        // 获取当前需要的 toolId 集合
        Set<String> currentToolIds = new HashSet<>(toolIds);
        Set<String> existingToolIds = new HashSet<>(existingManifest.keySet());
        // 1. 找出新增的：需要解压
        Set<String> toAdd = new HashSet<>(currentToolIds);
        toAdd.removeAll(existingToolIds);
        // 2. 找出要删除的：已存在但不在当前列表中
        Set<String> toRemove = new HashSet<>(existingToolIds);
        toRemove.removeAll(currentToolIds);
        // 删除不再需要的技能目录
        for (String toolId : toRemove) {
            String folderName = existingManifest.get(toolId);
            if (folderName != null) {
                Path dirToDelete = Paths.get(appSkillPath, folderName);
                deleteRecursively(dirToDelete);
            }
            existingManifest.remove(toolId);
        }
        return new ArrayList<>(toAdd);
    }

    public static Map<String, String> readManifest(String appSkillPath) {
        Path manifestPath = getManifestPath(appSkillPath);
        if (!Files.exists(manifestPath)) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(manifestPath.toFile(), new TypeReference<>() {
            });
        } catch (IOException e) {
            // 若 manifest 损坏，视为无记录，重建
            return new HashMap<>();
        }
    }

    public static void updateManifest(String appSkillPath, Map<String, String> manifest) throws ApiException {
        Path manifestPath = getManifestPath(appSkillPath);
        try {
            objectMapper.writeValue(manifestPath.toFile(), manifest);
        } catch (IOException e) {
            throw new ApiException("Failed to write tool manifest: " + e.getMessage());
        }
    }

    public static String unzipSkill(Path appSkillFolderPath, InputStream is) {
        String rootFolderName = null;
        try (ZipInputStream zis = new ZipInputStream(is)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (rootFolderName == null) {
                    rootFolderName = entry.getName();
                }
                // 防止 zip slip 漏洞：确保解压路径在目标目录内
                Path targetPath = appSkillFolderPath.resolve(entry.getName()).normalize();
                if (!targetPath.startsWith(appSkillFolderPath)) {
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
        return rootFolderName;
    }



    /**
     * 递归删除目录及其内容
     */
    private static void deleteRecursively(Path path) {
        if (!Files.exists(path)) {
            return;
        }
        try {
            Files.walkFileTree(path, new SimpleFileVisitor<>() {
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
            // log.warn("Failed to delete directory: " + path, e);
        }
    }


}
