/*
 * "Copyright (c) 2015   Capgemini Technology Services (hereinafter "Capgemini")
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


package com.github.wuic.nut.test;

import com.github.wuic.ApplicationConfig;
import com.github.wuic.NutType;
import com.github.wuic.ProcessContext;
import com.github.wuic.config.ObjectBuilderFactory;
import com.github.wuic.engine.EngineRequestBuilder;
import com.github.wuic.engine.NodeEngine;
import com.github.wuic.engine.core.TextAggregatorEngine;
import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.nut.dao.NutDao;
import com.github.wuic.nut.dao.NutDaoService;
import com.github.wuic.nut.NutsHeap;
import com.github.wuic.nut.Nut;
import com.github.wuic.nut.ByteArrayNut;
import com.github.wuic.nut.dao.gstorage.GStorageNutDao;
import com.github.wuic.config.ObjectBuilder;
import com.github.wuic.util.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.InputStream;
import java.io.InputStreamReader;
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
 * @version 1.5
 * @since 0.3.3
 */
@RunWith(JUnit4.class)
public class GStorageTest {

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
        when(nutsHeap.getNutTypes()).thenReturn(new HashSet<NutType>(Arrays.asList(NutType.CSS)));
        final List<Nut> nuts = new ArrayList<Nut>();
        nuts.add(new ByteArrayNut(array, "cloud.css", NutType.CSS, 1L, false));
        when(nutsHeap.getNuts()).thenReturn(nuts);

        final NodeEngine aggregator = new TextAggregatorEngine(true, true);

        final List<ConvertibleNut> group = aggregator.parse(new EngineRequestBuilder("", nutsHeap, null).processContext(ProcessContext.DEFAULT).build());

        Assert.assertFalse(group.isEmpty());
        InputStream is;

        for (final Nut res : group) {
            is = res.openStream();
            Assert.assertTrue(IOUtils.readString(new InputStreamReader(is)).length() > 0);
            is.close();
        }
    }
}
