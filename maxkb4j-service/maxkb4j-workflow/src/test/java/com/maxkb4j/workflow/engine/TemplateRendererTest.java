package com.maxkb4j.workflow.engine;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 回归测试：工作流模板渲染流程（{{var}} 占位替换、额外变量覆盖、空安全）。
 */
class TemplateRendererTest {

    private TemplateRenderer rendererWithGlobal(String name) {
        WorkflowContext ctx = new WorkflowContext();
        ctx.getGlobalContext().put("name", name);
        return new TemplateRenderer(new VariableResolver(ctx));
    }

    @Test
    void render_substitutesContextVariable() {
        TemplateRenderer renderer = rendererWithGlobal("world");
        assertThat(renderer.render("Hello {{global.name}}")).isEqualTo("Hello world");
    }

    @Test
    void render_keepsPlainPrompt() {
        TemplateRenderer renderer = rendererWithGlobal("world");
        assertThat(renderer.render("plain prompt")).isEqualTo("plain prompt");
    }

    @Test
    void render_blankAndNullReturnEmpty() {
        TemplateRenderer renderer = rendererWithGlobal("world");
        assertThat(renderer.render("")).isEqualTo("");
        assertThat(renderer.render(null)).isEqualTo("");
    }

    @Test
    void render_addVariablesOverrideContext() {
        TemplateRenderer renderer = rendererWithGlobal("world");
        assertThat(renderer.render("Hi {{global.name}}", Map.of("global.name", "MaxKB"))).isEqualTo("Hi MaxKB");
    }

    @Test
    void render_addVariablesProvideExtraKeys() {
        TemplateRenderer renderer = rendererWithGlobal("world");
        assertThat(renderer.render("{{global.name}} and {{extra}}", Map.of("extra", "x"))).isEqualTo("world and x");
    }

    @Test
    void render_missingVariableThrows() {
        TemplateRenderer renderer = rendererWithGlobal("world");
        assertThatThrownBy(() -> renderer.render("{{global.name}} {{missing}}"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}