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


package com.github.wuic.yuicompressor.test;

import com.github.wuic.NutType;
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
import com.github.wuic.util.Pipe;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.List;

/**
 * <p>
 * {@link com.github.wuic.engine.yuicompressor.YuiCompressorCssEngine} and {@link com.github.wuic.engine.yuicompressor.YuiCompressorJavascriptEngine}
 * tests.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.0
 * @since 0.5.0
 */
@RunWith(JUnit4.class)
public class YuiCompressorEngineTest {

    /**
     * Javascript compression test.
     *
     * @throws Exception if test fails
     */
    @Test
    public void javascriptTest() throws Exception {
        final Nut nut = Mockito.mock(Nut.class);
        Mockito.when(nut.getInitialName()).thenReturn("foo.js");
        Mockito.when(nut.openStream()).thenReturn(new ByteArrayInputStream("var foo = 0; // some comments".getBytes()));
        Mockito.when(nut.getVersionNumber()).thenReturn(new FutureLong(0L));
        Mockito.when(nut.isTextReducible()).thenReturn(Boolean.TRUE);
        Mockito.when(nut.getNutType()).thenReturn(NutType.JAVASCRIPT);

        final NutsHeap heap = Mockito.mock(NutsHeap.class);
        Mockito.when(heap.getNuts()).thenReturn(Arrays.asList(nut));

        final ObjectBuilderFactory<Engine> factory = new ObjectBuilderFactory<Engine>(EngineService.class, YuiCompressorJavascriptEngine.class);
        final ObjectBuilder<Engine> builder = factory.create("YuiCompressorJavascriptEngineBuilder");
        final Engine engine = builder.build();

        final List<ConvertibleNut> res = engine.parse(new EngineRequestBuilder("wid", heap).contextPath("cp").build());
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        res.get(0).transform(new Pipe.DefaultOnReady(bos));
        Assert.assertEquals("var foo=0;", new String(bos.toByteArray()));
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
        Mockito.when(nut.openStream()).thenReturn(new ByteArrayInputStream(".foo { color: black;/*some comments*/ }".getBytes()));
        Mockito.when(nut.getVersionNumber()).thenReturn(new FutureLong(0L));
        Mockito.when(nut.isTextReducible()).thenReturn(Boolean.TRUE);
        Mockito.when(nut.getNutType()).thenReturn(NutType.CSS);

        final NutsHeap heap = Mockito.mock(NutsHeap.class);
        Mockito.when(heap.getNuts()).thenReturn(Arrays.asList(nut));

        final ObjectBuilderFactory<Engine> factory = new ObjectBuilderFactory<Engine>(EngineService.class, YuiCompressorCssEngine.class);
        final ObjectBuilder<Engine> builder = factory.create("YuiCompressorCssEngineBuilder");
        final Engine engine = builder.build();

        final List<ConvertibleNut> res = engine.parse(new EngineRequestBuilder("wid", heap).contextPath("cp").build());
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        res.get(0).transform(new Pipe.DefaultOnReady(bos));
        Assert.assertEquals(".foo{color:black;}", new String(bos.toByteArray()));
    }
}
