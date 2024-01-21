package com.dailyon.notificationservice.domain.notification.service;


import com.dailyon.notificationservice.config.NotificationConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.RedisZSetCommands;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveSetOperations;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Slf4j
@RequiredArgsConstructor
@Service
public class RedisUtilService {
    private final ReactiveRedisTemplate<String, Long> reactiveRedisTemplate;
    private final ReactiveSetOperations<String, Long> reactiveSetOperations;

    public Flux<Long> fetchAllAuctionMemberIds(String auctionId) {
        return reactiveSetOperations.members(auctionId)
                .doOnNext(memberId -> log.info("Fetched auction member ID: {}", memberId))
                .doOnError(error -> log.error("Error fetching auction member IDs for Auction ID: {}, error: {}", auctionId, error.getMessage()))
                .doOnComplete(() -> log.info("Completed fetching auction member IDs for Auction ID: {}", auctionId));
    }
}
