package com.game.cache.layer;

import com.alibaba.fastjson.JSON;
import com.game.cache.AbstractCache;
import com.game.listener.IPublisher;
import com.game.listener.redis.RedisPubSubMessage;
import com.game.listener.redis.RedisPubSubMessageType;
import com.game.support.CacheMode;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;

/**
 * 
 * @author WinkeyZhao Cache-Aside
 *
 */
@Getter
public class LayeringCache extends AbstractCache {

    Logger logger = LoggerFactory.getLogger(LayeringCache.class);

    /**
     * 一级缓存
     */
    private final AbstractCache localCache;

    /**
     * 二级缓存
     */
    private final AbstractCache remoteCache;

    /**
     * 缓存模式
     */
    private final CacheMode cacheMode;

    /**
     *
     * @param localCache
     * @param remoteCache
     * @param cacheMode
     * @param cacheName
     * @param publisher
     */
    public LayeringCache(AbstractCache localCache, AbstractCache remoteCache, CacheMode cacheMode, String cacheName,
	    IPublisher<?> publisher) {
	super(cacheName, publisher);
	this.localCache = localCache;
	this.remoteCache = remoteCache;
	this.cacheMode = cacheMode;
    }

    /**
     *
     * @param localCache  本地缓存
     * @param remoteCache 远端缓存
     * @param cacheMode   缓存模式
     * @param cacheName   缓存名称
     */
    public LayeringCache(AbstractCache localCache, AbstractCache remoteCache, CacheMode cacheMode, String cacheName) {
	super(cacheName);
	this.localCache = localCache;
	this.remoteCache = remoteCache;
	this.cacheMode = cacheMode;
    }

    @Override
    public <T> T get(String key, Class<T> resultType) {
	T result = null;
	// 有本地缓存
	if (cacheMode.compare(CacheMode.LOCAL)) {
	    result = localCache.get(key, resultType);
	    if (logger.isDebugEnabled()) {
		logger.debug("查询一级缓存。 key={}:{},返回值是:{}", name, key, JSON.toJSONString(result));
	    }
	    if (result != null) {
		return result;
	    }
	}
	// 有远端缓存
	if (cacheMode.compare(CacheMode.REMOTE)) {
	    result = remoteCache.get(key, resultType);
	    if (logger.isDebugEnabled()) {
		logger.debug("查询二级缓存。 key={}:{},返回值是:{}", name, key, JSON.toJSONString(result));
	    }
	    // 把null也放入远端缓存
	    if (result == null) {
		remoteCache.put(key, result);
		logger.debug("二级缓存值为空,进行null保存。 key={}", key);
	    }
	}
	// 前面已经判断本地缓存,走到这里肯定本地缓存没有,所以存入result
	if (cacheMode.compare(CacheMode.LOCAL)) {
	    localCache.put(key, result);
	    if (logger.isDebugEnabled()) {
		logger.debug("将数据放到一级缓存。 key={}:{},返回值是:{}", name, key, JSON.toJSONString(result));
	    }
	}
	return result;
    }

    @Override
    public <T> T get(String key, Class<T> resultType, Callable<T> valueLoader) {
	T result = null;
	// 有本地缓存
	if (cacheMode.compare(CacheMode.LOCAL)) {
	    // 如果只有一级缓存,获取不到则在一级缓存自动加载,否则让二级缓存去加载
	    result = CacheMode.LOCAL.val == (CacheMode.LOCAL.val) ? localCache.get(key, resultType, valueLoader)
		    : localCache.get(key, resultType);
	    if (logger.isDebugEnabled()) {
		logger.debug("查询一级缓存。 key={}:{},返回值是:{}", name, key, JSON.toJSONString(result));
	    }
	    if (result != null) {
		return result;
	    }
	}
	// 有远端缓存
	if (cacheMode.compare(CacheMode.REMOTE)) {
	    result = remoteCache.get(key, resultType, valueLoader);
	    if (logger.isDebugEnabled()) {
		logger.debug("查询二级缓存。 key={}:{},返回值是:{}", name, key, JSON.toJSONString(result));
	    }
	    // 把null也放入远端缓存
	    if (result == null) {
		remoteCache.put(key, result);
	    }
	}
	// 前面已经判断本地缓存,走到这里肯定本地缓存没有,所以存入result
	if (cacheMode.compare(CacheMode.LOCAL)) {
	    localCache.put(key, result);
	    if (logger.isDebugEnabled()) {
		logger.debug("将数据放到一级缓存。 key={}:{},返回值是:{}", name, key, JSON.toJSONString(result));
	    }
	}
	return result;
    }

    @Override
    public void put(String key, Object value) {
	// 只开启一级缓存
	if (CacheMode.LOCAL.val == cacheMode.val) {
	    localCache.put(key, value);
	    return;
	}

	// 开启二级缓存
	remoteCache.put(key, value);
	// 删除一级缓存
	if (CacheMode.ALL.equals(cacheMode) && publisher != null) {
	    deleteLocalCache(key);
	}
    }

    @Override
    public <T> T putIfAbsent(String key, Object value, Class<T> resultType) {
	return null;
    }

    @Override
    public void evict(String key) {

    }

    @Override
    public void clear() {
	// 只开启一级缓存
	if (CacheMode.LOCAL.val == cacheMode.val) {
	    localCache.clear();
	    return;
	}

	// 开启二级缓存、删除的时候要先删除二级缓存再删除一级缓存，否则有并发问题
	remoteCache.clear();
	if (CacheMode.ALL.val == cacheMode.val) {
	    //通知其他服务器删除一级缓存
	    RedisPubSubMessage message = new RedisPubSubMessage();
	    message.setCacheName(name);
	    message.setMessageType(RedisPubSubMessageType.CLEAR);
	    // 发布消息
	    // RedisPublisher.publisher(redisClient, message);
	}
    }

    public void deleteLocalCache(String key) {
	// 删除一级缓存需要用到redis的Pub/Sub（订阅/发布）模式，否则集群中其他服服务器节点的一级缓存数据无法删除
	RedisPubSubMessage message = new RedisPubSubMessage();
	message.setCacheName(name);
	message.setKey(key);
	message.setMessageType(RedisPubSubMessageType.EVICT);
	publisher.publish(key, JSON.toJSONString(message));
    }
}
