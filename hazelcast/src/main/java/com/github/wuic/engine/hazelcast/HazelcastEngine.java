package com.github.wuic.engine.hazelcast;

import com.github.wuic.ApplicationConfig;
import com.github.wuic.config.Alias;
import com.github.wuic.config.BooleanConfigParam;
import com.github.wuic.config.Config;
import com.github.wuic.config.ObjectConfigParam;
import com.github.wuic.engine.EngineRequest;
import com.github.wuic.engine.EngineService;
import com.github.wuic.engine.core.AbstractCacheEngine;
import com.github.wuic.engine.setter.CacheProviderClassPropertySetter;
import com.hazelcast.core.IMap;

/**
 * <p>
 * This {@link com.github.wuic.engine.Engine engine} reads from a cache provided by Hazelcast the nuts associated to a
 * workflow to be processed.
 * </p>
 *
 * @author Corentin AZELART
 * @author Guillaume DROUET
 * @since 0.5.3
 */
@EngineService(injectDefaultToWorkflow = true)
@Alias("hazelcast")
public class HazelcastEngine extends AbstractCacheEngine {

    /**
     * The Hazelcast cache.
     */
    private IMap<EngineRequest.Key, CacheResult> hazelcastCache;

    /**
     * <p>
     * Initializes a new engine.
     * </p>
     *
     * @param work if cache should be activated or not
     * @param cache the cache to be wrapped
     * @param bestEffort enable best effort mode or not
     */
    @Config
    public void init(
            @BooleanConfigParam(propertyKey = ApplicationConfig.CACHE, defaultValue = true)
            final Boolean work,
            @ObjectConfigParam(propertyKey = ApplicationConfig.CACHE_PROVIDER_CLASS,
                    defaultValue = "com.github.wuic.engine.hazelcast.DefaultHazelcastProvider",
                    setter = CacheProviderClassPropertySetter.class)
            final IMap<EngineRequest.Key, CacheResult> cache,
            @BooleanConfigParam(propertyKey = ApplicationConfig.BEST_EFFORT, defaultValue = false)
            final Boolean bestEffort) {
        init(work, bestEffort);
        hazelcastCache = cache;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void putToCache(final EngineRequest.Key request, final CacheResult nuts) {
        hazelcastCache.put(request, nuts);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeFromCache(final EngineRequest.Key request) {
        hazelcastCache.evict(request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CacheResult getFromCache(final EngineRequest.Key request) {
        return hazelcastCache.get(request);
    }
}