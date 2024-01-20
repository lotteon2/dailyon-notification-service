package com.dailyon.notificationservice.domain.notification.service;


import com.dailyon.notificationservice.config.NotificationConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.RedisZSetCommands;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Slf4j
@RequiredArgsConstructor
@Service
public class RedisUtilService {
    private final ReactiveRedisTemplate<String, Long> reactiveRedisTemplate;

    public Flux<Long> fetchAllAuctionMemberIds(String auctionId) {
        return reactiveRedisTemplate.opsForZSet()
                .reverseRangeByScore(auctionId, Range.<Double>unbounded(), RedisZSetCommands.Limit.unlimited());
    }


}
