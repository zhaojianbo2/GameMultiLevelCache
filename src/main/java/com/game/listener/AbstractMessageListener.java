package com.game.listener;

import io.lettuce.core.pubsub.RedisPubSubListener;

/**
 * 
 * @author WinkeyZhao
 *
 *
 */
public abstract class AbstractMessageListener<K, V> implements RedisPubSubListener<K, V> {

    @Override
    public void message(K var1, K var2, V var3) {

    }

    @Override
    public void subscribed(K var1, long l) {

    }

    @Override
    public void psubscribed(K var1, long l) {

    }

    @Override
    public void unsubscribed(K var1, long l) {

    }

    @Override
    public void punsubscribed(K var1s, long l) {

    }
}
