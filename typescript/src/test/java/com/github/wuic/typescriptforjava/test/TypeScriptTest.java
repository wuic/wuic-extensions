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


package com.github.wuic.typescriptforjava.test;

import com.github.wuic.EnumNutType;
import com.github.wuic.NutTypeFactory;
import com.github.wuic.NutTypeFactoryHolder;
import com.github.wuic.ProcessContext;
import com.github.wuic.exception.WuicException;
import com.github.wuic.util.IOUtils;
import com.github.wuic.util.InMemoryInput;
import com.github.wuic.util.InMemoryOutput;
import com.github.wuic.util.Output;
import org.junit.Rule;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.wuic.config.ObjectBuilder;
import com.github.wuic.config.ObjectBuilderFactory;
import com.github.wuic.engine.Engine;
import com.github.wuic.engine.EngineRequestBuilder;
import com.github.wuic.engine.EngineService;
import com.github.wuic.engine.typescript.TypeScriptConverterEngine;
import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.nut.Nut;
import com.github.wuic.nut.NutsHeap;
import com.github.wuic.util.FutureLong;
import com.github.wuic.util.Pipe;
import org.junit.Assert;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

/**
 * <p>
 * Tests for typescript compilation support.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.0
 */
@RunWith(JUnit4.class)
public class TypeScriptTest {

    /**
     * Temporary folder.
     */
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    /**
     * Timeout.
     */
    @Rule
    public Timeout globalTimeout = Timeout.seconds(60);

    /**
     * Logger.
     */
    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Some TS code to compile.
     */
    public static final String TS1 = "interface Person {\n" +
            "    firstname: string;\n" +
            "    lastname: string;\n" +
            "}\n" +
            "function greeter(person : Person) {\n" +
            "    return \"Hello, \" + person.firstname + \" \" + person.lastname;\n" +
            "}\n";

    /**
     * Some TS code to compile.
     */
    public static final String TS2 = "var user = {firstname: \"Jane\", lastname: \"User\"};\n" +
            "\n" +
            "document.body.innerHTML = greeter(user);";

    /**
     * Expected compilation result.
     */
    public static final String JS = "function greeter(person) {" +
            "    return \"Hello, \" + person.firstname + \" \" + person.lastname;" +
            "}" +
            "var user = { firstname: \"Jane\", lastname: \"User\" };" +
            "document.body.innerHTML = greeter(user);" +
            "//# sourceMappingURL=aggregate.js.map";

    /**
     * Typescript compilation test.
     *
     * @throws Exception if test fails
     */
    @Test
    public void compileTest() throws Exception {
        final File parent = temporaryFolder.newFolder("parent");
        NutsHeap heap = mockHeap(parent);
        final ObjectBuilderFactory<Engine> factory = new ObjectBuilderFactory<Engine>(EngineService.class, TypeScriptConverterEngine.class);
        final ObjectBuilder<Engine> builder = factory.create("TypeScriptConverterEngineBuilder");
        final Engine engine = builder.build();
        NutTypeFactoryHolder.class.cast(engine).setNutTypeFactory(new NutTypeFactory(Charset.defaultCharset().displayName()));

        long start = System.currentTimeMillis();
        List<ConvertibleNut> res = engine.parse(new EngineRequestBuilder("wid", heap, null, new NutTypeFactory(Charset.defaultCharset().displayName()))
                .processContext(ProcessContext.DEFAULT).contextPath("cp").build());
        Output bos = new InMemoryOutput(Charset.defaultCharset().displayName());
        res.get(0).transform(new Pipe.DefaultOnReady(bos));
        logger.info("First compilation run in {}ms", System.currentTimeMillis() - start);
        start = System.currentTimeMillis();

        heap = mockHeap(parent);
        res = engine.parse(new EngineRequestBuilder("wid", heap, null, new NutTypeFactory(Charset.defaultCharset().displayName()))
                .processContext(ProcessContext.DEFAULT).contextPath("cp").build());
        bos = new InMemoryOutput(Charset.defaultCharset().displayName());
        res.get(0).transform(new Pipe.DefaultOnReady(bos));
        logger.info("Second compilation run in {}ms", System.currentTimeMillis() - start);

        final BufferedReader br = new BufferedReader(new StringReader(bos.execution().toString()));
        String s;
        StringBuilder sb = new StringBuilder();

        // Evict new lines
        while ((s = br.readLine()) != null) {
            sb.append(s);
        }

        Assert.assertEquals(JS, sb.toString());
    }

    /**
     * Bad Typescript compilation test.
     *
     * @throws IOException if test succeed
     * @throws com.github.wuic.exception.WuicException if test fails
     */
    @Test(expected = IOException.class)
    public void compileErrorTest() throws IOException, WuicException {
        final Nut nut = mock(Nut.class);
        when(nut.getInitialName()).thenReturn("foo.ts");
        when(nut.openStream()).thenReturn(new InMemoryInput(TS1.replace("{", ""), Charset.defaultCharset().displayName()));

        // Value must be different for each test
        when(nut.getVersionNumber()).thenReturn(new FutureLong(1L));
        when(nut.getInitialNutType()).thenReturn(new NutTypeFactory(Charset.defaultCharset().displayName()).getNutType(EnumNutType.TYPESCRIPT));

        final NutsHeap heap = mock(NutsHeap.class);
        when(heap.getNuts()).thenReturn(Arrays.asList(nut));

        final ObjectBuilderFactory<Engine> factory = new ObjectBuilderFactory<Engine>(EngineService.class, TypeScriptConverterEngine.class);
        final ObjectBuilder<Engine> builder = factory.create("TypeScriptConverterEngineBuilder");
        final Engine engine = builder.build();
        NutTypeFactoryHolder.class.cast(engine).setNutTypeFactory(new NutTypeFactory(Charset.defaultCharset().displayName()));

        List<ConvertibleNut> res = engine.parse(new EngineRequestBuilder("wid", heap, null, new NutTypeFactory(Charset.defaultCharset().displayName()))
                .processContext(ProcessContext.DEFAULT).contextPath("cp").build());
        res.get(0).transform(new Pipe.DefaultOnReady(new InMemoryOutput(Charset.defaultCharset().displayName())));
    }

    /**
     * <p>
     * Mocks a new heap.
     * </p>
     *
     * @param parent where nut could be stored
     * @throws Exception if mock fails
     * @return the mock
     */
    private NutsHeap mockHeap(final File parent) throws Exception {
        final Nut nut1 = mock(Nut.class);
        when(nut1.getInitialName()).thenReturn("foo.ts");
        when(nut1.openStream()).thenReturn(new InMemoryInput(TS1, Charset.defaultCharset().displayName()));

        // Value must be different for each test
        when(nut1.getVersionNumber()).thenReturn(new FutureLong(0L));
        when(nut1.getInitialNutType()).thenReturn(new NutTypeFactory(Charset.defaultCharset().displayName()).getNutType(EnumNutType.TYPESCRIPT));

        IOUtils.copyStream(new ByteArrayInputStream(TS2.getBytes()), new FileOutputStream(new File(parent, "bar.ts")));
        final Nut nut2 = mock(Nut.class);
        when(nut2.getParentFile()).thenReturn(parent.getAbsolutePath());
        when(nut2.getInitialName()).thenReturn("bar.ts");
        when(nut2.openStream()).thenReturn(new InMemoryInput(TS2, Charset.defaultCharset().displayName()));
        when(nut2.getVersionNumber()).thenReturn(new FutureLong(0L));
        when(nut2.getInitialNutType()).thenReturn(new NutTypeFactory(Charset.defaultCharset().displayName()).getNutType(EnumNutType.TYPESCRIPT));

        final NutsHeap heap = mock(NutsHeap.class);
        when(heap.getNuts()).thenReturn(Arrays.asList(nut1, nut2));

        return heap;
    }
}
