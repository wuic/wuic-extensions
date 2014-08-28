/*
 * "Copyright (c) 2014   Capgemini Technology Services (hereinafter "Capgemini")
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


package com.github.wuic.spring;

import com.github.wuic.util.IOUtils;
import com.github.wuic.util.UrlMatcher;
import com.github.wuic.util.UrlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.resource.VersionStrategy;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * <p>
 * This strategy gets the version from {@link org.springframework.core.io.Resource#lastModified()} call.
 * Considering that this implementation can be used only when the chain of responsibility relies on a
 * {@link WuicPathResourceResolver}, {@link Resource} implementation is always a {@link WuicResource} which returns
 * {@link com.github.wuic.nut.Nut#getVersionNumber()} when {@link org.springframework.core.io.Resource#lastModified()}
 * is called. Regarding how the {@link com.github.wuic.nut.dao.NutDao} which produced it is configured, the value will
 * be a timestamp or a hash based on content (see {@link com.github.wuic.ApplicationConfig#CONTENT_BASED_VERSION_NUMBER}.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.0
 * @since 0.5.0
 */
public class WuicVersionStrategy implements VersionStrategy {

    /**
     * The logger.
     */
    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * {@inheritDoc}
     */
    @Override
    public String getResourceVersion(final Resource resource) {
        try {
            return String.valueOf(resource.lastModified());
        } catch (IOException ioe) {
            logger.warn("Unable to retrieve version number.", ioe);
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String extractVersion(final String requestPath) {
        try {
            final UrlMatcher m = UrlUtils.urlMatcher(requestPath);

            if (m.matches()) {
                return m.getVersionNumber();
            } else {
               logger.warn("Unable to extract version", new IllegalArgumentException(requestPath));
            }
        } catch (UnsupportedEncodingException uee) {
            logger.warn("Unable to extract version", uee);
        }

        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String removeVersion(final String requestPath, final String version) {
        final int i = requestPath.indexOf(version);
        return IOUtils.mergePath(requestPath.substring(0, i), requestPath.substring(i + version.length()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String addVersion(final String requestPath, final String version) {
        return new StringBuilder(requestPath).insert(requestPath.indexOf('/'), "/" + version).toString();
    }
}
