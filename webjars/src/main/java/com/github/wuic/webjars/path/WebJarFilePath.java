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


package com.github.wuic.webjars.path;

import com.github.wuic.path.DirectoryPath;
import com.github.wuic.path.FilePath;
import com.github.wuic.path.core.SimplePath;
import com.github.wuic.util.DefaultInput;
import com.github.wuic.util.Input;
import com.github.wuic.util.NumberUtils;
import org.webjars.WebJarAssetLocator;

import java.io.IOException;

/**
 * <p>
 * Represents a file in the webjar file.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.3
 */
public class WebJarFilePath extends SimplePath implements FilePath {

    /**
     * The WebJar locator.
     */
    private final WebJarAssetLocator webJarAssetLocator;

    /**
     * The version of this resource.
     */
    private final long version;

    /**
     * The version in the WebJar path.
     */
    private final String pathVersion;

    /**
     * <p>
     * Creates a new instance.
     * </p>
     *
     * @param n the name
     * @param version the version of this resource
     * @param dp the parent
     * @param wasl the asset locator
     * @param cs the charset
     */
    public WebJarFilePath(final String n,
                          final String version,
                          final DirectoryPath dp,
                          final WebJarAssetLocator wasl,
                          final String cs) {
        super(n, dp, cs);
        this.webJarAssetLocator = wasl;
        this.version = computeVersion(version);
        this.pathVersion = version;
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
        final String integer = version.replaceAll("[^\\d]", "");

        if (!NumberUtils.isNumber(integer)) {
            throw new IllegalStateException(String.format("Bad number format: %s", version));
        }

        return Long.parseLong(integer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getLastUpdate() throws IOException {
        return version;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Input openStream() throws IOException {
        final String path = pathVersion + getAbsolutePath();
        return new DefaultInput(getClass().getResourceAsStream('/' + webJarAssetLocator.getFullPath(path)), getCharset());
    }
}
