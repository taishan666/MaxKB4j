package com.maxkb4j.knowledge.linearrag;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.seg.Segment;
import com.hankcs.hanlp.seg.common.Term;
import com.huaban.analysis.jieba.JiebaSegmenter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Lightweight entity extractor for LinearRAG Tri-Graph construction.
 * The LinearRAG paper uses a broad definition of "entity" — not just traditional
 * named entities (person/location/org), but ALL semantically meaningful noun
 * concepts including domain terms, technical concepts, product names, etc.
 * This extractor uses three complementary strategies for maximum coverage:
 * 1. HanLP POS tagging  — extract all noun-type terms (n, nr, ns, nt, nz, vn, nx...)
 * 2. TextRank keywords  — extract top important terms by graph-based ranking
 * 3. Jieba segmentation — supplementary recall for terms HanLP might miss
 * Results are merged, deduplicated, and filtered for quality.
 */
@Slf4j
public class EntityExtractor {

    private static final int MIN_ENTITY_LENGTH = 2;
    private static final int MAX_ENTITIES_PER_TEXT = 50;
    private static final int TEXT_RANK_TOP_N = 30;

    private static final JiebaSegmenter JIEBA = new JiebaSegmenter();

    /** Pure number/punctuation pattern — these are never entities */
    private static final Pattern NOISE_PATTERN = Pattern.compile("^[\\d\\s\\p{Punct}\\u3000-\\u303F\\uFF00-\\uFFEF]+$");

    /**
     * Chinese STOP_WORDS — common function words that should never be entities.
     * These are high-frequency grammatical words with no semantic content.
     */
    private static final Set<String> STOP_WORDS = Set.of(
            // 代词
            "我们", "你们", "他们", "她们", "它们", "自己", "别人", "大家", "人家",
            "什么", "怎么", "怎样", "如何", "为什么", "哪里", "哪个", "多少", "几个",
            "这个", "那个", "这些", "那些", "这里", "那里", "这儿", "那儿",
            "谁", "啥", "哪",
            // 助词/语气词
            "的话", "而已", "罢了", "着呢", "得了", "好了",
            // 连词/介词
            "但是", "然而", "虽然", "尽管", "因为", "所以", "因此", "于是",
            "如果", "假如", "即使", "无论", "不管", "只要", "只有", "除非",
            "而且", "并且", "或者", "还是", "不但", "不仅", "不过",
            "通过", "关于", "对于", "根据", "按照", "除了", "由于", "为了",
            // 副词
            "已经", "可以", "可能", "应该", "必须", "需要", "一定",
            "非常", "十分", "极其", "相当", "比较", "稍微", "有些",
            "不是", "没有", "不能", "不会", "不要", "别",
            // 动词（功能性）
            "进行", "表示", "认为", "觉得", "知道", "看到", "发现",
            // 其他
            "问题", "情况", "方面", "时候", "地方", "部分", "现在", "目前"
    );

    /**
     * POS tags representing semantically meaningful noun-type terms.
     * This is much broader than traditional NER tags:
     * - n:   普通名词 (common nouns) — covers domain terms, concepts
     * - nr:  人名 (person names)
     * - ns:  地名 (locations)
     * - nt:  机构团体 (organizations)
     * - nz:  其他专名 (proper nouns) — covers product names, tech terms
     * - nh:  人名 (alternate person tag)
     * - vn:  名动词 (verbal nouns) — e.g. "研究", "分析", "管理"
     * - nx:  外文/缩写 (foreign/abbreviations) — e.g. "AI", "RAG"
     * - ng:  名语素 (noun morpheme)
     * - nl:  名词性惯用语 (noun idioms)
     * - nf:  食品名 (food names)
     */
    private static final Set<String> NOUN_POS_TAGS = Set.of(
            "n", "nr", "nr1", "nr2", "nrf", "ns", "nt", "nz", "nh",
            "vn", "nx", "ng", "nl", "nf",
            "nnps", "nnp" // English proper nouns
    );

    /**
     * Extract entities from text using three complementary strategies.
     *
     * @param text input text (supports Chinese and English)
     * @return set of extracted entity names (deduplicated, normalized)
     */
    public static Set<String> extractEntities(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptySet();
        }

        LinkedHashSet<String> result = new LinkedHashSet<>();

        // Strategy 1: HanLP POS tagging — extract all noun-type terms
        try {
            List<String> posEntities = extractByPosTagging(text);
            result.addAll(posEntities);
        } catch (Exception e) {
            log.debug("HanLP POS extraction failed: {}", e.getMessage());
        }

        // Strategy 2: TextRank keyword extraction — important terms by graph ranking
        try {
            List<String> keywords = HanLP.extractKeyword(text, TEXT_RANK_TOP_N);
            for (String kw : keywords) {
                if (isValidEntity(kw)) {
                    result.add(kw.trim());
                }
            }
        } catch (Exception e) {
            log.debug("TextRank keyword extraction failed: {}", e.getMessage());
        }

        // Strategy 3: Jieba segmentation — supplementary recall
        try {
            List<String> jiebaEntities = extractByJieba(text);
            // Only add jieba results if we don't have enough entities yet
            if (result.size() < MAX_ENTITIES_PER_TEXT) {
                for (String entity : jiebaEntities) {
                    if (result.size() >= MAX_ENTITIES_PER_TEXT) break;
                    result.add(entity);
                }
            }
        } catch (Exception e) {
            log.debug("Jieba extraction failed: {}", e.getMessage());
        }

        // Trim to max
        if (result.size() > MAX_ENTITIES_PER_TEXT) {
            return result.stream()
                    .limit(MAX_ENTITIES_PER_TEXT)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }

        return result;
    }
    // ==================== Strategy Implementations ====================

    /**
     * Strategy 1: Extract noun-type terms via HanLP POS tagging.
     * Uses HanLP's CRF-based perceptron segmenter with POS tagging.
     * Extracts ALL noun-type POS tags, not just named entities.
     * This covers domain terms, technical concepts, product names, etc.
     */
    private static List<String> extractByPosTagging(String text) {
        Segment segment = HanLP.newSegment()
                .enableCustomDictionary(false)
                .enableNameRecognize(true)
                .enableTranslatedNameRecognize(true)
                .enableOrganizationRecognize(true)
                .enablePlaceRecognize(true);

        List<Term> terms = segment.seg(text);

        List<String> entities = new ArrayList<>();
        for (Term term : terms) {
            String word = term.word.trim();
            String nature = term.nature != null ? term.nature.toString() : "";

            if (isValidEntity(word) && isNounTag(nature)) {
                entities.add(word);
            }
        }
        return entities;
    }

    /**
     * Strategy 3: Extract terms via jieba segmentation for supplementary recall.
     * Jieba may catch terms that HanLP misses, especially for domain-specific
     * or newly coined terms not in HanLP's dictionary.
     */
    private static List<String> extractByJieba(String text) {
        List<String> tokens = JIEBA.sentenceProcess(text);
        return tokens.stream()
                .map(String::trim)
                .filter(EntityExtractor::isValidEntity)
                .distinct()
                .collect(Collectors.toList());
    }

    // ==================== Filtering Helpers ====================

    /**
     * Check if a word is a valid entity candidate.
     * Filters out noise, stopwords, and too-short terms.
     */
    private static boolean isValidEntity(String word) {
        if (word == null) return false;
        String trimmed = word.trim();
        if (trimmed.length() < MIN_ENTITY_LENGTH) return false;
        if (NOISE_PATTERN.matcher(trimmed).matches()) return false;
        return !STOP_WORDS.contains(trimmed);
    }

    /**
     * Check if a POS tag is a noun-type tag (broad definition).
     */
    private static boolean isNounTag(String nature) {
        if (nature == null || nature.isEmpty()) return false;
        String lower = nature.toLowerCase();
        return NOUN_POS_TAGS.contains(lower);
    }
}
