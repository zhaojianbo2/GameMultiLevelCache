package com.game.cache;

import com.game.listener.IPublisher;
import org.springframework.cache.support.NullValue;
import org.springframework.util.Assert;

/**
 * 
 * @author WinkeyZhao
 *
 *
 */
public abstract class AbstractCache implements ICache {
    /**
     * 缓存名称
     */
    protected final String name;
    protected IPublisher<?> publisher;

    public AbstractCache(String name, IPublisher<?> publisher) {
	Assert.notNull(name, "缓存名称不能为NULL");
	this.name = name;
	this.publisher = publisher;
    }

    public AbstractCache(String name) {
	Assert.notNull(name, "缓存名称不能为NULL");
	this.name = name;
    }

    /**
     * 处理null 情况
     * 
     * @param userValue
     * @return
     */
    protected Object toStoreValue(Object userValue) {
	if (userValue == null) {
	    return NullValue.INSTANCE;
	}
	return userValue;
    }
}
