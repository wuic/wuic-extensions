/*
 * Copyright (c) 2016   The authors of WUIC
 *
 * License/Terms of Use
 * Permission is hereby granted, free of charge and for the term of intellectual
 * property rights on the Software, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to use, copy, modify and
 * propagate free of charge, anywhere in the world, all or part of the Software
 * subject to the following mandatory conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, 
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, PEACEFUL ENJOYMENT,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS
 * OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */


package com.github.wuic.engine.ehcache;

import com.github.wuic.ApplicationConfig;
import com.github.wuic.config.Alias;
import com.github.wuic.config.BooleanConfigParam;
import com.github.wuic.config.Config;
import com.github.wuic.config.ObjectConfigParam;
import com.github.wuic.engine.EngineRequest;
import com.github.wuic.engine.EngineService;
import com.github.wuic.engine.core.AbstractCacheEngine;

import com.github.wuic.engine.setter.CacheProviderClassPropertySetter;
import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

/**
 * <p>
 * This {@link com.github.wuic.engine.Engine engine} reads from a cache provided by EhCache the nuts associated to a
 * workflow to be processed.
 * </p>
 * 
 * @author Guillaume DROUET
 * @since 0.1.1
 */
@EngineService(injectDefaultToWorkflow = true)
@Alias("ehcache")
public class EhCacheEngine extends AbstractCacheEngine implements ApplicationConfig {

    /**
     * The wrapped cache.
     */
    private Cache ehCache;

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
            @BooleanConfigParam(propertyKey = CACHE, defaultValue = true)
            final Boolean work,
            @ObjectConfigParam(propertyKey = CACHE_PROVIDER_CLASS,
                    defaultValue = "com.github.wuic.engine.ehcache.DefaultEhCacheProvider",
                    setter = CacheProviderClassPropertySetter.class)
            final Cache cache,
            @BooleanConfigParam(propertyKey = BEST_EFFORT, defaultValue = false)
            final Boolean bestEffort) {
        init(work, bestEffort);
        ehCache = cache;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void putToCache(final EngineRequest.Key request, final CacheResult nuts) {
        ehCache.put(new Element(request, nuts));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeFromCache(final EngineRequest.Key request) {
        ehCache.remove(request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CacheResult getFromCache(final EngineRequest.Key request) {
        final Element el = ehCache.get(request);
        return el == null ? null : (CacheResult) el.getObjectValue();
    }
}
