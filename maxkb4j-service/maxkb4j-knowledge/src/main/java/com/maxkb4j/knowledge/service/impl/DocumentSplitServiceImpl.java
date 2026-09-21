package com.maxkb4j.knowledge.service.impl;

import com.maxkb4j.common.domain.dto.KeyAndValue;
import com.maxkb4j.common.exception.ApiException;
import com.maxkb4j.knowledge.service.IDocumentSplitService;
import com.maxkb4j.knowledge.util.SentenceSplitter;
import com.maxkb4j.knowledge.util.TextSplitter;
import com.maxkb4j.knowledge.dto.ParagraphSimple;
import org.jetbrains.annotations.NotNull;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@Component
public class DocumentSplitServiceImpl implements IDocumentSplitService {

    // 预编译正则表达式以提高性能
    private static final Pattern MULTIPLE_SPACES = Pattern.compile(" +");
    private static final Pattern MULTIPLE_NEWLINES = Pattern.compile("\n{2,}");
    // 统一移除 Markdown 标题前缀（支持 1~6 个 # 后跟空格）
    private static final Pattern MARKDOWN_HEADER = Pattern.compile("#{1,6} ");

    // 统一标题正则：一次匹配所有 h1~h6 标题，替代原来 6 次扫描
    private static final Pattern UNIFIED_HEADING_PATTERN = Pattern.compile("(?m)^((#{1,6})\\s+(.+))$");

    private static final int DEFAULT_LIMIT = 512;

    /**
     * 相邻分块重叠比例（百分比）。重叠上限 = limit * overlapPercent / 100，
     * 缓解跨块边界信息丢失；设为 0 关闭重叠。
     */
    @Value("${knowledge.split.overlap-percent:10}")
    private int overlapPercent = 10;

    private static String buildTitleFromStack(String[] headingStack) {
        // 格式与原 recursive 一致："" + " " + heading → " Introduction Background"
        StringBuilder sb = new StringBuilder();
        for (String h : headingStack) {
            if (h != null) {
                sb.append(" ").append(h);
            }
        }
        return sb.toString();
    }

    public static List<String> lineSplit(String text, int limit) {
        String[] texts = text.split("\n");
        return TextSplitter.mergeChunksIntoParts(Arrays.asList(texts), limit, "\n");
    }

    /**
     * 清理字符串中的多余空格和空行，并移除 Markdown 标题符号
     */
    public static String cleanAndFilter(String input) {
        if (StringUtils.isEmpty(input)) {
            return "";
        }
        String result = MULTIPLE_SPACES.matcher(input).replaceAll(" ");
        result = MULTIPLE_NEWLINES.matcher(result).replaceAll("\n");
        result = MARKDOWN_HEADER.matcher(result).replaceAll("");
        return result.trim();
    }

    private static String cleanTitle(String input) {
        String result = MARKDOWN_HEADER.matcher(input).replaceAll("");
        return result.trim();
    }

    /**
     * 对超长内容切分，表格块整体保留；表格自身超长时按行分组切分并复制表头行，
     * 非表格文本按句子合并切分并回填 overlap 上下文。
     */
    public List<ParagraphSimple> splitContentPreserveTable(ParagraphSimple part, int limit) {
        String content = part.getContent();

        if (StringUtils.isBlank(content)) {
            return Collections.emptyList();
        }

        // 内容已不超过 limit，无需切分（避免调用昂贵的 SentenceSplitter）
        if (content.length() <= limit) {
            return Collections.singletonList(part);
        }

        // 查找所有表格块
        List<String> segments = getStringList(content);

        // 遍历 segments，对非表格段切分，表格段保留
        List<ParagraphSimple> result = new ArrayList<>();
        for (String seg : segments) {
            if (seg.startsWith("{{TABLE}}") && seg.endsWith("{{/TABLE}}")) {
                String tableContent = seg.substring("{{TABLE}}".length(), seg.length() - "{{/TABLE}}".length());
                result.addAll(splitTable(part.getTitle(), tableContent, limit));
            } else {
                List<String> texts = SentenceSplitter.split(seg, limit, overlapOf(limit));
                for (String text : texts) {
                    if (StringUtils.isNotBlank(text)) {
                        result.add(ParagraphSimple.builder()
                                .title(part.getTitle())
                                .content(text.trim())
                                .build());
                    }
                }
            }
        }

        return result;
    }

    /**
     * 表格块切分：不超长时整体保留；超长时按数据行分组，每个子块复制表头行（首行 +
     * 可选的 |---| 分隔行），避免切块后丢失列语义导致"列值与列名对不上"的召回损失。
     */
    private static List<ParagraphSimple> splitTable(String title, String tableContent, int limit) {
        ParagraphSimple whole = ParagraphSimple.builder().title(title).content(tableContent).build();
        if (tableContent.length() <= limit) {
            return Collections.singletonList(whole);
        }
        List<String> lines = Arrays.asList(tableContent.split("\n"));
        if (lines.size() <= 2) {
            // 行数太少（无法既保留表头又切分），整体保留
            return Collections.singletonList(whole);
        }
        // 表头 = 首行 + 可选分隔行
        int headerCount = lines.get(1).contains("---") ? 2 : 1;
        int headerLen = headerCount == 2 ? lines.get(0).length() + lines.get(1).length() + 2 : lines.get(0).length() + 1;

        List<ParagraphSimple> result = new ArrayList<>();
        List<String> group = new ArrayList<>(lines.subList(0, headerCount));
        int groupLen = headerLen;
        for (String line : lines.subList(headerCount, lines.size())) {
            if (group.size() > headerCount && groupLen + line.length() + 1 > limit) {
                result.add(ParagraphSimple.builder().title(title).content(String.join("\n", group)).build());
                group = new ArrayList<>(lines.subList(0, headerCount));
                groupLen = headerLen;
            }
            group.add(line);
            groupLen += line.length() + 1;
        }
        if (group.size() > headerCount) {
            result.add(ParagraphSimple.builder().title(title).content(String.join("\n", group)).build());
        }
        return result;
    }

    /**
     * 相邻分块的重叠字符数上限。
     */
    private int overlapOf(int limit) {
        return Math.max(0, limit * overlapPercent / 100);
    }

    /**
     * 将文本按表格行/非表格行分段。用逐行检测替代原正则匹配，
     * 避免 (?sm).*? 惰性匹配在大文本上的回溯开销。
     */
    private static @NotNull List<String> getStringList(String content) {
        // 快速检查：文本不含 | 则一定没有表格
        if (!content.contains("|")) {
            return Collections.singletonList(content);
        }

        String[] lines = content.split("\n");
        List<String> segments = new ArrayList<>();
        StringBuilder nonTableBuffer = new StringBuilder();
        StringBuilder tableBuffer = new StringBuilder();
        boolean inTable = false;

        for (String line : lines) {
            String trimmed = line.trim();
            boolean isTableLine = trimmed.startsWith("|") && trimmed.indexOf('|', 1) > 0;
            if (isTableLine) {
                if (!inTable) {
                    // 切换到表格模式，先把前面的非表格内容输出
                    if (!nonTableBuffer.isEmpty()) {
                        segments.add(nonTableBuffer.toString());
                        nonTableBuffer = new StringBuilder();
                    }
                    inTable = true;
                }
                tableBuffer.append(line).append("\n");
            } else {
                if (inTable) {
                    // 退出表格模式，输出表格块
                    segments.add("{{TABLE}}" + tableBuffer.toString() + "{{/TABLE}}");
                    tableBuffer = new StringBuilder();
                    inTable = false;
                }
                nonTableBuffer.append(line).append("\n");
            }
        }

        // 处理尾部缓冲
        if (inTable) {
            segments.add("{{TABLE}}" + tableBuffer.toString() + "{{/TABLE}}");
        }
        if (!nonTableBuffer.isEmpty()) {
            segments.add(nonTableBuffer.toString());
        }

        return segments;
    }

    public List<ParagraphSimple> split(String docText, String[] patterns, Integer limit, Boolean withFilter) {
        if (patterns != null && patterns.length > 0) {
            return recursive(docText, patterns, limit, withFilter);
        } else {
            return smartSplit(docText);
        }
    }

    public List<ParagraphSimple> smartSplit(String text) {
        List<ParagraphSimple> result = new ArrayList<>();

        // 阶段1：按标题切分（单次正则扫描检测；行首锚定，覆盖文档以标题开头、
        // 标题前仅单个换行等场景，原先 contains("\n\n#") 会漏检导致全文丢失标题上下文）
        List<ParagraphSimple> parts;
        if (!UNIFIED_HEADING_PATTERN.matcher(text).find()) {
            // 无标题：跳过正则扫描，整体作为一段
            parts = Collections.singletonList(ParagraphSimple.builder().title("").content(text).build());
        } else {
            // 有标题：单次正则扫描切分（替代原来 6 次扫描）
            parts = splitByHeadings(text);
        }

        // 阶段2：超长段落走 splitContentPreserveTable（与原 recursive 行为一致）
        List<ParagraphSimple> splitParts = new ArrayList<>();
        for (ParagraphSimple part : parts) {
            if (StringUtils.isNotBlank(part.getContent())) {
                if (part.getContent().length() <= DEFAULT_LIMIT) {
                    splitParts.add(part);
                } else {
                    splitParts.addAll(splitContentPreserveTable(part, DEFAULT_LIMIT));
                }
            }
        }

        // 阶段3：cleanAndFilter + lineSplit（与原 smartSplit 行为一致）
        for (ParagraphSimple part : splitParts) {
            if (StringUtils.isNotBlank(part.getContent())) {
                String cleaned = cleanAndFilter(part.getContent());
                List<String> lines = lineSplit(cleaned, DEFAULT_LIMIT);
                for (String line : lines) {
                    if (StringUtils.isNotBlank(line)) {
                        result.add(ParagraphSimple.builder().title(part.getTitle()).content(line).build());
                    }
                }
            }
        }
        return result;
    }

    /**
     * 单次正则扫描按标题层级切分，替代原来 6 次 pattern 循环。
     * 通过 heading stack 维护标题层级关系，输出与原 recursive(DEFAULT_PATTERNS) 等价。
     */
    private List<ParagraphSimple> splitByHeadings(String text) {
        List<ParagraphSimple> result = new ArrayList<>();
        // headingStack[0..5] 对应 h1~h6 的标题文本
        String[] headingStack = new String[6];

        Matcher matcher = UNIFIED_HEADING_PATTERN.matcher(text);
        int lastEnd = 0;
        String currentTitle = "";

        while (matcher.find()) {
            // 本标题之前的内容
            String contentBefore = text.substring(lastEnd, matcher.start()).trim();
            if (!contentBefore.isEmpty()) {
                result.add(ParagraphSimple.builder().title(currentTitle).content(contentBefore).build());
            }

            // 更新标题层级栈
            int level = matcher.group(2).length(); // # 的个数
            String headingText = matcher.group(3).trim();
            headingStack[level - 1] = headingText;
            // 低层级标题清空
            for (int i = level; i < 6; i++) {
                headingStack[i] = null;
            }
            currentTitle = buildTitleFromStack(headingStack);

            lastEnd = matcher.end();
        }

        // 最后一段内容
        String endContent = text.substring(lastEnd).trim();
        if (!endContent.isEmpty()) {
            result.add(ParagraphSimple.builder().title(currentTitle).content(endContent).build());
        }

        return result;
    }

    public List<ParagraphSimple> recursive(String docText, String[] patterns, int limit, Boolean withFilter) {
        if (docText == null || docText.isEmpty()) {
            return Collections.emptyList();
        }

        // 编译自定义 patterns（非法正则抛业务异常）
        Pattern[] compiledPatterns = new Pattern[patterns.length];
        for (int i = 0; i < patterns.length; i++) {
            if (patterns[i] != null && !patterns[i].isEmpty()) {
                try {
                    compiledPatterns[i] = Pattern.compile(patterns[i]);
                } catch (PatternSyntaxException e) {
                    throw new ApiException("knowledge.split.pattern.invalid", patterns[i]);
                }
            }
        }

        // 初始只有一个完整文本
        List<ParagraphSimple> parts = Collections.singletonList(ParagraphSimple.builder().title("").content(docText).build());
        // 按照标题层级循环切分
        for (Pattern pattern : compiledPatterns) {
            if (pattern == null) continue;
            List<ParagraphSimple> titleParts = new ArrayList<>();
            for (ParagraphSimple part : parts) {
                Matcher matcher = pattern.matcher(part.getContent());
                int lastEnd = 0;
                String lastTitle = part.getTitle();
                while (matcher.find()) {
                    String lastContent = part.getContent().substring(lastEnd, matcher.start()).trim();
                    titleParts.add(ParagraphSimple.builder().title(lastTitle).content(lastContent).build());
                    lastTitle = part.getTitle() + " " + cleanTitle(matcher.group());
                    lastEnd = matcher.end();
                }
                String endContent = part.getContent().substring(lastEnd).trim();
                if (!endContent.isEmpty()) {
                    titleParts.add(ParagraphSimple.builder().title(lastTitle).content(endContent).build());
                }
            }
            parts = titleParts;
        }
        // 所有 pattern 分割完成后，处理超长片段
        List<ParagraphSimple> result = new ArrayList<>();
        for (ParagraphSimple part : parts) {
            if (StringUtils.isNotBlank(part.getContent())) {
                // 内容已不超过 limit，无需再次切分
                if (part.getContent().length() <= limit) {
                    result.add(part);
                } else {
                    List<ParagraphSimple> splitParts = splitContentPreserveTable(part, limit);
                    result.addAll(splitParts);
                }
            }
        }
        if (Boolean.TRUE.equals(withFilter)) {
            return result.stream()
                    .filter(e -> StringUtils.isNotBlank(e.getContent()))
                    .peek(e -> e.setContent(cleanAndFilter(e.getContent())))
                    .toList();
        }
        return result.stream()
                .filter(e -> StringUtils.isNotBlank(e.getContent()))
                .toList();
    }

    public List<KeyAndValue> splitPattern() {
        return Arrays.asList(
                new KeyAndValue("#", "(?<=^)# .*|(?<=\\n)# .*"),
                new KeyAndValue("##", "(?<=\\n)(?<!#)## (?!#).*|(?<=^)(?<!#)## (?!#).*"),
                new KeyAndValue("###", "(?<=\\n)(?<!#)### (?!#).*|(?<=^)(?<!#)### (?!#).*"),
                new KeyAndValue("####", "(?<=\\n)(?<!#)#### (?!#).*|(?<=^)(?<!#)#### (?!#).*"),
                new KeyAndValue("#####", "(?<=\\n)(?<!#)##### (?!#).*|(?<=^)(?<!#)##### (?!#).*"),
                new KeyAndValue("######", "(?<=\\n)(?<!#)###### (?!#).*|(?<=^)(?<!#)###### (?!#).*"),
                new KeyAndValue("-", "(?<! )- .*"),
                new KeyAndValue("space", "(?<! ) (?! )"),
                new KeyAndValue("semicolon", "(?<!；)；(?!；)"),
                new KeyAndValue("comma", "(?<!，)，(?!，)"),
                new KeyAndValue("period", "(?<!。)。(?!。)"),
                new KeyAndValue("enter", "(?<!\\n)\\n(?!\\n)"),
                new KeyAndValue("blank line", "(?<!\\n)\\n\\n(?!\\n)")
        );
    }
}
