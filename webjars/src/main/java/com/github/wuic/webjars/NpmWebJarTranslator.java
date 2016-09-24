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


package com.github.wuic.webjars;

import com.github.wuic.exception.WuicException;
import com.github.wuic.util.IOUtils;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import org.webjars.WebJarAssetLocator;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * This class translates resources path when installed under {@code node_modules} directory to their corresponding
 * {@code WebJar} path. The {@code WebJarAssetLocator} is used to collect all the {@code WebJars} assets available in
 * the classpath and compute the path they would have if the module would be installed under {@code node_modules}. The
 * path under {@code node_modules} always starts with the module name, which is not always the case in {@code WebJar}.
 * The class tries to resolve the module name by reading the {@code package.json} if exists. If no {@code package.json}
 * exists, the asset path will be used.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.3
 */
public enum NpmWebJarTranslator {

    /**
     * Singleton.
     */
    INSTANCE;

    /**
     * The prefix of WebJar assets.
     */
    public static final String PREFIX = "META-INF/resources/webjars/";

    /**
     * The asset locator.
     */
    private final WebJarAssetLocator webJarAssetLocator;

    /**
     * The mappings.
     */
    private final Map<String, String> mappings;

    /**
     * Unmarshaller for {@code package.json}.
     */
    private final Gson gson;

    /**
     * <p>
     * Builds a new instance.
     * </p>
     */
    private NpmWebJarTranslator() {
        webJarAssetLocator = new WebJarAssetLocator();
        mappings = new HashMap<String, String>();
        gson = new Gson();

        final Map<String, String> webJars = webJarAssetLocator.getWebJars();

        for (final Map.Entry<String, String> webJar : webJars.entrySet()) {
            final Set<String> assets = webJarAssetLocator.listAssets(PREFIX + webJar.getKey());
            final String directory = extractDirectory(webJar.getKey(), webJar.getValue());

            for (final String asset : assets) {
                // Write it to node_module directory, skipping the version
                final int versionIndex = asset.indexOf(webJar.getValue()) + webJar.getValue().length();

                synchronized (mappings) {
                    mappings.put(directory + asset.substring(versionIndex), asset.substring(PREFIX.length()));
                }
            }
        }
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
     * Gets the WebJar URL corresponding to the given resource path when located under {@code node_modules} directory.
     * </p>
     *
     * @param npmPath the NPM path
     * @return the WebJar URL
     */
    public String getWebJarUrl(final String npmPath) {
        final String asset;

        synchronized (mappings) {
            asset = mappings.get(npmPath);
        }

        return asset == null ? null : "/webjars/" + asset;
    }

    /**
     * <p>
     * Gets a new {@code Map} that contains all the classpath locations of each existing {@code WebJar} for their
     * corresponding path location when installed under {@code node_modules}.
     * </p>
     *
     * @return the {@code Map}
     */
    public Map<String, String> getNpmClasspathLocations() {
        synchronized (mappings) {
            final Map<String, String> retval = new HashMap<String, String>(mappings.size());

            for (final Map.Entry<String, String> asset : mappings.entrySet()) {
                retval.put(asset.getKey(), "/" + PREFIX + asset.getValue());
            }

            return retval;
        }
    }

    /**
     * <p>
     * Gets the {@code WebJarAssetLocator}.
     * </p>
     *
     * @return the locator
     */
    public WebJarAssetLocator getWebJarAssetLocator() {
        return webJarAssetLocator;
    }
}
