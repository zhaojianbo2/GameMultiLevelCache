package com.game.listener.redis;

import com.game.listener.AbstractMessageListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author WinkeyZhao
 *
 *
 */
public class RedisPushMessageListener extends AbstractMessageListener<String,String> {
    private static final Logger log = LoggerFactory.getLogger(RedisPushMessageListener.class);

    @Override
    public void message(String channel, String message) {
        try {
            if (log.isDebugEnabled()) {
                log.debug("redis消息订阅者接收到频道【{}】发布的消息。消息内容：{}", channel, message);
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("layering-cache 清楚一级缓存异常：{}", e.getMessage(), e);
        }
    }
}
