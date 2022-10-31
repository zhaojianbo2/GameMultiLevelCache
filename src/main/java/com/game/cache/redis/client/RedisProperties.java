package com.game.cache.redis.client;

import lombok.Data;

@Data
public class RedisProperties {
    Integer database = 0;
    /**
     * 不为空表示集群版，示例
     * localhost:7379,localhost2:7379
     */
    String cluster = "";
    String host = "localhost";
    Integer port = 6379;
    String password = null;
    /**
     * 超时时间 单位秒 默认一个小时
     */
    Integer timeout = 3600;
    
    String valueSerializer = "com.game.serializer.ProtostuffRedisSerializer";
}