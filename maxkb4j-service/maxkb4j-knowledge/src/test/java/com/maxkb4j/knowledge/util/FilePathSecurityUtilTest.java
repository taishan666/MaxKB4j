package com.maxkb4j.knowledge.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link FilePathSecurityUtil} 路径穿越防护测试。
 */
class FilePathSecurityUtilTest {

    @Test
    void normalizeFilePath_rejectsTraversalAttempt() {
        assertNull(FilePathSecurityUtil.normalizeFilePath("../secret.txt"));
        assertNull(FilePathSecurityUtil.normalizeFilePath("docs/../../etc/passwd"));
    }

    @Test
    void normalizeFilePath_returnsNormalizedPathWithoutTraversal() {
        String normalized = FilePathSecurityUtil.normalizeFilePath("docs/./sub/file.txt");

        assertTrue(normalized != null && normalized.endsWith("file.txt"));
        assertFalse(normalized.contains(".."));
    }

    @Test
    void normalizeFilePath_plainNameKeptAsIs() {
        assertEquals("file.txt", FilePathSecurityUtil.normalizeFilePath("file.txt"));
    }

    @Test
    void normalizeFilePath_nullOrInvalid_returnsNull() {
        assertNull(FilePathSecurityUtil.normalizeFilePath(null));
        // 含非法字符（NUL）的路径无法构造 Path 对象
        assertNull(FilePathSecurityUtil.normalizeFilePath("bad\0name.txt"));
    }

    @Test
    void illegalityFileName_detectsTraversalPatterns() {
        assertTrue(FilePathSecurityUtil.illegalityFileName(null));
        assertTrue(FilePathSecurityUtil.illegalityFileName("a/../b.txt"));
        assertTrue(FilePathSecurityUtil.illegalityFileName("a\\..\\b.txt"));
        assertTrue(FilePathSecurityUtil.illegalityFileName("..hidden"));
        assertTrue(FilePathSecurityUtil.illegalityFileName("a..b.txt"));
    }

    @Test
    void illegalityFileName_acceptsNormalNames() {
        assertFalse(FilePathSecurityUtil.illegalityFileName("knowledge.xlsx"));
        assertFalse(FilePathSecurityUtil.illegalityFileName("dir/file.txt"));
        assertFalse(FilePathSecurityUtil.illegalityFileName("报告2024.docx"));
    }
}
