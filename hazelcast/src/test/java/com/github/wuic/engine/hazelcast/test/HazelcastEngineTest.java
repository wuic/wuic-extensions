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


package com.github.wuic.engine.hazelcast.test;

import com.github.wuic.ApplicationConfig;
import com.github.wuic.EnumNutType;
import com.github.wuic.NutType;
import com.github.wuic.NutTypeFactory;
import com.github.wuic.config.ObjectBuilderFactory;
import com.github.wuic.engine.Engine;
import com.github.wuic.engine.EngineRequest;
import com.github.wuic.engine.EngineRequestBuilder;
import com.github.wuic.engine.EngineService;
import com.github.wuic.engine.EngineType;
import com.github.wuic.engine.NodeEngine;
import com.github.wuic.engine.hazelcast.HazelcastEngine;
import com.github.wuic.engine.hazelcast.WuicHazelcastProvider;
import com.github.wuic.exception.WuicException;
import com.github.wuic.config.ObjectBuilder;
import com.github.wuic.nut.HeapListener;
import com.github.wuic.nut.Nut;
import com.github.wuic.nut.NutsHeap;
import com.github.wuic.util.FutureLong;
import com.github.wuic.util.InMemoryInput;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.IMap;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>
 * {@link com.github.wuic.engine.Engine} tests.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.4.0
 */
@RunWith(JUnit4.class)
public class HazelcastEngineTest {

    /**
     * <p>
     * Mocked cache provider.
     * </p>
     *
     * @author Guillaume DROUET
     * @since 0.4.0
     */
    public static final class CacheFactory implements WuicHazelcastProvider {

        /**
         * {@inheritDoc}
         */
        @Override
        public IMap getCache() {
            return Hazelcast.newHazelcastInstance().getMap("wuicCache");
        }
    }

    /**
     * Timeout.
     */
    @Rule
    public Timeout globalTimeout = Timeout.seconds(60);

    /**
     * Counter.
     */
    private AtomicInteger count;

    /**
     * Initializes counter.
     */
    @Before
    public void init() {
        count = new AtomicInteger();
    }

    /**
     * <p>
     * Creates a mocked engine that increments a counter each time its parse method is invoked.
     * </p>
     *
     * @return the mock
     * @throws WuicException if test fails
     */
    private NodeEngine mock() throws WuicException {
        final NodeEngine mock = Mockito.mock(NodeEngine.class);
        Mockito.when(mock.getEngineType()).thenReturn(EngineType.INSPECTOR);
        Mockito.when(mock.parse(Mockito.any(EngineRequest.class))).then(new Answer<Object>() {

            /**
             * {@inheritDoc}
             */
            @Override
            public Object answer(final InvocationOnMock invocationOnMock) throws Throwable {
                count.incrementAndGet();
                return ((EngineRequest)invocationOnMock.getArguments()[0]).getNuts();
            }
        });

        return mock;
    }

    /**
     * Test that content is cached.
     *
     * @throws Exception if test fails
     */
    @Test
    public void cacheTest() throws Exception {
        final ObjectBuilderFactory<Engine> factory = new ObjectBuilderFactory<Engine>(EngineService.class, HazelcastEngine.class);
        final ObjectBuilder<Engine> builder = factory.create("HazelcastEngineBuilder");
        Assert.assertNotNull(builder);
        final Engine e = builder.build();
        final NodeEngine chain = mock();
        final Map<NutType, NodeEngine> map = new HashMap<NutType, NodeEngine>();
        map.put(new NutType(EnumNutType.CSS, Charset.defaultCharset().displayName()), chain);
        final NutsHeap heap = Mockito.mock(NutsHeap.class);
        final Nut nut = Mockito.mock(Nut.class);
        Mockito.when(nut.getInitialName()).thenReturn("foo.css");
        Mockito.when(nut.getInitialNutType()).thenReturn(new NutType(EnumNutType.CSS, Charset.defaultCharset().displayName()));
        Mockito.when(heap.getNuts()).thenReturn(Arrays.asList(nut));
        Mockito.when(nut.getVersionNumber()).thenReturn(new FutureLong(1L));
        Mockito.when(nut.openStream()).thenReturn(new InMemoryInput(new byte[0], Charset.defaultCharset().displayName()));
        e.parse(new EngineRequestBuilder("", heap, null, new NutTypeFactory(Charset.defaultCharset().displayName())).chains(map).build());
        Assert.assertEquals(1, count.get());
        e.parse(new EngineRequestBuilder("", heap, null, new NutTypeFactory(Charset.defaultCharset().displayName())).chains(map).build());
        Assert.assertEquals(1, count.get());
    }

    /**
     * Test that content is not cached.
     *
     * @throws Exception if test fails
     */
    @Test
    public void noCacheTest() throws Exception {
        final ObjectBuilderFactory<Engine> factory = new ObjectBuilderFactory<Engine>(EngineService.class, HazelcastEngine.class);
        final ObjectBuilder<Engine> builder = factory.create("HazelcastEngineBuilder");
        Assert.assertNotNull(builder);
        builder.property(ApplicationConfig.CACHE, false);
        final Engine chain = mock();
        final NutsHeap heap = Mockito.mock(NutsHeap.class);
        chain.parse(new EngineRequestBuilder("", heap, null, new NutTypeFactory(Charset.defaultCharset().displayName())).build());
        Assert.assertEquals(1, count.get());
        chain.parse(new EngineRequestBuilder("", heap, null, new NutTypeFactory(Charset.defaultCharset().displayName())).build());
        Assert.assertEquals(2, count.get());
    }

    /**
     * Test that cached content is invalidated when changes are notified.
     *
     * @throws Exception if test fails
     */
    @Test
    public void invalidateCacheTest() throws Exception {
        final Nut nut = Mockito.mock(Nut.class);
        Mockito.when(nut.getInitialNutType()).thenReturn(new NutType(EnumNutType.JAVASCRIPT, Charset.defaultCharset().displayName()));
        Mockito.when(nut.getInitialName()).thenReturn("foo.js");
        Mockito.when(nut.getVersionNumber()).thenReturn(new FutureLong(1L));
        Mockito.when(nut.openStream()).thenReturn(new InMemoryInput(new byte[0], Charset.defaultCharset().displayName()));
        final NutsHeap heap = Mockito.mock(NutsHeap.class);
        Mockito.when(heap.getNuts()).thenReturn(Arrays.asList(nut));
        final List<HeapListener> listeners = new ArrayList<HeapListener>();

        Mockito.doAnswer(new Answer() {

            /**
             * {@inheritDoc}
             */
            @Override
            public Object answer(final InvocationOnMock invocationOnMock) throws Throwable {
                listeners.add((HeapListener) invocationOnMock.getArguments()[0]);
                return null;
            }
        }).when(heap).addObserver(Mockito.any(HeapListener.class));

        final ObjectBuilderFactory<Engine> factory = new ObjectBuilderFactory<Engine>(EngineService.class, HazelcastEngine.class);
        final ObjectBuilder<Engine> builder = factory.create("HazelcastEngineBuilder");
        Assert.assertNotNull(builder);
        builder.property(ApplicationConfig.CACHE_PROVIDER_CLASS, CacheFactory.class.getName());
        final HazelcastEngine cache = (HazelcastEngine) builder.build();
        final Map<NutType, NodeEngine> map = new HashMap<NutType, NodeEngine>();
        map.put(new NutType(EnumNutType.JAVASCRIPT, Charset.defaultCharset().displayName()), mock());

        cache.parse(new EngineRequestBuilder("", heap, null, new NutTypeFactory(Charset.defaultCharset().displayName())).chains(map).build());
        Assert.assertEquals(1, count.get());
        cache.parse(new EngineRequestBuilder("", heap, null, new NutTypeFactory(Charset.defaultCharset().displayName())).chains(map).build());
        Assert.assertEquals(1, count.get());
        Assert.assertEquals(listeners.size(), 1);

        // Invalidate cache
        listeners.get(0).nutUpdated(heap);

        cache.parse(new EngineRequestBuilder("", heap, null, new NutTypeFactory(Charset.defaultCharset().displayName())).chains(map).build());
        Assert.assertEquals(2, count.get());
        cache.parse(new EngineRequestBuilder("", heap, null, new NutTypeFactory(Charset.defaultCharset().displayName())).chains(map).build());
        Assert.assertEquals(2, count.get());
    }
}
