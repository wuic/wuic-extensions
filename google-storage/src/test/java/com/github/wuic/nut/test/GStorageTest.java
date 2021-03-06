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


package com.github.wuic.nut.test;

import com.github.wuic.ApplicationConfig;
import com.github.wuic.EnumNutType;
import com.github.wuic.NutType;
import com.github.wuic.NutTypeFactory;
import com.github.wuic.ProcessContext;
import com.github.wuic.config.ObjectBuilderFactory;
import com.github.wuic.engine.EngineRequestBuilder;
import com.github.wuic.engine.core.TextAggregatorEngine;
import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.nut.InMemoryNut;
import com.github.wuic.nut.dao.NutDao;
import com.github.wuic.nut.dao.NutDaoService;
import com.github.wuic.nut.NutsHeap;
import com.github.wuic.nut.Nut;
import com.github.wuic.nut.dao.gstorage.GStorageNutDao;
import com.github.wuic.config.ObjectBuilder;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.nio.charset.Charset;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Arrays;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * <p>
 * Tests for Google Storage module.
 * </p>
 *
 * @author Corentin AZELART
 * @since 0.3.3
 */
@RunWith(JUnit4.class)
public class GStorageTest {

    /**
     * Timeout.
     */
    @Rule
    public Timeout globalTimeout = Timeout.seconds(60);
    
    /**
     * <p>
     * Test builder.
     * </p>
     *
     * @throws Exception if test fails
     */
    @Test
    public void builderTest() throws Exception {
        final ObjectBuilderFactory<NutDao> factory = new ObjectBuilderFactory<NutDao>(NutDaoService.class, NutDaoService.DEFAULT_SCAN_PACKAGE);
        final ObjectBuilder<NutDao> builder = factory.create("GStorageNutDaoBuilder");
        Assert.assertNotNull(builder);

        builder.property(ApplicationConfig.CLOUD_BUCKET, "bucket")
                .property(ApplicationConfig.LOGIN, "login")
                .property(ApplicationConfig.PASSWORD, "password")
                .build();
    }

    /**
     * <p>
     * Test builder.
     * </p>
     *
     */
    @Test(expected = IllegalArgumentException.class)
    public void builderWithBadPropertyTest() {
        final ObjectBuilderFactory<NutDao> factory = new ObjectBuilderFactory<NutDao>(NutDaoService.class, GStorageNutDao.class);
        final ObjectBuilder<NutDao> builder = factory.create("GStorageNutDaoBuilder");
        Assert.assertNotNull(builder);
        builder.property("foo", "value");
    }

    /**
     * <p>
     * Tests the Google Storage access.
     * </p>
     */
    @Test
    public void gStorageTest() throws Exception {
        final NutsHeap nutsHeap = mock(NutsHeap.class);
        final byte[] array = ".cloud { text-align : justify;}".getBytes();
        when(nutsHeap.getNutTypes()).thenReturn(new HashSet<NutType>(Arrays.asList(new NutType(EnumNutType.CSS, Charset.defaultCharset().displayName()))));
        final List<Nut> nuts = new ArrayList<Nut>();
        nuts.add(new InMemoryNut(array, "cloud.css", new NutTypeFactory(Charset.defaultCharset().displayName()).getNutType(EnumNutType.CSS), 1L, false));
        when(nutsHeap.getNuts()).thenReturn(nuts);

        final TextAggregatorEngine aggregator = new TextAggregatorEngine();
        aggregator.init(true);
        aggregator.async(true);
        aggregator.setNutTypeFactory(new NutTypeFactory(Charset.defaultCharset().displayName()));

        final List<ConvertibleNut> group = aggregator.parse(new EngineRequestBuilder("", nutsHeap, null, new NutTypeFactory(Charset.defaultCharset().displayName()))
                .processContext(ProcessContext.DEFAULT).build());

        Assert.assertFalse(group.isEmpty());

        for (final Nut res : group) {
            Assert.assertTrue(res.openStream().execution().toString().length() > 0);
        }
    }
}
