package com.game.listener.redis;

import com.alibaba.fastjson.JSON;
import com.game.cache.ICache;
import com.game.cache.layer.LayeringCache;
import com.game.listener.AbstractMessageListener;
import com.game.manager.CacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * 
 * @author WinkeyZhao
 *
 *
 */
public class RedisPullMessageListener extends AbstractMessageListener<String, String> {
    
    private static final Logger logger = LoggerFactory.getLogger(RedisPullMessageListener.class);
    
    public static final String CHANNEL = "layering-cache-channel";
    
    @Override
    public void message(String channel, String message) {
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("redis消息订阅者接收到频道【{}】发布的消息。消息内容：{}", channel, message);
            }
            RedisPubSubMessage redisPubSubMessage = JSON.parseObject(message, RedisPubSubMessage.class);
            // 根据缓存名称获取多级缓存，可能有多个
            Collection<ICache> caches = CacheManager.getInstance().getCache(redisPubSubMessage.getCacheName());
            for (ICache cache : caches) {
                // 判断缓存是否是多级缓存
                if (cache != null && cache instanceof LayeringCache) {
                    switch (redisPubSubMessage.getMessageType()) {
                        case EVICT:
                            if (RedisPubSubMessage.SOURCE.equals(redisPubSubMessage.getSource())) {
                                ((LayeringCache) cache).getRemoteCache().evict(redisPubSubMessage.getKey());
                            }
                            // 获取一级缓存，并删除一级缓存数据
                            ((LayeringCache) cache).getLocalCache().evict(redisPubSubMessage.getKey());
                            logger.info("删除一级缓存 {} 数据,key={}", redisPubSubMessage.getCacheName(),
                                    redisPubSubMessage.getKey());
                            break;
                        
                        case CLEAR:
                            if (RedisPubSubMessage.SOURCE.equals(redisPubSubMessage.getSource())) {
                                ((LayeringCache) cache).getRemoteCache().clear();
                            }
                            // 获取一级缓存，并删除一级缓存数据
                            ((LayeringCache) cache).getLocalCache().clear();
                            logger.info("清除一级缓存 {} 数据", redisPubSubMessage.getCacheName());
                            break;
                        
                        default:
                            logger.error("接收到没有定义的消息数据");
                            break;
                    }
                    
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("layering-cache 清楚一级缓存异常：{}", e.getMessage(), e);
        }
    }
}
