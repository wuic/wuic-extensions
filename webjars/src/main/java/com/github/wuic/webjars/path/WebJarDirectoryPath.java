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


package com.github.wuic.webjars.path;

import com.github.wuic.path.AbstractDirectoryPath;
import com.github.wuic.path.DirectoryPath;
import com.github.wuic.path.Path;
import com.github.wuic.util.IOUtils;
import com.github.wuic.util.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webjars.WebJarAssetLocator;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * Represents a directory in the webjar file.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.3
 */
public class WebJarDirectoryPath extends AbstractDirectoryPath {

    /**
     * <p>
     * A sub path represent a child element of this directory.
     * </p>
     *
     * @author Guillaume DROUET
     * @since 0.5.3
     */
    private static class SubPath {

        /**
         * The version of this path.
         */
        private final String version;

        /**
         * Boolean value is {@code true} in case of directory, {@code false} in case of file.
         */
        private final boolean isDirectory;

        /**
         * <p>
         * Builds a new instance.
         * </p>
         *
         * @param version the version
         * @param directory the flag indicated if the path is a directory of not
         */
        private SubPath(final String version, final boolean directory) {
            this.version = version;
            isDirectory = directory;
        }

        /**
         * <p>
         * Gets the version.
         * </p>
         *
         * @return the version
         */
        private String getVersion() {
            return version;
        }

        /**
         * <p>
         * Indicates if the path is a directory or not.
         * </p>
         *
         * @return {@code true} in case of directory, {@code false} otherwise
         */
        private boolean isDirectory() {
            return isDirectory;
        }
    }

    /**
     * Logger.
     */
    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * The WebJar locator.
     */
    private WebJarAssetLocator webJarAssetLocator;

    /**
     * Sub paths of this directory.
     */
    private final Map<String, SubPath> subPaths;

    /**
     * All registered locations.
     */
    private final Map<String, String> locations;

    /**
     * <p>
     * Creates a new instance.
     * </p>
     *
     * @param name the name
     * @param parent the parent
     * @param wjal the
     * @param charset the charset
     * @param locations all locations associated to their version
     */
    public WebJarDirectoryPath(final String name,
                               final DirectoryPath parent,
                               final WebJarAssetLocator wjal,
                               final Map<String, String> locations,
                               final String charset) {
        super(name, parent, charset);
        this.webJarAssetLocator = wjal;
        this.subPaths = new HashMap<String, SubPath>();
        this.locations = locations;

        // Collect all sub path of this directory
        for (final Map.Entry<String, String> location : locations.entrySet()) {

            // Check if the direct child path is a directory or not
            final int slash = location.getKey().indexOf('/');
            final boolean isDirectory = slash != -1;
            final String path = isDirectory ? location.getKey().substring(0, slash) : location.getKey();
            subPaths.put(path, new SubPath(location.getValue(), isDirectory));
        }
    }

    /**
     * <p>
     * Compute a {@code String} representation of the version number. All dot (in case of float value) are removed and
     * then the resulting {@code String} is simply parsed to a {@code long}.
     * </p>
     *
     * @param version the version
     * @return the version
     */
    private static long computeVersion(final String version) {
        final String integer = version.replaceAll("\\.", "");

        if (!NumberUtils.isNumber(integer)) {
            throw new IllegalStateException(String.format("Bad number format: %s", version));
        }

        return Long.parseLong(integer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Path buildChild(final String child) throws IOException {
        final SubPath subPath = subPaths.get(child);

        // Path not found
        if (subPath == null) {
            final String absoluteParent = getAbsolutePath();
            final String absoluteChild = IOUtils.mergePath(absoluteParent, child);
            logger.warn("Path {} in {} was not found in WebJar asset locations.", absoluteParent, absoluteChild);
            throw new FileNotFoundException();
        } else if (!subPath.isDirectory()) {
            // Single path
            return new WebJarFilePath(child, computeVersion(subPath.getVersion()), this, webJarAssetLocator, getCharset());
        } else {
            // Compute all the paths inside this directory
            final Map<String, String> paths = new HashMap<String, String>();

            for (final Map.Entry<String, String> location : locations.entrySet()) {
                if (location.getKey().startsWith(child)) {
                    paths.put(location.getKey().substring(child.length() + 1), location.getValue());
                }
            }

            return new WebJarDirectoryPath(child, this, webJarAssetLocator, paths, getCharset());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public String[] list() throws IOException {
        return subPaths.keySet().toArray(new String[subPaths.size()]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getLastUpdate() throws IOException {
        return -1L;
    }
}
