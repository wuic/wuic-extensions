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


package com.github.wuic.test.attoparser;

import com.github.wuic.EnumNutType;
import com.github.wuic.NutType;
import com.github.wuic.NutTypeFactory;
import com.github.wuic.ProcessContext;
import com.github.wuic.Workflow;
import com.github.wuic.context.Context;
import com.github.wuic.engine.EngineRequest;
import com.github.wuic.engine.EngineRequestBuilder;
import com.github.wuic.engine.NodeEngine;
import com.github.wuic.engine.SpriteProvider;
import com.github.wuic.engine.core.BinPacker;
import com.github.wuic.engine.core.CssSpriteProvider;
import com.github.wuic.engine.core.ImageAggregatorEngine;
import com.github.wuic.engine.core.SpriteInspectorEngine;
import com.github.wuic.engine.core.TextAggregatorEngine;
import com.github.wuic.engine.core.HtmlInspectorEngine;
import com.github.wuic.exception.WorkflowNotFoundException;
import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.nut.InMemoryNut;
import com.github.wuic.nut.Nut;
import com.github.wuic.nut.dao.NutDao;
import com.github.wuic.nut.NutsHeap;
import com.github.wuic.nut.dao.core.DiskNutDao;
import com.github.wuic.util.FutureLong;
import com.github.wuic.util.InMemoryInput;
import com.github.wuic.util.InMemoryOutput;
import com.github.wuic.util.NutUtils;
import com.github.wuic.util.Pipe;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * <p>
 * Tests the {@link HtmlInspectorEngine} class.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.4.4
 */
@RunWith(JUnit4.class)
public class HtmlInspectorEngineTest {

    /**
     * The regex that should match HTML when transformed during tests.
     */
    private static final String REGEX = ".*?<link type=\"text/css\" rel=\"stylesheet\" href=\"/.*?aggregate.css\" />.*?" +
            "<script type=\"text/javascript\" src=\"/.*?aggregate.js\"></script>.*?" +
            "<link type=\"text/css\" rel=\"stylesheet\" href=\"/.*?aggregate.css\" />.*?" +
            "<img width=\"50%\" height=\"60%\" src=\".*?\\d.*?png\" />.*?" +
            "<link type=\"text/css\" rel=\"stylesheet\" href=\"/.*?aggregate.css\" />.*?" +
            "<script type=\"text/javascript\" src=\"/.*?aggregate.js\"></script>.*?" +
            "<link type=\"text/css\" rel=\"stylesheet\" href=\"/.*?aggregate.css\" />.*?" +
            "<script type=\"text/javascript\" src=\"/.*?aggregate.js\"></script>.*?";

    /**
     * Timeout.
     */
    @Rule
    public Timeout globalTimeout = Timeout.seconds(60);

    /**
     * <p>
     * Creates a {@link Context} for test purpose.
     * </p>
     *
     * @return the context
     * @throws WorkflowNotFoundException won't occurs
     * @throws IOException won't occurs
     */
    private Context newContext() throws WorkflowNotFoundException, IOException {
        final Context ctx = Mockito.mock(Context.class);
        final NutsHeap h = Mockito.mock(NutsHeap.class);
        final Nut n = Mockito.mock(Nut.class);
        Mockito.when(n.openStream()).thenReturn(new InMemoryInput("var workflow = '';", Charset.defaultCharset().displayName()));
        Mockito.when(n.getInitialNutType()).thenReturn(new NutType(EnumNutType.JAVASCRIPT, Charset.defaultCharset().displayName()));
        Mockito.when(n.getInitialName()).thenReturn("workflow.js");
        Mockito.when(n.getVersionNumber()).thenReturn(new FutureLong(1L));
        Mockito.when(h.getNuts()).thenReturn(Arrays.asList(n));
        Mockito.when(h.getId()).thenReturn("workflow");
        final Workflow workflow = new Workflow(null, new HashMap<NutType, NodeEngine>(), h);
        Mockito.when(ctx.getWorkflow(Mockito.anyString())).thenReturn(workflow);
        return ctx;
    }

    /**
     * <p>
     * Complete parse test.
     * </p>
     *
     * @throws Exception if test fails
     */
    @Test
    public void parseTest() throws Exception {
        final Context ctx = newContext();
        final DiskNutDao dao = new DiskNutDao();
        dao.init(getClass().getResource("/html").getFile(), null, -1, false, false);
        dao.init(false, true, null);
        dao.setNutTypeFactory(new NutTypeFactory(Charset.defaultCharset().displayName()));
        final NutsHeap heap = new NutsHeap(this, Arrays.asList("index.html"), dao, "heap", new NutTypeFactory(Charset.defaultCharset().displayName()));
        heap.checkFiles(ProcessContext.DEFAULT);
        final Map<NutType, NodeEngine> chains = new HashMap<NutType, NodeEngine>();

        final TextAggregatorEngine css = new TextAggregatorEngine();
        css.init(true);
        css.async(true);
        css.setNutTypeFactory(new NutTypeFactory(Charset.defaultCharset().displayName()));
        chains.put(new NutType(EnumNutType.CSS, Charset.defaultCharset().displayName()), css);

        final TextAggregatorEngine jse = new TextAggregatorEngine();
        jse.init(true);
        jse.async(true);
        jse.setNutTypeFactory(new NutTypeFactory(Charset.defaultCharset().displayName()));
        chains.put(new NutType(EnumNutType.JAVASCRIPT, Charset.defaultCharset().displayName()), jse);

        final EngineRequest request = new EngineRequestBuilder("workflow", heap, ctx, new NutTypeFactory(Charset.defaultCharset().displayName()))
                .chains(chains).processContext(ProcessContext.DEFAULT).build();
        final HtmlInspectorEngine h = new HtmlInspectorEngine();
        h.init(true, true);
        h.setNutTypeFactory(request.getNutTypeFactory());
        final List<ConvertibleNut> nuts = h.parse(request);

        Assert.assertEquals(1, nuts.size());
        final InMemoryOutput os = new InMemoryOutput(Charset.defaultCharset().displayName());
        final ConvertibleNut nut = nuts.get(0);
        nut.transform(new Pipe.DefaultOnReady(os));
        final String content = os.execution().toString();
        Assert.assertTrue(content, Pattern.compile(REGEX, Pattern.DOTALL).matcher(content).matches());
        Assert.assertNotNull(nut.getReferencedNuts());
        Assert.assertEquals(13, nut.getReferencedNuts().size());

        final ConvertibleNut js = nut.getReferencedNuts().get(11);
        Assert.assertTrue(js.getInitialNutType().isBasedOn(EnumNutType.JAVASCRIPT));
        final String script = js.openStream().execution().toString();

        Assert.assertTrue(script, script.contains("console.log(i);"));
        Assert.assertTrue(script, script.contains("i+=3"));
        Assert.assertTrue(script, script.contains("i+=4"));
    }

    /**
     * <p>
     * Tests that sequence of image are properly handled by the HTML inspector that apply a sprite inspector on it.
     * </p>
     *
     * @throws Exception if test fails
     */
    @Test
    public void imageSequenceTest() throws Exception {
        final Context ctx = newContext();
        final DiskNutDao dao = new DiskNutDao();
        dao.init(getClass().getResource("/html").getFile(), null, -1, false, false);
        dao.init(false, true, null);
        dao.setNutTypeFactory(new NutTypeFactory(Charset.defaultCharset().displayName()));
        final NutsHeap heap = new NutsHeap(this, Arrays.asList("img-sequence.html"), dao, "heap", new NutTypeFactory(Charset.defaultCharset().displayName()));
        heap.checkFiles(ProcessContext.DEFAULT);
        final Map<NutType, NodeEngine> chains = new HashMap<NutType, NodeEngine>();
        final SpriteInspectorEngine e = new SpriteInspectorEngine();
        e.init(true, new SpriteProvider[] { new CssSpriteProvider() });
        e.setNutTypeFactory(new NutTypeFactory(Charset.defaultCharset().displayName()));
        final ImageAggregatorEngine i = new ImageAggregatorEngine();
        i.init(true);
        i.init(new BinPacker<ConvertibleNut>());
        i.setNutTypeFactory(new NutTypeFactory(Charset.defaultCharset().displayName()));
        e.setNext(i);
        chains.put(new NutType(EnumNutType.PNG, Charset.defaultCharset().displayName()), e);
        final EngineRequest request = new EngineRequestBuilder("workflow", heap, ctx, new NutTypeFactory(Charset.defaultCharset().displayName())).chains(chains).processContext(ProcessContext.DEFAULT).build();
        final HtmlInspectorEngine h = new HtmlInspectorEngine();
        h.init(true, true);
        h.setNutTypeFactory(new NutTypeFactory(Charset.defaultCharset().displayName()));
        final List<ConvertibleNut> nuts = h.parse(request);
        Assert.assertEquals(1, nuts.size());
        final String html = NutUtils.readTransform(nuts.get(0));

        final int body = html.indexOf("body");
        int idx = html.indexOf("topbar-title.png", body);
        Assert.assertNotEquals(-1, idx);
        Assert.assertEquals(html, -1, html.indexOf("topbar-title.png", idx + 10));
        idx = html.indexOf("topbar-logo.png", body);
        Assert.assertNotEquals(-1, idx);
        Assert.assertEquals(html, -1, html.indexOf("topbar-logo.png", idx + 10));
    }

    /**
     * <p>
     * Tests that script content with operator does not break the code.
     * </p>
     *
     * @throws Exception if test fails
     */
    @Test
    public void checkInlineScript() throws Exception {
        final String script = "var j; for (j = 0; < 100; j++) { console.log(j);}";
        final byte[] bytes = ("<script>" + script + "</script>").getBytes();
        final HtmlInspectorEngine engine = new HtmlInspectorEngine();
        engine.init(true, true);
        engine.setNutTypeFactory(new NutTypeFactory(Charset.defaultCharset().displayName()));
        ConvertibleNut nut = new InMemoryNut(bytes, "index.html", new NutTypeFactory(Charset.defaultCharset().displayName()).getNutType(EnumNutType.HTML), 1L, true);
        final ConvertibleNut finalNut = nut;
        final NutDao dao = Mockito.mock(NutDao.class);
        Mockito.when(dao.create(Mockito.anyString(), Mockito.any(ProcessContext.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(final InvocationOnMock invocationOnMock) throws Throwable {
                final String name = invocationOnMock.getArguments()[0].toString();

                if (finalNut.getInitialName().equals(name)) {
                    return Arrays.asList(finalNut);
                }

                final Nut n = Mockito.mock(Nut.class);
                Mockito.when(n.getVersionNumber()).thenReturn(new FutureLong(1L));
                Mockito.when(n.getInitialNutType()).thenReturn(new NutType(EnumNutType.JAVASCRIPT, Charset.defaultCharset().displayName()));
                Mockito.when(n.getInitialName()).thenReturn(name);
                Mockito.when(n.openStream()).thenReturn(new InMemoryInput(new byte[0], Charset.defaultCharset().displayName()));
                return Arrays.asList(n);
            }
        });

        final NutsHeap heap = new NutsHeap(this, Arrays.asList("index.html"), dao, "heap", new NutTypeFactory(Charset.defaultCharset().displayName()));
        heap.checkFiles(ProcessContext.DEFAULT);

        List<ConvertibleNut> res = engine.parse(new EngineRequestBuilder("", heap, null, new NutTypeFactory(Charset.defaultCharset().displayName())).build());
        Assert.assertEquals(1, res.size());
        final ConvertibleNut n = res.get(0);
        n.transform(new Pipe.DefaultOnReady(new InMemoryOutput(Charset.defaultCharset().displayName())));
        Assert.assertNotNull(n.getReferencedNuts());
        Assert.assertEquals(1, n.getReferencedNuts().size());
        String content = NutUtils.readTransform(n.getReferencedNuts().get(0));
        Assert.assertTrue(content, script.equals(content));
    }
}