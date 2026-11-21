package com.maxkb4j.workflow.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.maxkb4j.knowledge.vo.ParagraphRagVO;
import com.maxkb4j.workflow.node.impl.VariableAggregationNode.Group;
import lombok.Data;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 回归测试：恢复执行后上下文/详情值为 JSON 形态（JSONArray/JSONObject）时的安全类型转换。
 */
class ContextValueUtilTest {

    @Data
    public static class Sample {
        private String name;
        private Integer age;
    }

    @Test
    void toList_rebuildsPojoFromParsedJsonArray() {
        // 模拟持久化 details 反序列化结果：JSONArray（元素 JSONObject）
        JSONArray parsed = JSON.parseArray("[{\"name\":\"Tom\",\"age\":18},{\"name\":\"Jerry\",\"age\":20}]");

        List<Sample> samples = ContextValueUtil.toList(parsed, Sample.class);

        assertThat(samples).hasSize(2);
        assertThat(samples.get(0)).isInstanceOf(Sample.class);
        assertThat(samples.get(0).getName()).isEqualTo("Tom");
        assertThat(samples.get(0).getAge()).isEqualTo(18);
        assertThat(samples.get(1).getName()).isEqualTo("Jerry");
    }

    @Test
    void toList_passesThroughLivePojoList() {
        Group group = new Group();
        group.setField("name");
        group.setValue("Tom");

        List<Group> groups = ContextValueUtil.toList(List.of(group), Group.class);

        assertThat(groups).containsExactly(group);
    }

    @Test
    void toList_convertsPlainMapElement() {
        Map<String, Object> element = new HashMap<>();
        element.put("name", "Tom");
        element.put("age", 18);

        List<Sample> samples = ContextValueUtil.toList(List.of(element), Sample.class);

        assertThat(samples).hasSize(1);
        assertThat(samples.get(0).getName()).isEqualTo("Tom");
        assertThat(samples.get(0).getAge()).isEqualTo(18);
    }

    @Test
    void toList_keepsStringElements() {
        JSONArray parsed = JSON.parseArray("[\"段落一\",\"段落二\"]");

        List<String> contents = ContextValueUtil.toList(parsed, String.class);

        assertThat(contents).containsExactly("段落一", "段落二");
    }

    @Test
    void toList_nullOrNonListInput_returnsEmptyList() {
        assertThat(ContextValueUtil.toList(null, Sample.class)).isEmpty();
        assertThat(ContextValueUtil.toList("plain string", Sample.class)).isEmpty();
        assertThat(ContextValueUtil.toList(JSON.parseObject("{\"name\":\"Tom\"}"), Sample.class)).isEmpty();
    }

    @Test
    void toList_skipsNullElements() {
        JSONArray parsed = new JSONArray();
        parsed.add(null);
        Map<String, Object> element = new HashMap<>();
        element.put("name", "Tom");
        parsed.add(element);

        List<Sample> samples = ContextValueUtil.toList(parsed, Sample.class);

        assertThat(samples).hasSize(1);
        assertThat(samples.get(0).getName()).isEqualTo("Tom");
    }

    @Test
    void toList_rebuildsParagraphRagVoForRerankerReference() {
        // Reranker 引用的 paragraph_list 恢复后为 JSONArray（元素 JSONObject），对应 BUG-51/BUG-46 同类隐患
        JSONArray parsed = JSON.parseArray(
                "[{\"id\":\"p1\",\"content\":\"正文内容\",\"title\":\"标题\",\"similarity\":0.9,\"knowledgeName\":\"知识库\",\"isActive\":true}]");

        List<ParagraphRagVO> paragraphs = ContextValueUtil.toList(parsed, ParagraphRagVO.class);

        assertThat(paragraphs).hasSize(1);
        assertThat(paragraphs.get(0).getContent()).isEqualTo("正文内容");
        assertThat(paragraphs.get(0).getTitle()).isEqualTo("标题");
        assertThat(paragraphs.get(0).getSimilarity()).isEqualTo(0.9);
        assertThat(paragraphs.get(0).getKnowledgeName()).isEqualTo("知识库");
        assertThat(paragraphs.get(0).getIsActive()).isTrue();
    }
}
