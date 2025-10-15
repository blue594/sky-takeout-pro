package com.sky.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@Slf4j
public class RedisConfiguration {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        log.info("开始创建redis模板对象...");

        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        // 设置redis的连接工厂对象
        redisTemplate.setConnectionFactory(redisConnectionFactory);

        // 创建JSON序列化器
        GenericJackson2JsonRedisSerializer jsonSerializer = createJsonSerializer();

        // 设置key的序列化器（String格式）
        redisTemplate.setKeySerializer(new StringRedisSerializer());

        // 设置value的序列化器（统一使用JSON格式）
        redisTemplate.setValueSerializer(jsonSerializer);

        // 设置hash key/value的序列化器
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(jsonSerializer);

        // 初始化参数
        redisTemplate.afterPropertiesSet();

        return redisTemplate;
    }

    private GenericJackson2JsonRedisSerializer createJsonSerializer() {
        // 创建ObjectMapper并配置
        ObjectMapper objectMapper = new ObjectMapper();

        // 支持Java 8日期时间类型
        objectMapper.registerModule(new JavaTimeModule());

        // 启用默认类型信息（在JSON中存储类型信息，解决反序列化时的类型识别问题）
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        // 禁用将日期序列化为时间戳
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        return new GenericJackson2JsonRedisSerializer(objectMapper);
    }

    /*@Bean
    public RedisTemplate redisTemplate(RedisConnectionFactory redisConnectionFactory){
        log.info("开始创建redis模板对象...");
        RedisTemplate redisTemplate = new RedisTemplate();
        //1.设置redis的连接工厂对象
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        // 2. 设置key的序列化器（String格式）
        redisTemplate.setKeySerializer(new StringRedisSerializer());

        // 3. 设置value的序列化器（JSON格式，解决乱码）
        // redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());

        // 4. 设置hash key/value的序列化器
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());

        // 5. 初始化参数
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }*/
}
