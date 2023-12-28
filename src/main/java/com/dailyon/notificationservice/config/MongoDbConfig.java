package com.dailyon.notificationservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;

@Configuration
@EnableReactiveMongoRepositories(basePackages = "com.dailyon.notificationservice.domain.notification.repository")
@EnableMongoAuditing
public class MongoDbConfig {
}