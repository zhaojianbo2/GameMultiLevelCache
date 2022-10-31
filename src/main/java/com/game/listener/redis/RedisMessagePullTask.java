package com.game.listener.redis;

import com.game.cache.redis.client.IRedisClient;
import com.game.support.GlobalConfig;
import com.game.support.LayeringCacheRedisLock;
import com.game.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.util.Calendar;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 
 * @author WinkeyZhao
 *
 *
 */
public class RedisMessagePullTask {
    
    private static final Logger log = LoggerFactory.getLogger(RedisMessagePullTask.class);
    
    /**
     * 本地消息偏移量
     */
    private static final AtomicLong OFFSET = new AtomicLong(-1);
    
    /**
     * 最后一次处理推消息的时间搓，忽略并发情况下的误差，只保证可见性即可
     */
    private static volatile long LAST_PUSH_TIME = 0L;
    
    /**
     * 最后一次处理拉消息的时间搓，忽略并发情况下的误差，只保证可见性即可
     */
    private static volatile long LAST_PULL_TIME = 0L;
    
    private IRedisClient redisClient;
    
    private RedisPullMessageListener messageListener;
    
    private String channel;
    
    /**
     * 定时任务线程池
     */
    private ScheduledThreadPoolExecutor EXECUTOR ;
    
    public RedisMessagePullTask(IRedisClient redisClient, String channel, RedisPullMessageListener messageListener,
            ScheduledThreadPoolExecutor EXECUTOR) {
        this.redisClient = redisClient;
        this.channel = channel;
        this.messageListener = messageListener;
        this.EXECUTOR = EXECUTOR;
        Random random = new Random();
        int initialDelay = Math.abs(random.nextInt()) % 23;
        int delay = Math.abs(random.nextInt()) % 7;
        log.info("一级缓存拉模式同步消息每隔{}秒，执行一次", delay);
        // 1. 服务启动同步最新的偏移量
        // 1. 同步offset
        long maxOffset = redisClient.llen(channel) - 1;
        if (maxOffset < 0) {
            return;
        }
        OFFSET.getAndSet(maxOffset > 0 ? maxOffset : 0);
        // 2. 启动PULL TASK
        startPullTask(initialDelay, delay);
        // 3. 启动重置本地偏消息移量任务
        clearMessageQueueTask();
    }
    
    /**
     * 启动PULL TASK
     */
    private void startPullTask(int initialDelay, int delay) {
        EXECUTOR.scheduleWithFixedDelay(() -> {
            try {
                pullMessage();
            } catch (Exception e) {
                e.printStackTrace();
                log.error("layering-cache PULL 方式清楚一级缓存异常：{}", e.getMessage(), e);
            }
            //  初始时间间隔是7秒
        }, initialDelay, delay, TimeUnit.SECONDS);
    }
    
    /**
     * 启动清空消息队列的任务
     */
    private void clearMessageQueueTask() {
        
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 3);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        long initialDelay = System.currentTimeMillis() - cal.getTimeInMillis();
        initialDelay = initialDelay > 0 ? initialDelay : 0;
        // 每天晚上凌晨3:00执行任务
        EXECUTOR.scheduleWithFixedDelay(() -> {
            try {
                clearMessageQueue();
            } catch (Exception e) {
                e.printStackTrace();
                log.error("layering-cache 重置本地消息偏移量异常：{}", e.getMessage(), e);
            }
        }, initialDelay, TimeUnit.DAYS.toMillis(1), TimeUnit.MILLISECONDS);
        
    }
    
    public void pullMessage() {
        long maxOffset = redisClient.llen(channel) - 1;
        // 没有消息
        if (maxOffset < 0) {
            return;
        }
        // 更新本地消息偏移量
        long oldOffset = OFFSET.getAndSet(maxOffset > 0 ? maxOffset : 0);
        if (oldOffset >= maxOffset) {
            return;
        }
        List<String> messages = redisClient.lrange(channel, 0, maxOffset - oldOffset - 1,
                GlobalConfig.GLOBAL_REDIS_SERIALIZER);
        if (CollectionUtils.isEmpty(messages)) {
            return;
        }
        
        // 更新最后一次处理拉消息的时间搓
        updateLastPullTime();
        
        for (String message : messages) {
            if (log.isDebugEnabled()) {
                log.debug("redis 通过PULL方式处理本地缓，消息内容：{}", message);
            }
            if (StringUtils.isBlank(message)) {
                continue;
            }
            messageListener.message(channel,message);
        }
    }
    
    /**
     * 更新最后一次处理拉消息的时间
     */
    public void updateLastPullTime() {
        LAST_PULL_TIME = System.currentTimeMillis();
    }
    
    /**
     * 更新最后一次处理推消息的时间
     */
    public void updateLastPushTime() {
        LAST_PUSH_TIME = System.currentTimeMillis();
    }
    
    /**
     * 清空消息队列
     */
    public void clearMessageQueue() {
        LayeringCacheRedisLock lock = new LayeringCacheRedisLock(redisClient, channel, 60);
        if (lock.lock()) {
            // 清空消息，直接删除key（不可以调换顺序）
            redisClient.delete(channel);
        }
        // 重置偏移量
        OFFSET.getAndSet(-1);
    }
}
