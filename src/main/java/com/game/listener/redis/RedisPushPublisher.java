package com.game.listener.redis;

import com.game.cache.redis.client.RedisClientException;
import com.game.cache.redis.client.RedisProperties;
import com.game.listener.IPublisher;
import com.game.serializer.SerializationException;
import com.game.utils.StringUtils;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Arrays;

/**
 * 
 * @author WinkeyZhao
 *
 *
 */
public class RedisPushPublisher implements IPublisher<RedisPushMessageListener> {
    
    private static final Logger logger = LoggerFactory.getLogger(RedisPushPublisher.class);
    
    private StatefulRedisPubSubConnection<String, String> pubSubConnection;
    
    
    public RedisPushPublisher(RedisProperties properties) {
        RedisURI redisURI = RedisURI.builder().withHost(properties.getHost()).withDatabase(properties.getDatabase())
                .withPort(properties.getPort()).withTimeout(Duration.ofSeconds(properties.getTimeout())).build();
        if (StringUtils.isNotBlank(properties.getPassword())) {
            redisURI.setPassword(properties.getPassword());
        }
    
        RedisClient clientPubSub = RedisClient.create(redisURI);
        clientPubSub.setOptions(ClientOptions.builder().autoReconnect(true).pingBeforeActivateConnection(true).build());
        this.pubSubConnection = clientPubSub.connectPubSub();
    }
    @Override
    public void subscribe(RedisPushMessageListener messageListener, String... channels) {
        try {
            logger.info("layering-cache和redis创建订阅关系，订阅频道【{}】", Arrays.toString(channels));
            pubSubConnection.sync().subscribe(channels);
            pubSubConnection.addListener(messageListener);
        } catch (SerializationException e) {
            throw e;
        } catch (Exception e) {
            throw new RedisClientException(e.getMessage(), e);
        }
    }
    
    @Override
    public void publish(String channel, String message) {
        pubSubConnection.sync().publish(channel, message);
    }
}
