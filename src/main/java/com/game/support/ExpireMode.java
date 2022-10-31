package com.game.support;

public enum ExpireMode
{
    /**
     * 每写入一次重新计算一次缓存的有效时间
     */
    WRITE,
    /**
     * 每访问一次重新计算一次缓存的有效时间
     */
    ACCESS;
}
