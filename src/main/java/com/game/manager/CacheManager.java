package com.game.manager;

import com.game.cache.ICache;
import com.game.setting.ICacheSetting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


/**
 * 
 * @author WinkeyZhao
 *
 *
 */
public class CacheManager implements ICacheManager {
    
    private Logger logger = LoggerFactory.getLogger(CacheManager.class);
    
    private static CacheManager instance;
    
    private CacheManager() {
    }
    
    public static synchronized CacheManager getInstance() {
        if (instance == null) {
            instance = new CacheManager();
        }
        return instance;
    }
    
    /**
     * 缓存容器
     */
    private final ConcurrentMap<String, ConcurrentMap<String, ICache>> cacheContainer = new ConcurrentHashMap<>(16);
    
    
    @Override
    public Collection<ICache> getCache(String name) {
        ConcurrentMap<String, ICache> cacheMap = this.cacheContainer.get(name);
        if (CollectionUtils.isEmpty(cacheMap)) {
            return Collections.emptyList();
        }
        return cacheMap.values();
    }
    
    /**
     * 创建缓存
     *
     * @param cacheSetting 缓存配置
     * @return
     */
    @Override
    public ICache createCache(ICacheSetting cacheSetting) {
        String name = cacheSetting.getCacheName();
        String internalKey = cacheSetting.getCacheInternalKey();
        // 第一次获取缓存Cache，如果有直接返回,如果没有加锁往容器里里面放Cache
        ConcurrentMap<String, ICache> cacheMap = this.cacheContainer.get(name);
        if (!CollectionUtils.isEmpty(cacheMap)) {
            ICache cache = cacheMap.get(internalKey);
            if (cache != null) {
                return cache;
            }
        }
        // 第二次获取缓存Cache，加锁往容器里里面放Cache
        synchronized (this.cacheContainer) {
            cacheMap = this.cacheContainer.get(name);
            if (!CollectionUtils.isEmpty(cacheMap)) {
                // 从容器中获取缓存
                ICache cache = cacheMap.get(internalKey);
                if (cache != null) {
                    return cache;
                }
            } else {
                cacheMap = new ConcurrentHashMap<>(16);
                cacheContainer.put(name, cacheMap);
            }
            // 新建一个Cache对象
            ICache cache = cacheSetting.createCache();
            if (cache != null) {
                cacheMap.put(internalKey, cache);
                if (cacheMap.size() > 1) {
                    logger.info("缓存名称为 {} 的缓存,存在不同的内部缓存 size:", name, cacheMap.size());
                }
            }
            return cache;
        }
    }

}
