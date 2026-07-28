package com.maxkb4j.common.util;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 回归测试：Bean 拷贝、列表/分页拷贝与对象转 Map 流程。
 */
class BeanUtilTest {

    static class Source {
        private String name;
        private Integer age;
        private String blank;

        public Source() {
        }

        public Source(String name, Integer age) {
            this.name = name;
            this.age = age;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Integer getAge() { return age; }
        public void setAge(Integer age) { this.age = age; }
        public String getBlank() { return blank; }
        public void setBlank(String blank) { this.blank = blank; }
    }

    static class Target {
        private String name;
        private Integer age;

        public Target() {
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Integer getAge() { return age; }
        public void setAge(Integer age) { this.age = age; }
    }

    @Test
    void copy_createsNewInstanceWithCopiedProperties() {
        Target target = BeanUtil.copy(new Source("alice", 30), Target.class);
        assertThat(target).isNotSameAs(new Source("alice", 30));
        assertThat(target.getName()).isEqualTo("alice");
        assertThat(target.getAge()).isEqualTo(30);
    }

    @Test
    void copyList_copiesAllElementsInOrder() {
        List<Target> result = BeanUtil.copyList(
                List.of(new Source("a", 1), new Source("b", 2)), Target.class);
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("a");
        assertThat(result.get(1).getName()).isEqualTo("b");
    }

    @Test
    void copyList_nullAndEmptyReturnEmptyList() {
        assertThat(BeanUtil.copyList(null, Target.class)).isEmpty();
        assertThat(BeanUtil.copyList(List.of(), Target.class)).isEmpty();
    }

    @Test
    void copyList_withMapperTransformsElements() {
        List<String> names = BeanUtil.copyList(
                List.of(new Source("a", 1), new Source("b", 2)), Source::getName);
        assertThat(names).containsExactly("a", "b");
    }

    @Test
    void copyPropertiesExcludeNull_keepsExistingTargetValuesForNullSource() {
        Source source = new Source("alice", null);
        Target target = new Target();
        target.setName("orig");
        target.setAge(99);

        BeanUtil.copyPropertiesExcludeNull(source, target);

        assertThat(target.getName()).isEqualTo("alice");
        // age 在 source 中为 null，被忽略，target 保留原值
        assertThat(target.getAge()).isEqualTo(99);
    }

    @Test
    void toMap_collectsNonNullNonBlankFieldsOnly() {
        Source source = new Source("alice", null);
        source.setBlank("   ");

        Map<String, Object> map = BeanUtil.toMap(source);

        assertThat(map).containsEntry("name", "alice");
        assertThat(map).doesNotContainKey("age");
        assertThat(map).doesNotContainKey("blank");
    }

    @Test
    void toMap_nullReturnsEmpty() {
        assertThat(BeanUtil.toMap(null)).isEmpty();
    }

    @Test
    void copyPage_preservesMetadataAndCopiesRecords() {
        Page<Source> page = new Page<>();
        page.setRecords(List.of(new Source("a", 1), new Source("b", 2)));
        page.setCurrent(1L);
        page.setSize(10L);
        page.setTotal(2L);
        page.setPages(1L);

        IPage<Target> result = BeanUtil.copyPage(page, Target.class);

        assertThat(result.getRecords()).hasSize(2);
        assertThat(result.getRecords().get(0).getName()).isEqualTo("a");
        assertThat(result.getCurrent()).isEqualTo(1L);
        assertThat(result.getSize()).isEqualTo(10L);
        assertThat(result.getTotal()).isEqualTo(2L);
        assertThat(result.getPages()).isEqualTo(1L);
    }

    @Test
    void copyPage_nullReturnsEmptyPage() {
        IPage<Target> result = BeanUtil.copyPage(null, Target.class);
        assertThat(result).isNotNull();
        assertThat(result.getRecords()).isEmpty();
    }
}