package com.game.support;


import com.game.serializer.JdkRedisSerializer;
import com.game.serializer.RedisSerializer;

public class GlobalConfig {
    /**
     * 缓存统计和消息推送序列化器
     */
    public static final RedisSerializer GLOBAL_REDIS_SERIALIZER = new JdkRedisSerializer();
}
