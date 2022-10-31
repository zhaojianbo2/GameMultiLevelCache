package com.game.setting;

import com.game.cache.redis.RedisCache;
import com.game.cache.redis.client.IRedisClient;
import com.game.cache.redis.client.RedisProperties;
import com.game.cache.redis.client.SingleRedisClient;
import com.game.serializer.AbstractRedisSerializer;
import com.game.serializer.StringRedisSerializer;
import com.game.utils.StringUtils;
import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.TimeUnit;

/**
 * 
 * @author WinkeyZhao
 *
 *
 */
@Getter
@Setter
public class RedisCacheSetting implements ICacheSetting {
    
    /**
     * 缓存名称
     */
    private final String cacheName;
    
    /**
     * 内部缓存key
     */
    private final String cacheInternalKey;
    
    /**
     * 缓存有效时间
     */
    private long expiration = 0;
    
    /**
     * 缓存主动在失效前强制刷新缓存的时间
     */
    private long preloadTime = 0;
    
    /**
     * 时间单位 {@link TimeUnit}
     */
    private TimeUnit timeUnit = TimeUnit.MICROSECONDS;
    
    /**
     * 每次缓存续期是否强刷
     */
    private boolean forceRefresh = false;
    
    /**
     * 是否使用缓存名称作为 redis key 前缀
     */
    private boolean usePrefix = true;
    
    /**
     * 是否允许存NULL值
     */
    boolean allowNullValue = false;
    
    /**
     * 非空值和null值之间的时间倍率，默认是1。allowNullValue=true才有效
     * <p>
     * 如配置缓存的有效时间是200秒，倍率这设置成10， 那么当缓存value为null时，缓存的有效时间将是20秒，非空时为200秒
     * </p>
     */
    int magnification = 1;
    
    private final RedisProperties redisProperties;
    
    private IRedisClient redisClient;
    
    /**
     * @param expiration      缓存有效时间
     * @param preloadTime     缓存刷新时间
     * @param timeUnit        时间单位 {@link TimeUnit}
     * @param forceRefresh    是否强制刷新
     * @param allowNullValues 是否允许存NULL值，模式允许
     * @param magnification   非空值和null值之间的时间倍率
     */
    public RedisCacheSetting(RedisProperties redisProperties, long expiration, long preloadTime, TimeUnit timeUnit,
            boolean forceRefresh, boolean allowNullValues, int magnification, String cacheName, String cacheInternalKey) {
        this.redisProperties = redisProperties;
        this.expiration = expiration;
        this.preloadTime = preloadTime;
        this.timeUnit = timeUnit;
        this.forceRefresh = forceRefresh;
        this.allowNullValue = allowNullValues;
        this.magnification = magnification;
        this.usePrefix = true;
        this.cacheName = cacheName;
        this.cacheInternalKey = cacheInternalKey;
    }
    
    @Override
    public RedisCache createCache() {
        try {
            AbstractRedisSerializer valueRedisSerializer = (AbstractRedisSerializer) Class
                    .forName(redisProperties.getValueSerializer()).getConstructor().newInstance();
            StringRedisSerializer keyRedisSerializer = new StringRedisSerializer();
            if (StringUtils.isNotBlank(redisProperties.getCluster())) {
        	//TODO 
                // redisClient = new ClusterRedisClient(redisProperties);
            } else {
                this.redisClient = new SingleRedisClient(redisProperties);
            }
            redisClient.setKeySerializer(keyRedisSerializer);
            redisClient.setValueSerializer(valueRedisSerializer);
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return new RedisCache(this);
    }
}
