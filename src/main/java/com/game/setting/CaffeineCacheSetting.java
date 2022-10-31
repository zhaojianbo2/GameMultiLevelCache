package com.game.setting;

import com.game.cache.caffeine.CaffeineCache;
import com.game.support.ExpireMode;
import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.TimeUnit;

/**
 * 
 * @author WinkeyZhao
 * @note 使用CaffeineCache作为本地缓存
 *
 */

@Getter
@Setter
public class CaffeineCacheSetting implements ICacheSetting {

    /**
     * 缓存初始Size
     */
    private int initialCapacity = 10;

    /**
     * 缓存最大Size
     */
    private int maximumSize = 500;

    /**
     * 缓存有效时间
     */
    private int expireTime = 0;

    /**
     * 缓存时间单位
     */
    private TimeUnit timeUnit = TimeUnit.MILLISECONDS;

    /**
     * 缓存失效模式{@link ExpireMode}
     */
    private ExpireMode expireMode = ExpireMode.WRITE;

    /**
     * 缓存名称
     */
    private final String cacheName;

    private final String cacheInternalKey;

    /**
     * @param initialCapacity 缓存初始Size
     * @param maximumSize     缓存最大Size
     * @param expireTime      缓存有效时间
     * @param timeUnit        缓存时间单位 {@link TimeUnit}
     * @param expireMode      缓存失效模式{@link ExpireMode}
     */
    public CaffeineCacheSetting(String cacheName, String cacheInternalKey, int initialCapacity, int maximumSize,
	    int expireTime, TimeUnit timeUnit, ExpireMode expireMode) {
	this.cacheName = cacheName;
	this.cacheInternalKey = cacheInternalKey;
	this.initialCapacity = initialCapacity;
	this.maximumSize = maximumSize;
	this.expireTime = expireTime;
	this.timeUnit = timeUnit;
	this.expireMode = expireMode;
    }

    @Override
    public CaffeineCache createCache() {
	return new CaffeineCache(this);
    }
}
