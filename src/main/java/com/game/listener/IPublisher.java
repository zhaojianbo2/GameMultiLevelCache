package com.game.listener;


/**
 * 
 * @author WinkeyZhao
 *
 *
 */
public interface IPublisher<T extends AbstractMessageListener<?,?>> {
    public void publish(String channel,String message);
    public void subscribe(T messageListener, String... channels);
}
