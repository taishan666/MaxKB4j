package com.maxkb4j.tool.util;

import com.maxkb4j.common.exception.ApiException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SkillsToolUtilTest {

    private static final long MIB = 1024 * 1024;
    private static final long SKILL_MD_LIMIT = MIB;

    @Test
    void parseSkillMetaReadsFrontMatter() throws IOException {
        byte[] zip = zip(output -> putEntry(output, "bundle/SKILL.md",
                "---\nname: demo\ndescription: test skill\n---\n".getBytes(StandardCharsets.UTF_8)));

        Map<String, String> meta = SkillsToolUtil.parseSkillMeta(zip);

        assertThat(meta).containsEntry("name", "demo").containsEntry("description", "test skill");
    }

    @Test
    void parseSkillMetaRejectsOversizedSkillMdWithUnknownEntrySize() throws IOException {
        byte[] zip = zip(output -> putEntry(output, "SKILL.md", SKILL_MD_LIMIT + 1));
        assertFirstEntrySizeUnknown(zip);

        assertThatThrownBy(() -> SkillsToolUtil.parseSkillMeta(zip))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void parseSkillMetaAcceptsSkillMdAtLimit() throws IOException {
        byte[] content = new byte[(int) SKILL_MD_LIMIT];
        Arrays.fill(content, (byte) ' ');
        byte[] frontMatter = "---\nname: bounded\n---\n".getBytes(StandardCharsets.UTF_8);
        System.arraycopy(frontMatter, 0, content, 0, frontMatter.length);
        byte[] zip = zip(output -> putEntry(output, "SKILL.md", content));

        assertThat(SkillsToolUtil.parseSkillMeta(zip)).containsEntry("name", "bounded");
    }

    @Test
    void unzipSkillRejectsUnknownSizeEntriesWhenTotalExceedsLimit() throws IOException {
        byte[] zip = zip(output -> {
            putEntry(output, "bundle/first.bin", 60 * MIB);
            putEntry(output, "bundle/second.bin", 41 * MIB);
        });
        assertFirstEntrySizeUnknown(zip);
        String toolId = "zip-limit-" + UUID.randomUUID();

        try {
            assertThatThrownBy(() -> SkillsToolUtil.unzipSkill(new ByteArrayInputStream(zip), toolId))
                    .isInstanceOf(ApiException.class);
            assertThat(Files.exists(SkillsToolUtil.getSkillFolder(toolId))).isFalse();
        } finally {
            SkillsToolUtil.deleteDirectory(toolId);
        }
    }

    @Test
    void unzipSkillWritesValidEntries() throws IOException {
        byte[] zip = zip(output -> {
            putEntry(output, "bundle/SKILL.md", "---\nname: demo\n---\n".getBytes(StandardCharsets.UTF_8));
            putEntry(output, "bundle/data.txt", "content".getBytes(StandardCharsets.UTF_8));
        });
        String toolId = "zip-valid-" + UUID.randomUUID();

        try {
            SkillsToolUtil.unzipSkill(new ByteArrayInputStream(zip), toolId);

            assertThat(SkillsToolUtil.getSkillFolder(toolId).resolve("data.txt"))
                    .content(StandardCharsets.UTF_8).isEqualTo("content");
        } finally {
            SkillsToolUtil.deleteDirectory(toolId);
        }
    }

    private static void assertFirstEntrySizeUnknown(byte[] zip) throws IOException {
        try (ZipInputStream input = new ZipInputStream(new ByteArrayInputStream(zip))) {
            ZipEntry entry = input.getNextEntry();
            assertThat(entry).isNotNull();
            assertThat(entry.getSize()).isEqualTo(-1);
        }
    }

    private static byte[] zip(ZipConsumer consumer) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream output = new ZipOutputStream(bytes)) {
            consumer.accept(output);
        }
        return bytes.toByteArray();
    }

    private static void putEntry(ZipOutputStream output, String name, byte[] content) throws IOException {
        output.putNextEntry(new ZipEntry(name));
        output.write(content);
        output.closeEntry();
    }

    private static void putEntry(ZipOutputStream output, String name, long size) throws IOException {
        output.putNextEntry(new ZipEntry(name));
        byte[] buffer = new byte[8192];
        long remaining = size;
        while (remaining > 0) {
            int length = (int) Math.min(buffer.length, remaining);
            output.write(buffer, 0, length);
            remaining -= length;
        }
        output.closeEntry();
    }

    @FunctionalInterface
    private interface ZipConsumer {
        void accept(ZipOutputStream output) throws IOException;
    }
}
