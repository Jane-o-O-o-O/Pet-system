package com.example.petmgmt.config;

import com.example.petmgmt.domain.entity.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * Jackson配置 - 确保API响应时不返回密码字段
 */
@Configuration
public class JacksonConfig {

    /**
     * 用于API响应的Jackson配置 - 隐藏password字段
     */
    @Bean
    public Jackson2ObjectMapperBuilder jackson2ObjectMapperBuilder() {
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        builder.mixIn(User.class, UserMixIn.class);
        return builder;
    }

    /**
     * MixIn接口 - 在API响应时忽略passwordHash字段
     */
    abstract class UserMixIn {
        @JsonIgnore
        abstract String getPasswordHash();
    }
}
