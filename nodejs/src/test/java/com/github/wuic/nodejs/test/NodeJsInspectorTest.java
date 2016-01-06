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


package com.github.wuic.nodejs.test;

import com.github.wuic.ApplicationConfig;
import com.github.wuic.NutType;
import com.github.wuic.ProcessContext;
import com.github.wuic.config.ObjectBuilderFactory;
import com.github.wuic.context.ContextBuilder;
import com.github.wuic.engine.Engine;
import com.github.wuic.engine.EngineService;
import com.github.wuic.engine.core.CommandLineConverterEngine;
import com.github.wuic.nodejs.NodeJsInspector;
import com.github.wuic.nut.dao.core.ClasspathNutDao;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests that {@link com.github.wuic.nodejs.NodeJsInspector} is correctly installed.
 */
@RunWith(JUnit4.class)
public class NodeJsInspectorTest {

    /**
     * <p>
     * Nominal test.
     * </p>
     *
     * @throws Exception if test fails
     */
    @Test(timeout = 60000)
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
                .property(ApplicationConfig.INPUT_NUT_TYPE, NutType.LESS.name())
                .property(ApplicationConfig.OUTPUT_NUT_TYPE, NutType.CSS.name())
                .property(ApplicationConfig.COMMAND, command)
                .toContext()
                .contextEngineBuilder(BazCommandLineConverterEngine.class)
                .property(ApplicationConfig.INPUT_NUT_TYPE, NutType.LESS.name())
                .property(ApplicationConfig.OUTPUT_NUT_TYPE, NutType.CSS.name())
                .property(ApplicationConfig.COMMAND, command)
                .toContext()
                .contextEngineBuilder(BarCommandLineConverterEngine.class)
                .property(ApplicationConfig.INPUT_NUT_TYPE, NutType.LESS.name())
                .property(ApplicationConfig.OUTPUT_NUT_TYPE, NutType.CSS.name())
                .property(ApplicationConfig.COMMAND, command)
                .toContext()
                .contextEngineBuilder(FooCommandLineConverterEngine.class)
                .property(ApplicationConfig.INPUT_NUT_TYPE, NutType.LESS.name())
                .property(ApplicationConfig.OUTPUT_NUT_TYPE, NutType.CSS.name())
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