package com.game.manager;

import com.game.cache.ICache;
import com.game.setting.ICacheSetting;

import java.util.Collection;


/**
 * 
 * @author WinkeyZhao
 *
 *
 */
public interface ICacheManager {
    
    
    /**
     * 根据缓存名称返回对应的{@link Collection}.
     *
     * @param name 缓存的名称 (不能为 {@code null})
     * @return 返回对应名称的Cache, 如果没找到返回 {@code null}
     */
    Collection<ICache> getCache(String name);
    
    /**
     * 根据缓存名称返回对应的{@link ICache}，如果没有找到就新建一个并放到容器
     *
     * @param cacheSetting 缓存配置
     * @return {@link ICache}
     */
    ICache createCache(ICacheSetting cacheSetting);
    
}
