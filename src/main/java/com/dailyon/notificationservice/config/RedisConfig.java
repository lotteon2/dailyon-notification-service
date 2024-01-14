package com.dailyon.notificationservice.config;


import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.*;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@RequiredArgsConstructor
public class RedisConfig {
    private final Environment env;

    @Bean
    @Profile(value = "!prod")
    @Primary
    public ReactiveRedisConnectionFactory standaloneRedisConnectionFactory() {
        RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration();
        redisStandaloneConfiguration.setHostName(
                Objects.requireNonNull(env.getProperty("spring.redis.host")));
        redisStandaloneConfiguration.setPort(
                Integer.parseInt(Objects.requireNonNull(env.getProperty("spring.redis.port"))));
        redisStandaloneConfiguration.setPassword(env.getProperty("spring.redis.password"));

        return new LettuceConnectionFactory(redisStandaloneConfiguration);
    }

    @Bean
    @Profile("prod")
    @Primary
    public ReactiveRedisConnectionFactory clusterRedisConnectionFactory() {
        RedisClusterConfiguration clusterConfiguration = new RedisClusterConfiguration();
        clusterConfiguration.setClusterNodes(
                parseRedisNodes(Objects.requireNonNull(env.getProperty("spring.redis.cluster.nodes"))));
//        String password = env.getProperty("spring.redis.password"); // 필요시 추가
//        if (password != null && !password.isEmpty()) {
//            clusterConfiguration.setPassword(password);
//        }
        return new LettuceConnectionFactory(clusterConfiguration);
    }

    private Set<RedisNode> parseRedisNodes(String nodes) {
        Set<RedisNode> redisNodes = new HashSet<>();

        for (String node : Objects.requireNonNull(nodes).split(",")) {
            String[] parts = node.split(":");
            redisNodes.add(new RedisNode(parts[0], Integer.parseInt(parts[1])));
        }
        return redisNodes;
    }

    @Bean
    @Primary
    public ReactiveRedisTemplate<String, String> reactiveRedisStringTemplate(ReactiveRedisConnectionFactory factory) {
        StringRedisSerializer keySerializer = new StringRedisSerializer();
        StringRedisSerializer valueSerializer = new StringRedisSerializer();

        RedisSerializationContext<String, String> serializationContext = RedisSerializationContext
                .<String, String>newSerializationContext(keySerializer)
                .hashKey(keySerializer)
                .hashValue(valueSerializer)
                .string(valueSerializer)
                .build();

        return new ReactiveRedisTemplate<>(factory, serializationContext);
    }


//    @Bean
//    public ReactiveStringRedisTemplate reactiveStringRedisTemplate(ReactiveRedisConnectionFactory factory) {
//        return new ReactiveStringRedisTemplate(factory, RedisSerializationContext.string());
//    }

//    @Bean
//    public ReactiveRedisTemplate<String, String> reactiveStringRedisTemplate(
//            ReactiveRedisConnectionFactory factory) {
//        StringRedisSerializer keySerializer = new StringRedisSerializer();
//        StringRedisSerializer valueSerializer = new StringRedisSerializer();

//        RedisSerializationContext.RedisSerializationContextBuilder<String, String> builder =
//                RedisSerializationContext.newSerializationContext(keySerializer);
//        RedisSerializationContext<String, String> context = builder.value(valueSerializer).build();
//        return new ReactiveRedisTemplate<>(factory, context);
//        return new ReactiveStringRedisTemplate(factory, RedisSerializationContext.string());
//    }

    

//    @Bean
//    public <V> ReactiveRedisTemplate<String, V> reactiveRedisTemplate(
//            ReactiveRedisConnectionFactory factory, RedisSerializationContext<String, V> serializationContext) {
//        return new ReactiveRedisTemplate<>(factory, serializationContext);
//    }
}
