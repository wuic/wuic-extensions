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


package com.github.wuic.webjars;

import com.github.wuic.ApplicationConfig;
import com.github.wuic.EnumNutType;
import com.github.wuic.NutTypeFactory;
import com.github.wuic.ProcessContext;
import com.github.wuic.config.ObjectBuilderFactory;
import com.github.wuic.context.Context;
import com.github.wuic.context.ContextBuilder;
import com.github.wuic.engine.Engine;
import com.github.wuic.engine.EngineService;
import com.github.wuic.engine.core.CommandLineConverterEngine;
import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.nut.Nut;
import com.github.wuic.nut.dao.core.ClasspathNutDao;
import com.github.wuic.nut.dao.webjars.WebJarNutDao;
import com.github.wuic.util.IOUtils;
import com.github.wuic.util.NutUtils;
import com.github.wuic.util.UrlUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.webjars.WebJarAssetLocator;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * Tests for {@link WebJarNutDao}.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.3
 */
@RunWith(JUnit4.class)
public class WebJarsTest {

    /**
     * <p>
     * Simple webjar detection.
     * </p>
     */
    @Test
    public void simpleWebJarTest() throws IOException {
        final WebJarNutDao dao = new WebJarNutDao();
        dao.init("/", null, false, false);
        dao.init(false, true, "");
        dao.setNutTypeFactory(new NutTypeFactory(Charset.defaultCharset().displayName()));
        dao.setWebJarAssetLocator(new WebJarAssetLocator());
        Assert.assertEquals(1, assertOpenStream(dao.create("angular.js", ProcessContext.DEFAULT)).size());
    }

    /**
     * <p>
     * Regex webjar detection.
     * </p>
     */
    @Test
    public void regexWebJarTest() throws IOException {
        WebJarNutDao dao = new WebJarNutDao();
        dao.init("/", null, true, false);
        dao.init(false, true, "");
        dao.setWebJarAssetLocator(new WebJarAssetLocator());
        dao.setNutTypeFactory(new NutTypeFactory(Charset.defaultCharset().displayName()));

        Assert.assertEquals(442, assertOpenStream(dao.create(".*.js", ProcessContext.DEFAULT)).size());
        Assert.assertEquals(279, assertOpenStream(dao.create("i18n/.*.js", ProcessContext.DEFAULT)).size());

        dao = new WebJarNutDao();
        dao.init("/i18n", null, true, false);
        dao.init(false, true, "");
        dao.setWebJarAssetLocator(new WebJarAssetLocator());
        dao.setNutTypeFactory(new NutTypeFactory(Charset.defaultCharset().displayName()));
        Assert.assertEquals(279, assertOpenStream(dao.create(".*.js", ProcessContext.DEFAULT)).size());
    }

    /**
     * <p>
     * Wildcard webjar detection.
     * </p>
     */
    @Test
    public void wildcardWebJarTest() throws IOException {
        WebJarNutDao dao = new WebJarNutDao();
        dao.init("/", null, false, true);
        dao.init(false, true, "");
        dao.setWebJarAssetLocator(new WebJarAssetLocator());
        dao.setNutTypeFactory(new NutTypeFactory(Charset.defaultCharset().displayName()));
        Assert.assertEquals(442, assertOpenStream(dao.create("*.js", ProcessContext.DEFAULT)).size());
        Assert.assertEquals(279, assertOpenStream(dao.create("i18n/*.js", ProcessContext.DEFAULT)).size());

        dao = new WebJarNutDao();
        dao.init("/i18n", null, false, true);
        dao.init(false, true, "");
        dao.setWebJarAssetLocator(new WebJarAssetLocator());
        dao.setNutTypeFactory(new NutTypeFactory(Charset.defaultCharset().displayName()));
        Assert.assertEquals(279, assertOpenStream(dao.create("*.js", ProcessContext.DEFAULT)).size());
    }

    /**
     * <p>
     * Tests the command execution with node_modules.
     * </p>
     *
     * @throws Exception if test fails
     */
    @Test
    public void commandTest() throws Exception {
        final ObjectBuilderFactory<Engine> factory =
                new ObjectBuilderFactory<Engine>(EngineService.class, CommandLineConverterEngine.class);

        final WebJarInspector inspector = new WebJarInspector();
        ContextBuilder ctx = new ContextBuilder(factory, null, null, false, inspector);
        final String sourceMap = "{"
                + "  \"version\": 3,"
                + "  \"file\": \"testcode.js\","
                + "  \"sections\": ["
                + "    {"
                + "      \"map\": {"
                + "         \"version\": 3,"
                + "         \"mappings\": \"AAAAA,QAASA,UAAS,EAAG;\","
                + "         \"sources\": [\"testcode.js\"],"
                + "         \"names\": [\"foo\"]"
                + "      },"
                + "      \"offset\": {"
                + "        \"line\": 1,"
                + "        \"column\": 1"
                + "      }"
                + "    }"
                + "  ]"
                + "}";

        final String command = String.format("echo %s > %s | echo %s > %s",
                CommandLineConverterEngine.PATH_TOKEN,
                CommandLineConverterEngine.OUT_PATH_TOKEN,
                sourceMap,
                CommandLineConverterEngine.SOURCE_MAP_TOKEN);

        final Context c = ctx.configureDefault().tag(this)
                .processContext(ProcessContext.DEFAULT)
                .contextEngineBuilder(CommandLineConverterEngine.class)
                .property(ApplicationConfig.INPUT_NUT_TYPE, EnumNutType.JAVASCRIPT.name())
                .property(ApplicationConfig.OUTPUT_NUT_TYPE, new NutTypeFactory(Charset.defaultCharset().displayName()).getNutType(EnumNutType.JSX).name())
                .property(ApplicationConfig.COMMAND, command)
                .toContext()
                .heap("heap", ContextBuilder.getDefaultBuilderId(ClasspathNutDao.class), new String[] { "foo.js" })
                .template("tpl", new String[]{
                        ContextBuilder.getDefaultBuilderId(CommandLineConverterEngine.class)
                }).workflow("workflow", true, ".*", "tpl").releaseTag()
                .build();

        final List<ConvertibleNut> r = c.process("", "workflowheap", UrlUtils.urlProviderFactory(), ProcessContext.DEFAULT);
        Assert.assertNotNull(r);
        Assert.assertEquals(1, r.size());
        final String str = NutUtils.readTransform(r.get(0));
        final File file = new File(new File(str.substring(0, str.indexOf(" "))).getParentFile(), "node_modules");
        Assert.assertTrue(file.getAbsolutePath(), file.exists());
        final List<String> assets = new ArrayList<String>();
        final WebJarAssetLocator locator = new WebJarAssetLocator();

        for (final String asset : locator.listAssets("angular-dynamic-locale")) {
            assets.add(asset.substring(NpmWebJarTranslator.PREFIX.length())
                    .replace("/0.1.32/", "/"));
        }

        // package.json available
        for (final String asset : locator.listAssets("angular__common")) {
            assets.add(asset.substring(NpmWebJarTranslator.PREFIX.length())
                    .replace("angular__common", "@angular/common")
                    .replace("/2.0.0-rc.5/", "/"));
        }

        for (final String asset : assets) {
            final File f = new File(file, asset);
            Assert.assertTrue(f.getAbsolutePath(), f.exists());
        }
    }

    /**
     * <p>
     * Asserts that each stream of the given nuts can be read.
     * </p>
     *
     * @param nuts the nuts to read
     * @return the read nuts
     * @throws IOException if any I/O error occurs when reading
     */
    private List<Nut> assertOpenStream(final List<Nut> nuts) throws IOException {
        for (final Nut cn : nuts) {
            InputStream is = null;

            try {
                is = cn.openStream().inputStream();
                Assert.assertNotNull(is);
                Assert.assertNotEquals(0, IOUtils.copyStream(is, new ByteArrayOutputStream()));
            } finally {
                IOUtils.close(is);
            }
        }

        return nuts;
    }
}
