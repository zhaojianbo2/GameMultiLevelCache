package com.game.support;

public enum CacheMode {
    /**
     * 只开启本地缓存
     */
    LOCAL(1 << 0),

    /**
     * 只开启远程缓存
     */
    REMOTE(1 << 1),

    /**
     * 同时开启一级缓存和二级缓存
     */
    ALL(1 << 0 | 1 << 1);

    public int val;

    CacheMode(int val) {
	this.val = val;
    }

    public boolean compare(CacheMode cacheMode) {
	return (cacheMode.val & this.val) != 0;
    }

}
