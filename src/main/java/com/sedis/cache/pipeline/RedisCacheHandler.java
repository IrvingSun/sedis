package com.sedis.cache.pipeline;

import com.sedis.cache.domain.MemoryCacheDto;
import com.sedis.cache.domain.RedisCacheDto;
import com.sedis.cache.spring.CacheInterceptor;
import com.sedis.util.JsonUtils;
import org.apache.log4j.Logger;
import org.codehaus.jackson.map.type.TypeFactory;
import org.codehaus.jackson.type.JavaType;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.ShardedJedisPool;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public class RedisCacheHandler implements CacheHandler {

    private static Logger logger = Logger.getLogger(RedisCacheHandler.class);

    private static final int NEXT = 3;

    private ShardedJedisPool sedisClient;

    public RedisCacheHandler() {
        super();
    }

    public RedisCacheHandler(CacheInterceptor interceptor) {
        super();
        this.sedisClient = interceptor.getSedisClient();
    }

    @Override
    public <V> V handle(CacheHandlerContext context) {
        final CacheHandler nextHandler = context.getHandlers().get(NEXT);
        if ((context.getHandlerFlag() & CacheHandlerContext.REDIS_HANDLER) == 0) {
            return nextHandler.handle(context);
        }
        if (sedisClient == null) {
            logger.warn("Redis访问层有效,但redis客户端对象为null,将从下一层获取数据");
            return nextHandler.handle(context);
        }
        final String key = context.getKey();
        ShardedJedis jedis = null;
        try {
            jedis = sedisClient.getResource();
            if (jedis == null) {
                logger.warn("Redis访问层有效,从redis客户端获取的连接为null,将从下一层获取数据");
                return nextHandler.handle(context);
            }

            RedisCacheDto rcd = this.getFromRedisAndConvert(jedis, key);
            V result;
            if (rcd == null || System.currentTimeMillis() > rcd.getEt()) {
                logger.info("从redis获取的数据,为空或者失效,从下一层获取数据, key = " + key);
                result = nextHandler.handle(context);
                if (result == null) {
                    return null;
                }
                rcd = new RedisCacheDto();
                rcd.setKey(key);
                rcd.setJson(JsonUtils.beanToJson(result));
                parseAndFillValueType(rcd, result);
                rcd.setEt(System.currentTimeMillis() + context.getCacheAttribute().getRedisExpiredTime());
            } else {
                result = (V) rcd.getVal();
            }
            rcd.getHt().incrementAndGet();
            rcd.setVal(null);
            jedis.set(key, JsonUtils.beanToJson(rcd));
            rcd.setVal(result);
            return (V) rcd.getVal();
        } catch (Throwable t) {
            logger.error("RedisCacheHandlerError, the context is " + JsonUtils.beanToJson(context), t);
            t.printStackTrace();
        } finally {
            try {
                sedisClient.returnResource(jedis);
            } catch (Throwable t) {
            }
        }
        return null;
    }

    /**
     * 从redis中获取key对应的值，并转换成具体的对象
     * 1.list直接转换LinkedHashMap,直接返回
     * 2.array会转换成LinkedHashMap
     * 3.Map直接转换,直接返回
     * 4.原生类型,比如String,
     */
    private <V> RedisCacheDto<V> getFromRedisAndConvert(ShardedJedis jedis, String key) {
        RedisCacheDto<V> rcd = null;
        try {
            final String rcdJson = jedis.get(key);
            if (rcdJson == null || rcdJson.trim().isEmpty()) {
                return null;
            }
            rcd = JsonUtils.jsonToBean(rcdJson, RedisCacheDto.class);
            final int type = rcd.getType();
            TypeFactory typeFactory = TypeFactory.defaultInstance();
            String json = rcd.getJson();
            JavaType javaType = null;
            if (type == 0) { // element
                javaType = typeFactory.constructType(rcd.getEc());
                rcd.setVal((V) JsonUtils.jsonToBean(json, javaType));
                return rcd;
            } else if (type == 1) { // array
                javaType = typeFactory.constructArrayType(rcd.getEc());
            } else if (type == 2) { // collection
                javaType = typeFactory.constructCollectionType(rcd.getCc(), rcd.getEc());
            } else if (type == 3) { // map
                javaType = typeFactory.constructMapType(rcd.getMc(), rcd.getMkc(), rcd.getEc());
            }
            if (json.length() <= 0 || javaType == null) {
                return rcd;
            }
            rcd.setVal((V) JsonUtils.jsonToBean(json, javaType));
        } catch (Throwable t) {
            rcd = null;
            logger.error("RedisCacheHandlerConvertError, the key is " + key, t);
            t.printStackTrace();
        }
        return rcd;
    }

    private void parseAndFillValueType(RedisCacheDto rcd, Object value) {
        rcd.setEc(null);
        rcd.setCc(null);
        rcd.setMc(null);
        rcd.setMkc(null);
        rcd.setType(0);
        if (value == null) {
            return;
        }
        if (value.getClass().isArray()) {
            rcd.setType(1);
            Object[] arrayValue = (Object[]) value;
            if (arrayValue.length > 0) {
                rcd.setEc(arrayValue[0].getClass());
            }
            return;
        } else if (value instanceof Collection) {
            rcd.setType(2);
            Collection collectionValue = (Collection) value;
            if (collectionValue.size() > 0) {
                rcd.setCc(collectionValue.getClass());
                rcd.setEc(collectionValue.iterator().next().getClass());
            }
            return;
        } else if (value instanceof Map) {
            rcd.setType(3);
            Map mapValue = (Map) value;
            if (mapValue.size() > 0) {
                Map.Entry entry = (Map.Entry) mapValue.entrySet().iterator().next();
                rcd.setMc(mapValue.getClass());
                rcd.setMkc(entry.getKey().getClass());
                rcd.setEc(entry.getValue().getClass());
            }
            return;
        } else {
            rcd.setEc(value.getClass());
        }
    }

}
