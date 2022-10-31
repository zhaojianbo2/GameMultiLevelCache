package com.game.cache.redis.client;

import com.alibaba.fastjson.JSON;
import com.game.serializer.JdkRedisSerializer;
import com.game.serializer.RedisSerializer;
import com.game.serializer.SerializationException;
import com.game.serializer.StringRedisSerializer;
import com.game.utils.StringUtils;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.KeyScanCursor;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.ScanArgs;
import io.lettuce.core.ScanCursor;
import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.SetArgs;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.codec.ByteArrayCodec;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 单机版Redis客户端
 *
 */
@Getter
@Setter
public class SingleRedisClient implements IRedisClient {
    Logger logger = LoggerFactory.getLogger(SingleRedisClient.class);
    /**
     * 默认key序列化方式
     */
    private RedisSerializer keySerializer = new StringRedisSerializer();

    /**
     * 默认value序列化方式
     */
    private RedisSerializer valueSerializer = new JdkRedisSerializer();

    private final StatefulRedisConnection<byte[], byte[]> connection;

    public SingleRedisClient(RedisProperties properties) {
        RedisURI redisURI = RedisURI.builder().withHost(properties.getHost())
                .withDatabase(properties.getDatabase())
                .withPort(properties.getPort())
                .withTimeout(Duration.ofSeconds(properties.getTimeout()))
                .build();
        if (StringUtils.isNotBlank(properties.getPassword())) {
            redisURI.setPassword(properties.getPassword());
        }
        if (StringUtils.isNotBlank(properties.getValueSerializer())) {
            try {
                valueSerializer = (RedisSerializer) Class.forName(properties.getValueSerializer()).getDeclaredConstructor().newInstance();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        logger.info("layering-cache redis配置" + JSON.toJSONString(properties));
        RedisClient client = RedisClient.create(redisURI);
        client.setOptions(ClientOptions.builder()
                .autoReconnect(true)
                .pingBeforeActivateConnection(true)
                .build());
        this.connection = client.connect(new ByteArrayCodec());
    }

    @Override
    public <T> T get(String key, Class<T> resultType) {
        try {
            RedisCommands<byte[], byte[]> sync = connection.sync();
            return getValueSerializer().deserialize(sync.get(getKeySerializer().serialize(key)), resultType);
        } catch (SerializationException e) {
            throw e;
        } catch (Exception e) {
            throw new RedisClientException(e.getMessage(), e);
        }
    }

    @Override
    public String set(String key, Object value) {

        try {
            RedisCommands<byte[], byte[]> sync = connection.sync();
            return sync.set(getKeySerializer().serialize(key), getValueSerializer().serialize(value));
        } catch (SerializationException e) {
            throw e;
        } catch (Exception e) {
            throw new RedisClientException(e.getMessage(), e);
        }
    }

    @Override
    public String set(String key, Object value, long time, TimeUnit unit) {

        try {
            RedisCommands<byte[], byte[]> sync = connection.sync();
            return sync.setex(getKeySerializer().serialize(key), unit.toSeconds(time), getValueSerializer().serialize(value));
        } catch (SerializationException e) {
            throw e;
        } catch (Exception e) {
            throw new RedisClientException(e.getMessage(), e);
        }
    }
    @Override
    public String setNxEx(String key, Object value, long time) {

        try {
            RedisCommands<byte[], byte[]> sync = connection.sync();
            return sync.set(getKeySerializer().serialize(key), getValueSerializer().serialize(value), SetArgs.Builder.nx().ex(time));
        } catch (SerializationException e) {
            throw e;
        } catch (Exception e) {
            throw new RedisClientException(e.getMessage(), e);
        }
    }

    @Override
    public Long delete(String... keys) {
        if (Objects.isNull(keys) || keys.length == 0) {
            return 0L;
        }
        try {
            RedisCommands<byte[], byte[]> sync = connection.sync();

            final byte[][] bkeys = new byte[keys.length][];
            for (int i = 0; i < keys.length; i++) {
                bkeys[i] = getKeySerializer().serialize(keys[i]);
            }
            return sync.del(bkeys);
        } catch (SerializationException e) {
            throw e;
        } catch (Exception e) {
            throw new RedisClientException(e.getMessage(), e);
        }
    }


    @Override
    public Long delete(Set<String> keys) {

        return delete(keys.toArray(new String[0]));
    }

    @Override
    public Boolean hasKey(String key) {

        try {
            RedisCommands<byte[], byte[]> sync = connection.sync();
            return sync.exists(getKeySerializer().serialize(key)) > 0;
        } catch (SerializationException e) {
            throw e;
        } catch (Exception e) {
            throw new RedisClientException(e.getMessage(), e);
        }
    }

    @Override
    public Boolean expire(String key, long timeout, TimeUnit timeUnit) {
        try {
            RedisCommands<byte[], byte[]> sync = connection.sync();
            return sync.expire(getKeySerializer().serialize(key), timeUnit.toSeconds(timeout));
        } catch (SerializationException e) {
            throw e;
        } catch (Exception e) {
            throw new RedisClientException(e.getMessage(), e);
        }
    }

    @Override
    public Long getExpire(String key) {
        try {
            RedisCommands<byte[], byte[]> sync = connection.sync();
            return sync.ttl(getKeySerializer().serialize(key));
        } catch (SerializationException e) {
            throw e;
        } catch (Exception e) {
            throw new RedisClientException(e.getMessage(), e);
        }
    }

    @Override
    public Long lpush(String key, RedisSerializer valueRedisSerializer, String... values) {
        if (Objects.isNull(values) || values.length == 0) {
            return 0L;
        }
        try {
            RedisCommands<byte[], byte[]> sync = connection.sync();
            final byte[][] bvalues = new byte[values.length][];
            for (int i = 0; i < values.length; i++) {
                bvalues[i] = valueRedisSerializer.serialize(values[i]);
            }

            return sync.lpush(getKeySerializer().serialize(key), bvalues);
        } catch (SerializationException e) {
            throw e;
        } catch (Exception e) {
            throw new RedisClientException(e.getMessage(), e);
        }
    }

    @Override
    public Long llen(String key) {
        try {
            RedisCommands<byte[], byte[]> sync = connection.sync();
            return sync.llen(getKeySerializer().serialize(key));
        } catch (SerializationException e) {
            throw e;
        } catch (Exception e) {
            throw new RedisClientException(e.getMessage(), e);
        }
    }

    @Override
    public List<String> lrange(String key, long start, long end, RedisSerializer valueRedisSerializer) {
        try {
            RedisCommands<byte[], byte[]> sync = connection.sync();
            List<String> list = new ArrayList<>();
            List<byte[]> values = sync.lrange(getKeySerializer().serialize(key), start, end);
            if (CollectionUtils.isEmpty(values)) {
                return list;
            }
            for (byte[] value : values) {
                list.add(valueRedisSerializer.deserialize(value, String.class));
            }
            return list;
        } catch (SerializationException e) {
            throw e;
        } catch (Exception e) {
            throw new RedisClientException(e.getMessage(), e);
        }
    }

    @Override
    public Set<String> scan(String pattern) {
        Set<String> keys = new HashSet<>();
        try {
            RedisCommands<byte[], byte[]> sync = connection.sync();
            boolean finished;
            ScanCursor cursor = ScanCursor.INITIAL;
            do {
                KeyScanCursor<byte[]> scanCursor = sync.scan(cursor, ScanArgs.Builder.limit(10000).match(pattern));
                scanCursor.getKeys().forEach(key -> keys.add(getKeySerializer().deserialize(key, String.class)));
                finished = scanCursor.isFinished();
                cursor = ScanCursor.of(scanCursor.getCursor());
            } while (!finished);
        } catch (SerializationException e) {
            throw e;
        } catch (Exception e) {
            throw new RedisClientException(e.getMessage(), e);
        }
        return keys;
    }

    @Override
    public Object eval(String script, List<String> keys, List<String> args) {

        try {
            RedisCommands<byte[], byte[]> sync = connection.sync();

            List<byte[]> bkeys = keys.stream().map(key -> getKeySerializer().serialize(key)).collect(Collectors.toList());
            List<byte[]> bargs = args.stream().map(arg -> getValueSerializer().serialize(arg)).collect(Collectors.toList());
            return sync.eval(script, ScriptOutputType.INTEGER, bkeys.toArray(new byte[0][0]), bargs.toArray(new byte[0][0]));
        } catch (SerializationException e) {
            throw e;
        } catch (Exception e) {
            throw new RedisClientException(e.getMessage(), e);
        }
    }

}
