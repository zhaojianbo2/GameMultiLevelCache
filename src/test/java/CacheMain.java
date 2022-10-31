import com.game.listener.IPublisher;
import com.game.listener.redis.RedisPushPublisher;
import com.game.manager.CacheManager;
import com.game.manager.PublisherBuilder;
import com.game.setting.CaffeineCacheSetting;
import com.game.setting.ICacheSetting;
import com.game.setting.LayeringCacheSetting;
import com.game.setting.RedisCacheSetting;
import com.game.support.CacheMode;

/**
 * @author : zGame
 * @version V1.0
 * @Project: game-multi-cache
 * @Package PACKAGE_NAME
 * @Description: TODO
 * @date Date : 2022年02月16日 17:47
 */
public class CacheMain {
    
    public static void main(String[] args) {
    
//        CacheManager manager = CacheManager.getInstance();
//        RedisPushPublisher publisher = PublisherBuilder.buildPushPublisher();
//        LayeringCacheSetting layeringCacheSetting  = new LayeringCacheSetting( localCacheSetting,  remoteCacheSetting,
//                 cacheMode,  cacheName,  cacheInternalKey, publisher);
//        manager.createCache(layeringCacheSetting);
    }
}
