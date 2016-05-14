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


package com.github.wuic.nodejs;

import com.github.eirslett.maven.plugins.frontend.lib.FrontendPluginFactory;
import com.github.eirslett.maven.plugins.frontend.lib.InstallationException;
import com.github.eirslett.maven.plugins.frontend.lib.ProxyConfig;
import com.github.wuic.config.ObjectBuilderInspector;
import com.github.wuic.engine.EngineRequest;
import com.github.wuic.engine.core.CommandLineConverterEngine;
import com.github.wuic.engine.core.CommandLineConverterEngine.CommandLineInfo;
import com.github.wuic.exception.WuicException;
import com.github.wuic.util.BiFunction;
import com.github.wuic.util.IOUtils;
import com.github.wuic.util.NutDiskStore;
import com.github.wuic.util.WuicScheduledThreadPool;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * <p>
 * This inspector decorates any created {@link CommandLineConverterEngine} with an executor that installs {@code Node.JS}
 * and {@code NPM} in the parent of given {@link CommandLineInfo#compilationResult}. This guarantee to any
 * {@link CommandLineConverterEngine} that any command will be executed in a context where 'npm' command is available.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.3
 */
@ObjectBuilderInspector.InspectedType(CommandLineConverterEngine.class)
public class NodeJsInspector implements ObjectBuilderInspector, Runnable {

    /**
     * Configuration file location in classpath.
     */
    public static final String CONFIG_FILE_PATH = '/' + NodeJsInspector.class.getName() + ".config.json";

    /**
     * Contains all paths where installation is already done.
     */
    private static final Set<String> INSTALLED = new HashSet<String>();

    /**
     * Mandatory node version.
     */
    private static final String NODE_VERSION = "nodeVersion";

    /**
     * Mandatory NPM version.
     */
    private static final String NPM_VERSION = "npmVersion";

    /**
     * Mandatory port in proxy configuration.
     */
    private static final String PORT = "port";

    /**
     * The logger.
     */
    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Node version.
     */
    private final String nodeVersion;

    /**
     * NPM version.
     */
    private final String npmVersion;

    /**
     * Node download URL.
     */
    private final String nodeDownloadRoot;

    /**
     * NPM download URL.
     */
    private final String npmDownloadRoot;

    /**
     * Proxies.
     */
    private final List<ProxyConfig.Proxy> proxies;

    /**
     * <p>
     * Builds a new instance.
     * </p>
     */
    public NodeJsInspector() {
        InputStream is = null;

        try {
            is = getClass().getResourceAsStream(CONFIG_FILE_PATH);

            if (is == null) {
                WuicException.throwBadArgumentException(new IllegalArgumentException(
                        String.format("Configuration file '%s' must be defined in the classpath", CONFIG_FILE_PATH)));
            }

            // Read configuration
            final JsonObject config = new Gson().fromJson(new InputStreamReader(is), JsonObject.class);

            // Mandatory fields
            if (!config.has(NODE_VERSION)) {
                WuicException.throwBadArgumentException(new IllegalArgumentException(
                        String.format("%s is mandatory in %s", NODE_VERSION, CONFIG_FILE_PATH)));
            } else if (!config.has(NPM_VERSION)) {
                WuicException.throwBadArgumentException(new IllegalArgumentException(
                        String.format("%s is mandatory in %s", NPM_VERSION, CONFIG_FILE_PATH)));
            }

            nodeVersion = read(config, "nodeVersion");
            npmVersion = read(config, "npmVersion");

            // Optional
            nodeDownloadRoot = read(config, "nodeDownloadRoot");
            npmDownloadRoot = read(config, "npmDownloadRoot");
            proxies = new ArrayList<ProxyConfig.Proxy>();
            installProxies(config);

            // Download and install npm
            WuicScheduledThreadPool.getInstance().executeAsap(this);
        } finally {
            IOUtils.close(is);
        }
    }

    /**
     * <p>
     * Installs optional proxies.
     * </p>
     *
     * @param config the configuration object
     */
    private void installProxies(final JsonObject config) {

        // Read proxies
        logger.info("Reading JSON array {} from {}", "proxies", CONFIG_FILE_PATH);
        final JsonArray jsonProxies = config.getAsJsonArray("proxies");

        // Proxies are optional
        if (jsonProxies != null) {
            for (final JsonElement element : jsonProxies) {
                final JsonObject jsonProxy = element.getAsJsonObject();
                logger.info("Reading {} from {}", PORT, CONFIG_FILE_PATH);

                // In a proxy configuration, port is mandatory
                if (!jsonProxy.has(PORT)) {
                    WuicException.throwBadArgumentException(new IllegalArgumentException(
                            String.format("%s is mandatory in %s", PORT, CONFIG_FILE_PATH)));
                }

                proxies.add(new ProxyConfig.Proxy(read(jsonProxy, "id"),
                        read(jsonProxy, "protocol"),
                        read(jsonProxy, "host"),
                        jsonProxy.get(PORT).getAsInt(),
                        read(jsonProxy, "username"),
                        read(jsonProxy, "password"),
                        read(jsonProxy, "nonProxyHosts")));
            }
        }
    }

    /**
     * <p>
     * Reads a member from a {@code JsonObject}.
     * </p>
     *
     * @param element the JSON object
     * @param member the member
     * @return the {@code String representation}
     */
    private String read(final JsonObject element, final String member) {
        logger.info("Reading {} from {}", member, CONFIG_FILE_PATH);
        return element.has(member) ? element.get(member).getAsString() : null;
    }

    /**
     * <p>
     * Checks if node is already installed in the current working directory and download it if needed.
     * </p>
     *
     * @return the installation directory
     */
    private File installNpm() {
        final File workingDir = NutDiskStore.INSTANCE.getWorkingDirectory();

        synchronized (INSTALLED) {

            // Not already installed
            if (!INSTALLED.contains(workingDir.getAbsolutePath())) {
                final FrontendPluginFactory factory = new FrontendPluginFactory(workingDir, workingDir);

                try {
                    // NodeJS will be installed in "node" directory, script will be referenced from the working directory
                    factory.getNodeAndNPMInstaller(new ProxyConfig(proxies)).install(nodeVersion, npmVersion, nodeDownloadRoot, npmDownloadRoot);
                    INSTALLED.add(workingDir.getAbsolutePath());
                } catch (InstallationException ie) {
                    WuicException.throwBadStateException(ie);
                }
            }
        }

        return workingDir;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T inspect(final T object) {
        final CommandLineConverterEngine engine = CommandLineConverterEngine.class.cast(object);
        engine.setExecutor(new NodeJsExecutor(engine.getExecutor()));
        return object;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        installNpm();
    }

    /**
     * <p>
     * This executor wraps any new {@link CommandLineConverterEngine} executor to make sure NodeJS is installed before
     * a command is executed.
     * </p>
     *
     * @author Guillaume DROUET
     * @since 0.5.3
     */
    private class NodeJsExecutor implements BiFunction<CommandLineInfo, EngineRequest, Boolean> {

        /**
         * Wrapped executor.
         */
        private BiFunction<CommandLineInfo, EngineRequest, Boolean> wrap;

        /**
         * <p>
         * Builds a new instance.
         * </p>
         *
         * @param wrap the wrapped executor
         */
        private NodeJsExecutor(final BiFunction<CommandLineInfo, EngineRequest, Boolean> wrap) {
            this.wrap = wrap;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Boolean apply(final CommandLineInfo commandLineInfo, final EngineRequest engineRequest) {
            // Download and install npm
            final File workingDir = installNpm();

            // Installs an "npm" script if needed
            final File bin = new File(commandLineInfo.getCompilationResult().getParent(), "npm");

            if (!bin.exists()) {
                OutputStream outputStream = null;

                try {
                    outputStream = new FileOutputStream(bin);

                    if (CommandLineConverterEngine.IS_WINDOWS) {
                        // Windows script
                        outputStream.write((IOUtils.mergePath(workingDir.getAbsolutePath(), "node/npm.cmd") + " $@").getBytes());
                    } else {
                        // Linux script
                        outputStream.write((IOUtils.mergePath(workingDir.getAbsolutePath(), "node/npm") + " %*").getBytes());
                    }
                } catch (IOException ioe) {
                    logger.error("Unable to create scripts for Node.JS", ioe);
                } finally {
                    IOUtils.close(outputStream);
                }
            }

            return wrap.apply(commandLineInfo, engineRequest);
        }
    }
}
