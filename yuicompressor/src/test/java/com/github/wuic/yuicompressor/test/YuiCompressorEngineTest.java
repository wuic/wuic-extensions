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


package com.github.wuic.yuicompressor.test;

import com.github.wuic.EnumNutType;
import com.github.wuic.NutType;
import com.github.wuic.NutTypeFactory;
import com.github.wuic.NutTypeFactoryHolder;
import com.github.wuic.config.ObjectBuilder;
import com.github.wuic.config.ObjectBuilderFactory;
import com.github.wuic.engine.Engine;
import com.github.wuic.engine.EngineRequestBuilder;
import com.github.wuic.engine.EngineService;
import com.github.wuic.engine.yuicompressor.YuiCompressorCssEngine;
import com.github.wuic.engine.yuicompressor.YuiCompressorJavascriptEngine;
import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.nut.Nut;
import com.github.wuic.nut.NutsHeap;
import com.github.wuic.util.FutureLong;
import com.github.wuic.util.InMemoryInput;
import com.github.wuic.util.InMemoryOutput;
import com.github.wuic.util.Pipe;
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
 * {@link com.github.wuic.engine.yuicompressor.YuiCompressorCssEngine} and {@link com.github.wuic.engine.yuicompressor.YuiCompressorJavascriptEngine}
 * tests.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.0
 */
@RunWith(JUnit4.class)
public class YuiCompressorEngineTest {

    /**
     * Timeout.
     */
    @Rule
    public Timeout globalTimeout = Timeout.seconds(60);

    /**
     * Javascript compression test.
     *
     * @throws Exception if test fails
     */
    @Test
    public void javascriptTest() throws Exception {
        final Nut nut = Mockito.mock(Nut.class);
        Mockito.when(nut.getInitialName()).thenReturn("foo.js");
        Mockito.when(nut.openStream()).thenReturn(new InMemoryInput("var foo = 0; // some comments", Charset.defaultCharset().displayName()));
        Mockito.when(nut.getVersionNumber()).thenReturn(new FutureLong(0L));
        Mockito.when(nut.getInitialNutType()).thenReturn(new NutType(EnumNutType.JAVASCRIPT, Charset.defaultCharset().displayName()));

        final NutsHeap heap = Mockito.mock(NutsHeap.class);
        Mockito.when(heap.getNuts()).thenReturn(Arrays.asList(nut));

        final ObjectBuilderFactory<Engine> factory = new ObjectBuilderFactory<Engine>(EngineService.class, YuiCompressorJavascriptEngine.class);
        final ObjectBuilder<Engine> builder = factory.create("YuiCompressorJavascriptEngineBuilder");
        final Engine engine = builder.build();
        NutTypeFactoryHolder.class.cast(engine).setNutTypeFactory(new NutTypeFactory(Charset.defaultCharset().displayName()));
        final List<ConvertibleNut> res = engine.parse(new EngineRequestBuilder("wid", heap, null, new NutTypeFactory(Charset.defaultCharset().displayName())).contextPath("cp").build());
        final InMemoryOutput bos = new InMemoryOutput(Charset.defaultCharset().displayName());
        res.get(0).transform(new Pipe.DefaultOnReady(bos));
        Assert.assertEquals("var foo=0;", bos.execution().toString());
    }

    /**
     * CSS compression test.
     *
     * @throws Exception if test fails
     */
    @Test
    public void cssTest() throws Exception {
        final Nut nut = Mockito.mock(Nut.class);
        Mockito.when(nut.getInitialName()).thenReturn("foo.js");
        Mockito.when(nut.openStream()).thenReturn(new InMemoryInput(".foo { color: black;/*some comments*/ }", Charset.defaultCharset().displayName()));
        Mockito.when(nut.getVersionNumber()).thenReturn(new FutureLong(0L));
        Mockito.when(nut.getInitialNutType()).thenReturn(new NutType(EnumNutType.CSS, Charset.defaultCharset().displayName()));

        final NutsHeap heap = Mockito.mock(NutsHeap.class);
        Mockito.when(heap.getNuts()).thenReturn(Arrays.asList(nut));

        final ObjectBuilderFactory<Engine> factory = new ObjectBuilderFactory<Engine>(EngineService.class, YuiCompressorCssEngine.class);
        final ObjectBuilder<Engine> builder = factory.create("YuiCompressorCssEngineBuilder");
        final Engine engine = builder.build();
        NutTypeFactoryHolder.class.cast(engine).setNutTypeFactory(new NutTypeFactory(Charset.defaultCharset().displayName()));

        final List<ConvertibleNut> res = engine.parse(new EngineRequestBuilder("wid", heap, null, new NutTypeFactory(Charset.defaultCharset().displayName())).contextPath("cp").build());
        final InMemoryOutput bos = new InMemoryOutput(Charset.defaultCharset().displayName());
        res.get(0).transform(new Pipe.DefaultOnReady(bos));
        Assert.assertEquals(".foo{color:black}", bos.execution().toString());
    }
}
