package com.game.setting;

import com.game.cache.caffeine.CaffeineCache;
import com.game.cache.layer.LayeringCache;
import com.game.cache.redis.RedisCache;
import com.game.listener.IPublisher;
import com.game.support.CacheMode;
import lombok.Getter;
import lombok.Setter;

/**
 * 
 * @author WinkeyZhao
 * @note 包含一级CaffeineCache 二级 RedisCache
 *
 */
@Getter
@Setter
public class LayeringCacheSetting implements ICacheSetting {

    /**
     * 緩存名稱
     */
    private final String cacheName;

    /**
     * 内部缓存名
     */
    private final String cacheInternalKey;

    /**
     * 本地缓存设置
     */
    private final CaffeineCacheSetting localCacheSetting;

    /**
     * reDis缓存设置
     */
    private final RedisCacheSetting remoteCacheSetting;

    /**
     * 是否使用一级缓存
     */
    CacheMode cacheMode;

    /**
     * 缓存变更推送
     */
    private final IPublisher<?> publisher;

    /**
     * cache下
     */

    public LayeringCacheSetting(CaffeineCacheSetting localCacheSetting, RedisCacheSetting remoteCacheSetting,
	    CacheMode cacheMode, String cacheName, String cacheInternalKey, IPublisher<?> publisher) {
	this.localCacheSetting = localCacheSetting;
	this.remoteCacheSetting = remoteCacheSetting;
	this.cacheMode = cacheMode;
	this.cacheName = cacheName;
	this.cacheInternalKey = cacheInternalKey;
	this.publisher = publisher;
    }

    @Override
    public LayeringCache createCache() {
	CaffeineCache caffeineCache = null;
	RedisCache redisCache = null;
	// 创建一级缓存
	if (cacheMode.compare(CacheMode.LOCAL)) {
	    caffeineCache = localCacheSetting.createCache();
	}
	if (cacheMode.compare(CacheMode.REMOTE)) {
	    // 创建二级缓存
	    redisCache = remoteCacheSetting.createCache();
	}
	// 封装多级缓存对象
	return new LayeringCache(caffeineCache, redisCache, cacheMode, cacheName, publisher);
    }
}
