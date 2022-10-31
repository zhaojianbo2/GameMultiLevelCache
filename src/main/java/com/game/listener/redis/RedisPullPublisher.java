package com.game.listener.redis;

import com.alibaba.fastjson.JSON;
import com.game.cache.redis.client.RedisProperties;
import com.game.cache.redis.client.SingleRedisClient;
import com.game.listener.IPublisher;
import com.game.support.GlobalConfig;
import com.game.utils.NamedThreadFactory;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 
 * @author WinkeyZhao
 *
 *
 */
@Getter
public class RedisPullPublisher implements IPublisher<RedisPullMessageListener> {
    private static final Logger logger = LoggerFactory.getLogger(RedisPushPublisher.class);
    private  SingleRedisClient redisClient;
    /**
     * 定时任务线程池
     */
    private ScheduledThreadPoolExecutor EXECUTOR = new ScheduledThreadPoolExecutor(3,
            new NamedThreadFactory("layering-cache-pull-message"));
    public RedisPullPublisher(RedisProperties properties) {
        this.redisClient = new SingleRedisClient(properties);
    }
    @Override
    public void publish(String channel, String message) {
        String messageJson = JSON.toJSONString(message);
        // pull 拉模式消息
        redisClient.lpush(channel, GlobalConfig.GLOBAL_REDIS_SERIALIZER, messageJson);
        redisClient.expire(channel, 25, TimeUnit.HOURS);
    }
    
    @Override
    public void subscribe(RedisPullMessageListener messageListener, String... channels) {
        Arrays.stream(channels).forEach(channel->{
             new RedisMessagePullTask(redisClient,channel,messageListener,EXECUTOR);
        });
        
    }
}
