package com.game.setting;

import com.game.cache.ICache;


/**
 * 
 * @author WinkeyZhao
 *
 *
 */
public interface ICacheSetting{
    
    public  String getCacheInternalKey();
    
    public String getCacheName();
    
    public ICache createCache();
}
