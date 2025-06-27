package com.tarzan.maxkb4j.config;


import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
//@OpenAPIDefinition
public class SwaggerConfig {


    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("MaxKB4J API文档") // 设置文档标题
                        .description("MaxKB4J WEB API文档") // 设置文档描述
                        .version("1.0.0") // 设置文档版本
                        .contact(new Contact()
                                .name("tarzan") // 设置联系人姓名
                                .email("1334512682@qq.com") // 设置联系人邮箱
                                .url("https://tarzan.blog.csdn.net/")) // 设置联系人网址
                        .license(new io.swagger.v3.oas.models.info.License()
                                .name("Apache 2.0") // 设置许可证名称
                                .url("http://springdoc.org"))); // 设置许可证网址

    }


    // 配置 GroupedOpenApi，只显示指定路径的接口
    @Bean
    public GroupedOpenApi defaultGroup() {
        return GroupedOpenApi.builder()
                .group("default") // 分组名称
                .packagesToScan("com.tarzan.maxkb4j.module.application.api")
                .build();
    }

}

