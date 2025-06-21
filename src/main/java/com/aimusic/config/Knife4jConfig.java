package com.aimusic.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j配置类
 * 集成Swagger/OpenAPI 3.0，提供API文档和测试界面
 */
@Configuration
public class Knife4jConfig {
    
    /**
     * 配置OpenAPI信息
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("曲中人 - AI音乐问答系统")
                        .description("用歌词解答人生的AI音乐问答系统API文档")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("开发团队")
                                .email("support@aimusic.com")
                                .url("https://github.com/aimusic/aimusic"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")));
    }
    
    /**
     * 配置API分组
     */
    // @Bean
    // public GroupedOpenApi publicApi() {
    //     return GroupedOpenApi.builder()
    //             .group("public")
    //             .pathsToMatch("/api/**")
    //             .build();
    // }
}