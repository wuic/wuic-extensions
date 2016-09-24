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


package com.github.wuic.nodejs.test;

import com.github.wuic.ApplicationConfig;
import com.github.wuic.EnumNutType;
import com.github.wuic.NutTypeFactory;
import com.github.wuic.ProcessContext;
import com.github.wuic.config.ObjectBuilderFactory;
import com.github.wuic.context.ContextBuilder;
import com.github.wuic.engine.Engine;
import com.github.wuic.engine.EngineService;
import com.github.wuic.engine.core.CommandLineConverterEngine;
import com.github.wuic.nodejs.NodeJsInspector;
import com.github.wuic.nut.dao.core.ClasspathNutDao;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.nio.charset.Charset;

/**
 * Tests that {@link com.github.wuic.nodejs.NodeJsInspector} is correctly installed.
 */
@RunWith(JUnit4.class)
public class NodeJsInspectorTest {

    /**
     * Timeout.
     */
    @Rule
    public Timeout globalTimeout = Timeout.seconds(60);

    /**
     * <p>
     * Nominal test.
     * </p>
     *
     * @throws Exception if test fails
     */
    @Test
    public void nodeJsInspectorTest() throws Exception {
        final ObjectBuilderFactory<Engine> factory = new ObjectBuilderFactory<Engine>(EngineService.class,
                CommandLineConverterEngine.class,
                BazCommandLineConverterEngine.class,
                BarCommandLineConverterEngine.class,
                FooCommandLineConverterEngine.class);

        final NodeJsInspector inspector = new NodeJsInspector();
        ContextBuilder ctx = new ContextBuilder(factory, null, null, false, inspector);

        final String command = String.format("echo %s > %s | echo %s > %s",
                CommandLineConverterEngine.PATH_TOKEN,
                CommandLineConverterEngine.OUT_PATH_TOKEN,
                "{}",
                CommandLineConverterEngine.SOURCE_MAP_TOKEN);

        ctx.configureDefault().tag(this)
                .processContext(ProcessContext.DEFAULT)
                .contextEngineBuilder(CommandLineConverterEngine.class)
                .property(ApplicationConfig.INPUT_NUT_TYPE, EnumNutType.LESS.name())
                .property(ApplicationConfig.OUTPUT_NUT_TYPE, new NutTypeFactory(Charset.defaultCharset().displayName()).getNutType(EnumNutType.CSS).name())
                .property(ApplicationConfig.COMMAND, command)
                .toContext()
                .contextEngineBuilder(BazCommandLineConverterEngine.class)
                .property(ApplicationConfig.INPUT_NUT_TYPE, EnumNutType.LESS.name())
                .property(ApplicationConfig.OUTPUT_NUT_TYPE, new NutTypeFactory(Charset.defaultCharset().displayName()).getNutType(EnumNutType.CSS).name())
                .property(ApplicationConfig.COMMAND, command)
                .toContext()
                .contextEngineBuilder(BarCommandLineConverterEngine.class)
                .property(ApplicationConfig.INPUT_NUT_TYPE, EnumNutType.LESS.name())
                .property(ApplicationConfig.OUTPUT_NUT_TYPE, new NutTypeFactory(Charset.defaultCharset().displayName()).getNutType(EnumNutType.CSS).name())
                .property(ApplicationConfig.COMMAND, command)
                .toContext()
                .contextEngineBuilder(FooCommandLineConverterEngine.class)
                .property(ApplicationConfig.INPUT_NUT_TYPE, EnumNutType.LESS.name())
                .property(ApplicationConfig.OUTPUT_NUT_TYPE, new NutTypeFactory(Charset.defaultCharset().displayName()).getNutType(EnumNutType.CSS).name())
                .property(ApplicationConfig.COMMAND, command)
                .toContext()
                .heap("heap", ContextBuilder.getDefaultBuilderId(ClasspathNutDao.class), new String[] { "foo.js" })
                .template("tpl", new String[]{
                        ContextBuilder.getDefaultBuilderId(FooCommandLineConverterEngine.class),
                        ContextBuilder.getDefaultBuilderId(BarCommandLineConverterEngine.class),
                        ContextBuilder.getDefaultBuilderId(BazCommandLineConverterEngine.class),
                        ContextBuilder.getDefaultBuilderId(CommandLineConverterEngine.class)
                }).workflow("workflow", true, ".*", "tpl").releaseTag()
                .build();
    }
}
