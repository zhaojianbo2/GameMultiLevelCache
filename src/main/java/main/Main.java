package main;

import java.util.concurrent.TimeUnit;

import com.game.manager.CacheManager;
import com.game.setting.CaffeineCacheSetting;
import com.game.support.ExpireMode;

public class Main {

    public static void main(String[] args) {
	// 创建本地CaffeineCache
	CaffeineCacheSetting saffeineCacheSetting = new CaffeineCacheSetting("local", "local-1", 500, 500, 60,
		TimeUnit.SECONDS, ExpireMode.WRITE);
	CacheManager.getInstance().createCache(saffeineCacheSetting);
    }
}
