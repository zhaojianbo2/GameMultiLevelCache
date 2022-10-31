package com.game.cache.redis;

import com.alibaba.fastjson.JSON;
import com.game.cache.AbstractCache;
import com.game.cache.redis.client.IRedisClient;
import com.game.cache.redis.client.RedisCacheKey;
import com.game.setting.RedisCacheSetting;
import com.game.support.AwaitThreadContainer;
import com.game.support.LayeringCacheRedisLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.support.NullValue;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * 
 * @author WinkeyZhao
 *
 *
 */
public class RedisCache extends AbstractCache {

    protected static final Logger logger = LoggerFactory.getLogger(RedisCache.class);

    /**
     * 刷新缓存等待时间，单位毫秒
     */
    private static final long WAIT_TIME = 500;

    /**
     * 等待线程容器
     */
    private final AwaitThreadContainer container = new AwaitThreadContainer();

    private RedisCacheSetting redisCacheSetting;

    /**
     * @param redisCacheSetting
     */
    public RedisCache(RedisCacheSetting redisCacheSetting) {
	super(redisCacheSetting.getCacheName());
	Assert.notNull(redisCacheSetting.getRedisClient(), "RedisTemplate 不能为NULL");
	this.redisCacheSetting = redisCacheSetting;
    }

    @Override
    public <T> T get(String key, Class<T> resultType) {
	RedisCacheKey redisCacheKey = getRedisCacheKey(key);
	if (logger.isDebugEnabled()) {
	    logger.debug("redis缓存 key= {} 查询redis缓存", redisCacheKey.getKey());
	}
	return redisCacheSetting.getRedisClient().get(redisCacheKey.getKey(), resultType);
    }

    /**
     * 获取 RedisCacheKey
     *
     * @param key 缓存key
     * @return RedisCacheKey
     */
    public RedisCacheKey getRedisCacheKey(String key) {
	return new RedisCacheKey(key, redisCacheSetting.getRedisClient().getKeySerializer(), name,
		redisCacheSetting.isUsePrefix());
    }

    @Override
    public <T> T get(String key, Class<T> resultType, Callable<T> valueLoader) {
	IRedisClient redisClient = redisCacheSetting.getRedisClient();
	RedisCacheKey redisCacheKey = getRedisCacheKey(key);
	if (logger.isDebugEnabled()) {
	    logger.debug("redis缓存 key= {} 查询redis缓存如果没有命中，从数据库获取数据", redisCacheKey.getKey());
	}
	// 先获取缓存，如果有直接返回
	T result = redisClient.get(redisCacheKey.getKey(), resultType);
	if (Objects.nonNull(result) || redisClient.hasKey(redisCacheKey.getKey())) {
	    // 检查是否继续续期
	    refreshCache(redisCacheKey, resultType, valueLoader, result);
	    return result;
	}
	// 执行缓存方法,核心方法
	return executeCacheMethod(redisCacheKey, resultType, valueLoader);
    }

    @Override
    public void put(String key, Object value) {
	RedisCacheKey redisCacheKey = getRedisCacheKey(key);
	if (logger.isDebugEnabled()) {
	    logger.debug("redis缓存 key= {} put缓存，缓存值：{}", redisCacheKey.getKey(), JSON.toJSONString(value));
	}
	putValue(redisCacheKey, value);
    }

    @Override
    public <T> T putIfAbsent(String key, Object value, Class<T> resultType) {
	if (logger.isDebugEnabled()) {
	    logger.debug("redis缓存 key= {} putIfAbsent缓存，缓存值：{}", getRedisCacheKey(key).getKey(),
		    JSON.toJSONString(value));
	}
	T result = get(key, resultType);
	if (result != null) {
	    return result;
	}
	put(key, value);
	return null;
    }

    @Override
    public void evict(String key) {

    }

    @Override
    public void clear() {
	// 必须开启了使用缓存名称作为前缀，clear才有效
	if (redisCacheSetting.isUsePrefix()) {
	    logger.info("清空redis缓存 ，缓存前缀为{}", name);
	    Set<String> keys = redisCacheSetting.getRedisClient().scan(name + "*");
	    if (!CollectionUtils.isEmpty(keys)) {
		redisCacheSetting.getRedisClient().delete(keys);
	    }
	}
    }

    private Object putValue(RedisCacheKey key, Object value) {
	Object result = toStoreValue(value);
	long expirationTime = redisCacheSetting.getExpiration();
	// 允许缓存NULL值且缓存为值为null时需要重新计算缓存时间
	if (result instanceof NullValue) {
	    expirationTime = expirationTime / redisCacheSetting.getMagnification();
	}
	// 将数据放到缓存
	redisCacheSetting.getRedisClient().set(key.getKey(), result, expirationTime, TimeUnit.MILLISECONDS);
	return result;
    }

    /**
     * 刷新缓存数据
     */
    private <T> void refreshCache(RedisCacheKey redisCacheKey, Class<T> resultType, Callable<T> valueLoader,
	    Object result) {
	long preload = redisCacheSetting.getPreloadTime();
	// 允许缓存NULL值，则自动刷新时间也要除以倍数
	if (result instanceof NullValue) {
	    preload = preload / redisCacheSetting.getMagnification();
	}
	if (isRefresh(redisCacheKey, preload)) {
	    // 判断是否需要强制刷新在开启刷新线程
	    if (!redisCacheSetting.isForceRefresh()) {
		if (logger.isDebugEnabled()) {
		    logger.debug("redis缓存 key={} 软刷新缓存模式", redisCacheKey.getKey());
		}
		// 续期
		softRefresh(redisCacheKey);
	    } else {
		if (logger.isDebugEnabled()) {
		    logger.debug("redis缓存 key={} 强刷新缓存模式", redisCacheKey.getKey());
		}
		// forceRefresh(redisCacheKey, resultType, valueLoader, preload);
	    }
	}
    }

    /**
     * 判断是否需要刷新缓存
     *
     * @param redisCacheKey 缓存key
     * @param preloadTime   预加载时间（经过计算后的时间）
     * @return boolean
     */
    private boolean isRefresh(RedisCacheKey redisCacheKey, long preloadTime) {
	// 获取锁之后再判断一下过期时间，看是否需要加载数据
	Long ttl = redisCacheSetting.getRedisClient().getExpire(redisCacheKey.getKey());
	// -2表示key不存在
	if (ttl == null || ttl == -2) {
	    return true;
	}
	// 当前缓存时间小于刷新时间就需要刷新缓存
	return ttl > 0 && TimeUnit.SECONDS.toMillis(ttl) <= preloadTime;
    }

    /**
     * 软刷新，直接修改缓存时间
     *
     * @param redisCacheKey {@link RedisCacheKey}
     */
    private void softRefresh(RedisCacheKey redisCacheKey) {
	IRedisClient redisClient = redisCacheSetting.getRedisClient();
	// 加一个分布式锁，只放一个请求去刷新缓存
	LayeringCacheRedisLock redisLock = new LayeringCacheRedisLock(redisClient, redisCacheKey.getKey() + "_lock");
	try {
	    if (redisLock.tryLock()) {
		redisClient.expire(redisCacheKey.getKey(), redisCacheSetting.getExpiration(), TimeUnit.MILLISECONDS);
	    }
	} catch (Exception e) {
	    logger.error(e.getMessage(), e);
	} finally {
	    redisLock.unlock();
	}
    }

    /**
     * 核心方法,可以参考成熟框架源码对这里继续进行优化
     */
    private <T> T executeCacheMethod(RedisCacheKey redisCacheKey, Class<T> resultType, Callable<T> valueLoader) {
	IRedisClient redisClient = redisCacheSetting.getRedisClient();
	LayeringCacheRedisLock redisLock = new LayeringCacheRedisLock(redisClient,
		redisCacheKey.getKey() + "_sync_lock", 1);
	while (true) {
	    try {
		// 先取缓存，如果有直接返回
		T result = redisClient.get(redisCacheKey.getKey(), resultType);
		if (result != null) {
		    if (logger.isDebugEnabled()) {
			logger.debug("redis缓存 key= {} 获取到锁后查询查询缓存命中，不需要执行被缓存的方法", redisCacheKey.getKey());
		    }
		    return result;
		}

		// 获取分布式锁去后台查询数据
		if (redisLock.lock()) {
		    T t = loaderAndPutValue(redisCacheKey, valueLoader);
		    if (logger.isDebugEnabled()) {
			logger.debug("redis缓存 key= {} 从数据库获取数据完毕，唤醒所有等待线程", redisCacheKey.getKey());
		    }
		    // 唤醒线程
		    container.signalAll(redisCacheKey.getKey());
		    return t;
		}
		// 线程等待
		if (logger.isDebugEnabled()) {
		    logger.debug("redis缓存 key= {} 从数据库获取数据未获取到锁，进入等待状态，等待{}毫秒", redisCacheKey.getKey(), WAIT_TIME);
		}
		container.await(redisCacheKey.getKey(), WAIT_TIME);
	    } catch (Exception e) {
		container.signalAll(redisCacheKey.getKey());
	    } finally {
		redisLock.unlock();
	    }
	}
    }

    /**
     * 加载并将数据放到redis缓存
     */
    private <T> T loaderAndPutValue(RedisCacheKey key, Callable<T> valueLoader) throws Exception {
	long start = System.currentTimeMillis();

	try {
	    // 加载数据
	    Object result = putValue(key, valueLoader.call());
	    if (logger.isDebugEnabled()) {
		logger.debug("redis缓存 key={} 执行被缓存的方法，并将其放入缓存, 耗时：{}。数据:{}", key.getKey(),
			System.currentTimeMillis() - start, JSON.toJSONString(result));
	    }
	    return (T) (result);
	} catch (Exception e) {
	    throw new Exception(key.getKey(), e.getCause());
	}
    }

    /**
     * 硬刷新（执行被缓存的方法）
     *
     * @param redisCacheKey {@link RedisCacheKey}
     * @param valueLoader   数据加载器
     * @param preloadTime   缓存预加载时间
     */
//    private <T> void forceRefresh(RedisCacheKey redisCacheKey, Class<T> resultType, Callable<T> valueLoader, long preloadTime) {
//        // 尽量少的去开启线程，因为线程池是有限的
//        ThreadTaskUtils.run(() -> {
//            // 加一个分布式锁，只放一个请求去刷新缓存
//            LayeringCacheRedisLock redisLock = new LayeringCacheRedisLock(redisClient, redisCacheKey.getKey() + "_lock");
//            try {
//                if (redisLock.lock()) {
//                    // 获取锁之后再判断一下过期时间，看是否需要加载数据
//                    if (isRefresh(redisCacheKey, preloadTime)) {
//                        // 获取缓存中老数据
//                        Object oldDate = redisClient.get(redisCacheKey.getKey(), resultType);
//                        // 加载数据并放到缓存
//                        Object newDate = loaderAndPutValue(redisCacheKey, valueLoader, false);
//                        // 比较新老数据是否相等，如果不想等就删除一级缓存
//                        if (!Objects.equals(oldDate, newDate) && !JSON.toJSONString(oldDate).equals(JSON.toJSONString(newDate))) {
//                            logger.debug("二级缓存数据发生变更，同步刷新一级缓存");
//                            deleteFirstCache((String) redisCacheKey.getKeyElement(), redisClient);
//                        }
//                    }
//                }
//            } catch (Exception e) {
//                logger.error(e.getMessage(), e);
//            } finally {
//                redisLock.unlock();
//            }
//        });
//    }
}
