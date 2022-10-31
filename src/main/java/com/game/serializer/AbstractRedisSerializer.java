package com.game.serializer;


import org.springframework.cache.support.NullValue;

import java.util.Objects;

public abstract class AbstractRedisSerializer implements RedisSerializer {
    private byte[] nullValueBytes;

    /**
     * 获取空值的序列化值
     *
     * @return byte[]
     */
    public byte[] getNullValueBytes() {
        if (Objects.isNull(nullValueBytes)) {
            synchronized (this) {
                nullValueBytes = serialize(NullValue.INSTANCE);
            }
        }
        return nullValueBytes;
    }
}