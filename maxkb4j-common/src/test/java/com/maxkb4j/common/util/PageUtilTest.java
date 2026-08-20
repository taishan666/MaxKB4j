package com.maxkb4j.common.util;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 回归测试：分页对象拷贝与记录类型转换流程。
 */
class PageUtilTest {

    static class Source {
        private String name;
        public Source() {}
        public Source(String name) { this.name = name; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    static class Target {
        private String name;
        public Target() {}
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    private Page<Source> page(List<Source> records) {
        Page<Source> page = new Page<>();
        page.setRecords(records);
        page.setCurrent(2L);
        page.setSize(20L);
        page.setTotal(55L);
        return page;
    }

    @Test
    void copy_byClass_copiesRecordsAndMetadata() {
        IPage<Target> result = BeanUtil.copyPage(
                page(List.of(new Source("a"), new Source("b"))), Target.class);

        assertThat(result.getRecords()).hasSize(2);
        assertThat(result.getRecords().get(0).getName()).isEqualTo("a");
        assertThat(result.getCurrent()).isEqualTo(2L);
        assertThat(result.getSize()).isEqualTo(20L);
        assertThat(result.getTotal()).isEqualTo(55L);
    }


    @Test
    void copy_byMapper_transformsRecords() {
        IPage<String> result = BeanUtil.copyPage(
                page(List.of(new Source("a"), new Source("b"))), Source::getName);

        assertThat(result.getRecords()).containsExactly("a", "b");
        assertThat(result.getCurrent()).isEqualTo(2L);
        assertThat(result.getTotal()).isEqualTo(55L);
    }
}