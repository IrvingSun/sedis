package com.sedis.cache.spring;

import com.sedis.cache.keytool.CacheKeyGenerator;
import com.sedis.cache.keytool.DefaultCacheKeyGenerator;
import com.sedis.cache.pipeline.CacheHandlerContext;
import org.aopalliance.intercept.MethodInvocation;

/**
 * Created by yollock on 2016/9/13.
 */
public class CacheAttributeUtils {

    private static CacheKeyGenerator keyGenerator = new DefaultCacheKeyGenerator();

    private CacheAttributeUtils() {
    }

    public static int getHandlerFlag(CacheAttribute cacheAttribute) {
        int handlerFlag = 0;
        if (cacheAttribute.getMemoryEnable()) {
            handlerFlag |= CacheHandlerContext.MEMORY_HANDLER;
        }
        if (cacheAttribute.getRedisEnable()) {
            handlerFlag |= CacheHandlerContext.REDIS_HANDLER;
        }
        if (cacheAttribute.getDataSourceEnable()) {
            handlerFlag |= CacheHandlerContext.DATASOURCE_HANDLER;
        }
        return handlerFlag;
    }

    public static String getKey(CacheAttribute cacheAttr, MethodInvocation invocation) {
        // Method method, Object[] args, Object target, String key
        return keyGenerator.generateKey(invocation.getMethod(), //
                invocation.getArguments(),//
                invocation.getThis(), //
                cacheAttr.getKey());
    }
}
