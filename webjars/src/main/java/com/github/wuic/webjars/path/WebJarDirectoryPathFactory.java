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

import com.github.wuic.path.DirectoryPath;
import com.github.wuic.path.DirectoryPathFactory;
import com.github.wuic.util.NumberUtils;
import org.webjars.WebJarAssetLocator;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>
 * {@link com.github.wuic.path.DirectoryPathFactory} in charge of creating new {@link WebJarDirectoryPath}.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.3
 */
public class WebJarDirectoryPathFactory implements DirectoryPathFactory {

    /**
     * Pattern to match resources from webjar locations.
     */
    private static final Pattern WEBJAR_PATH_PATTERN = Pattern.compile(Pattern.quote(WebJarAssetLocator.WEBJARS_PATH_PREFIX) + "/[^/]*?/([^/]*?)/(.*)");

    /**
     * The WebJar locator.
     */
    private final WebJarAssetLocator webJarAssetLocator;

    /**
     * All valid locations in webjars. Each location is associated to the artifact version.
     */
    private final Map<String, String> locations;

    /**
     * <p>
     * Creates a new instance.
     * </p>
     *
     * @param wjal the asset locator
     */
    public WebJarDirectoryPathFactory(final WebJarAssetLocator wjal) {
        webJarAssetLocator = wjal;
        locations = new HashMap<String, String>();

        for (final String location : webJarAssetLocator.listAssets(WebJarAssetLocator.WEBJARS_PATH_PREFIX)) {
            final Matcher matcher = WEBJAR_PATH_PATTERN.matcher(location);

            if (matcher.matches()) {
                locations.put(matcher.group(NumberUtils.TWO), matcher.group(1));
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DirectoryPath create(final String path) {
        return new WebJarDirectoryPath(path, null, webJarAssetLocator, locations);
    }
}
