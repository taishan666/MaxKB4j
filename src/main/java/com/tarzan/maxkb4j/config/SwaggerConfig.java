package com.tarzan.maxkb4j.config;


import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    // Header 参数名称
    public static final String HEADER_NAME = "Authorization";
    private static final String DEFAULT_HEADER_VALUE = "Bearer "; // 默认值，用户可修改


    @Bean
    public OpenAPI customOpenAPI() {
/*        // 定义安全方案：使用 header 传递 API Key
        SecurityScheme apiKeyScheme = new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.HEADER)
                .name(HEADER_NAME);

        // 定义全局安全需求：所有接口都应用该 header
        SecurityRequirement securityRequirement = new SecurityRequirement()
                .addList(HEADER_NAME);*/
        return new OpenAPI()
                .info(new Info()
                        .title("MaxKB4J API文档") // 设置文档标题
                        .description("MaxKB4J WEB API文档") // 设置文档描述
                        .version("2.0.0") // 设置文档版本
                        .contact(new Contact()
                                .name("tarzan") // 设置联系人姓名
                                .email("1334512682@qq.com") // 设置联系人邮箱
                                .url("https://tarzan.blog.csdn.net/")) // 设置联系人网址
                        .license(new License()
                                .name("Apache 2.0") // 设置许可证名称
                                .url("http://springdoc.org"))); // 设置许可证网址
      /*          .components(new Components().addSecuritySchemes(HEADER_NAME, apiKeyScheme))
                .addSecurityItem(securityRequirement); // 应用到全局;*/

    }

    // 关键：为所有操作（接口）动态添加一个带默认值的 Header 参数
    @Bean
    public OperationCustomizer customize() {
        return (operation, handlerMethod) -> {
            operation.addParametersItem(
                    new HeaderParameter()
                            .name(HEADER_NAME)
                            .description("API 访问密钥")
                            .required(false) // 根据需要设为 true/false
                            .schema(new StringSchema()._default(DEFAULT_HEADER_VALUE))
            );
            return operation;
        };
    }


    // 配置 GroupedOpenApi，只显示指定路径的接口
    @Bean
    public GroupedOpenApi defaultGroup() {
        return GroupedOpenApi.builder()
                .group("default") // 分组名称
                .packagesToScan("com.tarzan.maxkb4j.module.chat.controller")
                .addOperationCustomizer(customize())
                .build();
    }



}

