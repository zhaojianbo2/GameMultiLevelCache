package com.game.cache.caffeine;

import com.alibaba.fastjson.JSON;
import com.game.cache.AbstractCache;
import com.game.setting.CaffeineCacheSetting;
import com.game.support.ExpireMode;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;

/**
 * 
 * @author WinkeyZhao
 * @note 创建CaffeineCache
 *
 */
public class CaffeineCache extends AbstractCache {
    
    protected static final Logger logger = LoggerFactory.getLogger(CaffeineCache.class);
    
    /**
     * 缓存对象
     */
    private final Cache<Object, Object> cache;
    
    public CaffeineCache(CaffeineCacheSetting localCacheSetting) {
        super(localCacheSetting.getCacheName());
        Caffeine<Object, Object> builder = Caffeine.newBuilder();
        builder.initialCapacity(localCacheSetting.getInitialCapacity());
        builder.maximumSize(localCacheSetting.getMaximumSize());
        //基于引用回收
        //软引用：如果一个对象只具有软引用，则内存空间足够，垃圾回收器就不会回收它；如果内存空间不足了，就会回收这些对象的内存。
        //弱引用：弱引用的对象拥有更短暂的生命周期。在垃圾回收器线程扫描它所管辖的内存区域的过程中，一旦发现了只具有弱引用的对象，不管当前内存空间足够与否，都会回收它的内存
        builder.softValues();//软引用
        //基于时间回收
        if (ExpireMode.WRITE.equals(localCacheSetting.getExpireMode())) {
            builder.expireAfterWrite(localCacheSetting.getExpireTime(), localCacheSetting.getTimeUnit());
        } else if (ExpireMode.ACCESS.equals(localCacheSetting.getExpireMode())) {
            builder.expireAfterAccess(localCacheSetting.getExpireTime(), localCacheSetting.getTimeUnit());
        }
        // 根据Caffeine builder创建 Cache 对象
        this.cache = builder.build();
    }
    
    @Override
    public <T> T get(String key, Class<T> resultType) {
        if (logger.isDebugEnabled()) {
            logger.debug("caffeine缓存 key={}:{} 获取缓存", name, key);
        }
        //是否自动加载
        if (this.cache instanceof LoadingCache) {
            return (T) ((LoadingCache<Object, Object>) this.cache).get(key);
        }
        return (T) cache.getIfPresent(key);
    }
    
    @Override
    public <T> T get(String key, Class<T> resultType, Callable<T> valueLoader) {
        if (logger.isDebugEnabled()) {
            logger.debug("caffeine缓存 key={}:{} 获取缓存， 如果没有命中自动加载", name, key);
        }
        Object result = this.cache.get(key, k -> loaderValue(key, valueLoader));
        return (T) (result);
    }
    
    @Override
    public void put(String key, Object value) {
        this.cache.put(key, toStoreValue(value));
    }
    
    @Override
    public <T> T putIfAbsent(String key, Object value, Class<T> resultType) {
        if (logger.isDebugEnabled()) {
            logger.debug("caffeine缓存 key={}:{} putIfAbsent 缓存，缓存值：{}", name, key, JSON.toJSONString(value));
        }
        Object result = this.cache.get(key, k -> toStoreValue(value));
        return (T) (result);
    }
    
    @Override
    public void evict(String key) {
        if (logger.isDebugEnabled()) {
            logger.debug("caffeine缓存 key={}:{} 清除缓存", name, key);
        }
        this.cache.invalidate(key);
    }
    @Override
    public void clear() {
        logger.debug("caffeine缓存清空缓存");
        this.cache.invalidateAll();
    }
    
    /**
     * 加载数据
     */
    private <T> Object loaderValue(Object key, Callable<T> valueLoader) {
        long start = System.currentTimeMillis();
        try {
            T t = valueLoader.call();
            if (logger.isDebugEnabled()) {
                logger.debug("caffeine缓存 key={}:{} 从库加载缓存 {}", name, key, JSON.toJSONString(t));
            }
            return toStoreValue(t);
        } catch (Exception ex) {
            throw new RuntimeException(String.format("加载key为 %s 的缓存数据,执行被缓存方法异常", JSON.toJSONString(key)), ex);
        }
        
    }
}
