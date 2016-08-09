/*
 * "Copyright (c) 2016   Capgemini Technology Services (hereinafter "Capgemini")
 *
 * License/Terms of Use
 * Permission is hereby granted, free of charge and for the term of intellectual
 * property rights on the Software, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to use, copy, modify and
 * propagate free of charge, anywhere in the world, all or part of the Software
 * subject to the following mandatory conditions:
 *
 * -   The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * Any failure to comply with the above shall automatically terminate the license
 * and be construed as a breach of these Terms of Use causing significant harm to
 * Capgemini.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, PEACEFUL ENJOYMENT,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS
 * OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * Except as contained in this notice, the name of Capgemini shall not be used in
 * advertising or otherwise to promote the use or other dealings in this Software
 * without prior written authorization from Capgemini.
 *
 * These Terms of Use are subject to French law.
 *
 * IMPORTANT NOTICE: The WUIC software implements software components governed by
 * open source software licenses (BSD and Apache) of which CAPGEMINI is not the
 * author or the editor. The rights granted on the said software components are
 * governed by the specific terms and conditions specified by Apache 2.0 and BSD
 * licenses."
 */


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