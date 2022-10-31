package com.game.manager;

import com.game.cache.redis.client.RedisProperties;
import com.game.listener.redis.RedisPullMessageListener;
import com.game.listener.redis.RedisPullPublisher;
import com.game.listener.redis.RedisPushMessageListener;
import com.game.listener.redis.RedisPushPublisher;


/**
 * 
 * @author WinkeyZhao
 *
 *
 */
public class PublisherBuilder {
    public static final String CHANNEL = "layering-cache-channel";
    /**
     * 注册redis 推送模式的数据更新通知
     */
    public static RedisPushPublisher buildPushPublisher(){
        //注册redis 推模式 消息通知
        RedisPushMessageListener listener = new RedisPushMessageListener();
        RedisPushPublisher publisher = new RedisPushPublisher(new RedisProperties());
        publisher.subscribe(listener, CHANNEL);
        return publisher;
    }
    
    /**
     * 注册redis 拉取模式的数据更新通知
     */
    public static RedisPullPublisher buildPullPublisher(){
        //注册redis 推模式 消息通知
        RedisPullMessageListener listener = new RedisPullMessageListener();
        RedisPullPublisher publisher = new RedisPullPublisher(new RedisProperties());
        publisher.subscribe(listener,CHANNEL);
        return publisher;
    }
    
}
