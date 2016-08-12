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

import com.github.wuic.config.ObjectBuilderInspector;
import com.github.wuic.engine.EngineRequest;
import com.github.wuic.engine.core.CommandLineConverterEngine;
import com.github.wuic.engine.core.ExecutorHolder;
import com.github.wuic.exception.WuicException;
import com.github.wuic.nut.dao.webjars.WebJarNutDao;
import com.github.wuic.util.BiFunction;
import com.github.wuic.util.IOUtils;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webjars.WebJarAssetLocator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * This inspector decorates any created {@link ExecutorHolder} with an executor that installs WebJars
 * available in the classpath to the parent of given {@link CommandLineConverterEngine.CommandLineInfo#compilationResult}
 * in the than NPM. This guarantee to any {@link ExecutorHolder} that any command will be executed in a context
 * where 'node_modules' are available.
 * </p>
 *
 * <p>
 * This inspector also sets the {@code WebJarAssetLocator} to any new {@link WebJarNutDao} component.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.3
 */
@ObjectBuilderInspector.InspectedType({ WebJarNutDao.class, ExecutorHolder.class })
public class WebJarInspector implements ObjectBuilderInspector {

    /**
     * The prefix of WebJar assets.
     */
    public static final String PREFIX = "META-INF/resources/webjars/";

    /**
     * The logger.
     */
    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Unmarshaller for {@code package.json}.
     */
    private final Gson gson;

    /**
     * The locator.
     */
    private final WebJarAssetLocator webJarAssetLocator;

    /**
     * <p>
     * Builds a new instance.
     * </p>
     */
    public WebJarInspector() {
        this.webJarAssetLocator = new WebJarAssetLocator();
        this.gson = new Gson();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T inspect(final T object) {
        if (object instanceof WebJarNutDao) {
            WebJarNutDao.class.cast(object).setWebJarAssetLocator(webJarAssetLocator);
        } else {
            final ExecutorHolder executorHolder = ExecutorHolder.class.cast(object);
            executorHolder.setExecutor(new WebJarExecutor(executorHolder.getExecutor()));
        }

        return object;
    }

    /**
     * <p>
     * This executor installs all the NPM dependencies declared in WebJar to the working directory.
     * </p>
     *
     * @author Guillaume DROUET
     * @since 0.5.3
     */
    private class WebJarExecutor implements BiFunction<CommandLineConverterEngine.CommandLineInfo, EngineRequest, Boolean> {

        /**
         * Wrapped executor.
         */
        private BiFunction<CommandLineConverterEngine.CommandLineInfo, EngineRequest, Boolean> wrap;

        /**
         * <p>
         * Builds a new instance.
         * </p>
         *
         * @param wrap the wrapped executor
         */
        private WebJarExecutor(final BiFunction<CommandLineConverterEngine.CommandLineInfo, EngineRequest, Boolean> wrap) {
            this.wrap = wrap;
        }

        /**
         * <p>
         * Extracts the directory name for the WebJar identified by the given name.
         * The directory will be the name located in the {@code package.json} at the root of module.
         * If this file does not exists, the given WebJar name will be used.
         * </p>
         *
         * @param webJarName the WebJar name
         * @param version the WebJar version
         * @return the name defined in {@code package.json}, the WebJar name if it does not exist
         */
        private String extractDirectory(final String webJarName, final String version) {
            InputStream packageJson = null;
            final String packageJsonPath = String.format("/%s%s/%s/package.json", PREFIX, webJarName, version);

            try {
                packageJson = getClass().getResourceAsStream(packageJsonPath);

                // Load the package.json
                if (packageJson != null) {
                    final JsonReader reader = gson.newJsonReader(new InputStreamReader(packageJson));
                    reader.beginObject();

                    // Look for name property
                    while (reader.hasNext()) {
                        if ("name".equals(reader.nextName())) {
                            // Name found
                            return reader.nextString();
                        } else {
                            reader.skipValue();
                        }
                    }
                }
            } catch (IOException ioe) {
                WuicException.throwBadStateException(new IllegalStateException(String.format("Unable to read %s", packageJson), ioe));
            } finally {
                IOUtils.close(packageJson);
            }

            // name not found in package.json, use WebJar name
            return webJarName;
        }

        /**
         * <p>
         * Copy the given WebJar asset to the specified module and its sub directory.
         * </p>
         *
         * @param modules the modules directory
         * @param directory the sub directory
         * @param asset the asset
         * @param webJarVersion the WebJar version
         */
        private void copyAsset(final File modules, final String directory, final String asset, final String webJarVersion) {
            InputStream is = null;
            OutputStream os = null;

            try {
                // Read the file from JAR file
                is = getClass().getResourceAsStream("/" + asset);

                // Write it to node_module directory, skipping the version
                final int versionIndex = asset.indexOf(webJarVersion) + webJarVersion.length();
                final File file = new File(modules, directory + asset.substring(versionIndex));

                if (file.getParentFile().mkdirs()) {
                    logger.info("Created directory {}", file.getParent());
                }

                if (file.createNewFile()) {
                    os = new FileOutputStream(file);
                    IOUtils.copyStream(is, os);
                } else {
                    WuicException.throwBadStateException(
                            new IllegalStateException(String.format("Unable to create %s", file.getAbsolutePath())));
                }
            } catch (IOException ioe) {
                WuicException.throwBadStateException(new IllegalStateException("Unable to extract the WebJar to working directory", ioe));
            } finally {
                IOUtils.close(is, os);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Boolean apply(final CommandLineConverterEngine.CommandLineInfo commandLineInfo, final EngineRequest engineRequest) {

            // Installs a "node_modules" directory if needed
            final File modules = new File(commandLineInfo.getCompilationResult().getParent(), "node_modules");
            final Map<String, String> webJars = webJarAssetLocator.getWebJars();

            for (final Map.Entry<String, String> webJar : webJars.entrySet()) {
                final Set<String> assets = webJarAssetLocator.listAssets(PREFIX + webJar.getKey());
                final String directory = extractDirectory(webJar.getKey(), webJar.getValue());

                for (final String asset : assets) {
                    copyAsset(modules, directory, asset, webJar.getValue());
                }
            }

            return wrap.apply(commandLineInfo, engineRequest);
        }
    }
}
