package com.maxkb4j.knowledge.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link TextSplitter#mergeChunksIntoParts} 分块合并逻辑测试：限额切分与句子级重叠回填。
 */
class TextSplitterTest {

    @Test
    void mergeChunksIntoParts_allFitIntoSinglePart() {
        List<String> parts = TextSplitter.mergeChunksIntoParts(List.of("a", "b", "c"), 10, "\n");

        assertEquals(List.of("a\nb\nc\n"), parts);
    }

    @Test
    void mergeChunksIntoParts_splitsWhenExceedingLimit() {
        // 每块计入连接符后占 3 字符，limit=7 只能容纳两块
        List<String> parts = TextSplitter.mergeChunksIntoParts(List.of("aa", "bb", "cc"), 7, "\n");

        assertEquals(List.of("aa\nbb\n", "cc\n"), parts);
    }

    @Test
    void mergeChunksIntoParts_overlapReplaysTailChunksIntoNextPart() {
        // overlap=4：切分后下一部分以完整的前一块 "bbbb" 开头，形成跨块上下文重叠
        List<String> parts = TextSplitter.mergeChunksIntoParts(List.of("aaaa", "bbbb", "cccc"), 10, "\n", 4);

        assertEquals(List.of("aaaa\nbbbb\n", "bbbb\ncccc\n"), parts);
    }

    @Test
    void mergeChunksIntoParts_zeroOverlap_startsNextPartClean() {
        List<String> parts = TextSplitter.mergeChunksIntoParts(List.of("aaaa", "bbbb", "cccc"), 10, "\n", 0);

        assertEquals(List.of("aaaa\nbbbb\n", "cccc\n"), parts);
    }

    @Test
    void mergeChunksIntoParts_emptyInput_returnsEmptyList() {
        assertEquals(List.of(), TextSplitter.mergeChunksIntoParts(List.of(), 10, "\n"));
    }

    @Test
    void mergeChunksIntoParts_singleOversizedChunk_keptAsIs() {
        // 单块超过 limit 不做字符级截断，原样保留
        List<String> parts = TextSplitter.mergeChunksIntoParts(List.of("超长内容不做截断"), 3, "\n");

        assertEquals(List.of("超长内容不做截断\n"), parts);
    }
}
