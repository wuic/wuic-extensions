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


package com.github.wuic.engine.htmlcompressor.test;

import com.github.wuic.EnumNutType;
import com.github.wuic.NutTypeFactory;
import com.github.wuic.NutTypeFactoryHolder;
import com.github.wuic.engine.EngineRequestBuilder;
import com.github.wuic.engine.htmlcompressor.HtmlCompressorEngine;
import com.github.wuic.ApplicationConfig;
import com.github.wuic.config.ObjectBuilder;
import com.github.wuic.config.ObjectBuilderFactory;
import com.github.wuic.engine.Engine;
import com.github.wuic.engine.EngineService;
import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.nut.Nut;
import com.github.wuic.nut.NutsHeap;
import com.github.wuic.util.DefaultInput;
import com.github.wuic.util.FutureLong;
import com.github.wuic.util.NutUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

/**
 * <p>
 * HtmlCompressor support test.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.0
 */
@RunWith(JUnit4.class)
public class HtmlCompressorEngineTest {

    /**
     * Timeout.
     */
    @Rule
    public Timeout globalTimeout = Timeout.seconds(60);

    /**
     * <p>
     * Test when compressor is enabled.
     * </p>
     *
     * @throws Exception if test fails
     */
    @Test
    public void htmlCompressTest() throws Exception {
        final Nut nut = Mockito.mock(Nut.class);
        Mockito.when(nut.getInitialNutType()).thenReturn(new NutTypeFactory(Charset.defaultCharset().displayName()).getNutType(EnumNutType.HTML));
        Mockito.when(nut.getInitialName()).thenReturn("index.html");
        Mockito.when(nut.openStream()).thenReturn(new DefaultInput(HtmlCompressorEngineTest.class.getResourceAsStream("/htmlcompressor/index.html"), Charset.defaultCharset().displayName()));
        Mockito.when(nut.getVersionNumber()).thenReturn(new FutureLong(0L));

        final NutsHeap heap = Mockito.mock(NutsHeap.class);
        Mockito.when(heap.getNuts()).thenReturn(Arrays.asList(nut));

        final ObjectBuilderFactory<Engine> factory = new ObjectBuilderFactory<Engine>(EngineService.class, HtmlCompressorEngine.class);
        final ObjectBuilder<Engine> builder = factory.create("HtmlCompressorEngineBuilder");
        final Engine engine = builder.build();
        final NutTypeFactory nutTypeFactory = new NutTypeFactory(Charset.defaultCharset().displayName());
        NutTypeFactoryHolder.class.cast(engine).setNutTypeFactory(nutTypeFactory);

        final List<ConvertibleNut> res = engine.parse(new EngineRequestBuilder("wid", heap, null, nutTypeFactory).contextPath("cp").build());
        Assert.assertEquals(-1, NutUtils.readTransform(res.get(0)).indexOf('\n'));
    }

    /**
     * <p>
     * Test when compressor is disabled.
     * </p>
     *
     * @throws Exception if test fails
     */
    @Test
    public void disableHtmlCompressTest() throws Exception {
        final Nut nut = Mockito.mock(Nut.class);
        Mockito.when(nut.getInitialNutType()).thenReturn(new NutTypeFactory(Charset.defaultCharset().displayName()).getNutType(EnumNutType.HTML));
        Mockito.when(nut.getInitialName()).thenReturn("index.html");
        Mockito.when(nut.openStream()).thenReturn(new DefaultInput(HtmlCompressorEngineTest.class.getResourceAsStream("/htmlcompressor/index.html"), Charset.defaultCharset().displayName()));
        Mockito.when(nut.getVersionNumber()).thenReturn(new FutureLong(0L));

        final NutsHeap heap = Mockito.mock(NutsHeap.class);
        Mockito.when(heap.getNuts()).thenReturn(Arrays.asList(nut));

        final ObjectBuilderFactory<Engine> factory = new ObjectBuilderFactory<Engine>(EngineService.class, HtmlCompressorEngine.class);
        final ObjectBuilder<Engine> builder = factory.create("HtmlCompressorEngineBuilder");
        final Engine engine = builder.property(ApplicationConfig.COMPRESS, false).build();

        final NutTypeFactory nutTypeFactory = new NutTypeFactory(Charset.defaultCharset().displayName());
        NutTypeFactoryHolder.class.cast(engine).setNutTypeFactory(nutTypeFactory);
        final List<ConvertibleNut> res = engine.parse(new EngineRequestBuilder("wid", heap, null, nutTypeFactory).contextPath("cp").build());
        Assert.assertNotSame(-1, res.get(0).openStream().execution().toString().indexOf('\n'));
    }
}
