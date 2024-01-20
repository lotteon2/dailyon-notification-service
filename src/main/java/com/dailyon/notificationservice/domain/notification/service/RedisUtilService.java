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
    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    public Flux<Long> fetchAllAuctionMemberIds() {
        return reactiveRedisTemplate.opsForZSet()
                .reverseRangeByScore(NotificationConfig.AUCTION_REDIS_KEY, Range.<Double>unbounded(), RedisZSetCommands.Limit.unlimited())
                .map(Long::valueOf); // Assuming the elements in ZSet are stored as Strings that can be parsed to Longs
    }


}
