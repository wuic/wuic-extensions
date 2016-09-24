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


package com.github.wuic.spring;

import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.util.IOUtils;
import com.github.wuic.util.UrlProvider;
import com.github.wuic.util.UrlProviderFactory;
import org.springframework.web.servlet.resource.ResourceUrlProvider;

/**
 * <p>
 * The helper is a bridge between the {@code ResourceUrlProvider} from spring and the {@code UrlProvider} which helps
 * to resolve URLs inside WUIC.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.0
 */
public class ResourceUrlProviderHelperFactory implements UrlProviderFactory {

    /**
     * The spring provider.
     */
    private ResourceUrlProvider resourceUrlProvider;

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param rup the spring provider
     */
    public ResourceUrlProviderHelperFactory(final ResourceUrlProvider rup) {
        this.resourceUrlProvider = rup;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UrlProvider create(final String workflowId) {
        return new UrlProviderHelper(workflowId);
    }

    /**
     * <p>
     * Calls spring to get the public resource.
     * </p>
     *
     * @param path the path to retrieve
     * @return retrieved value
     */
    public String get(final String path) {
        return this.resourceUrlProvider.getForLookupPath(path);
    }

    /**
     * <p>
     * Helper implementation for {@link UrlProvider}.
     * </p>
     *
     * @author Guillaume DROUET
     * @since 0.5.0
     */
    private final class UrlProviderHelper implements UrlProvider {

        /**
         * Workflow context path.
         */
        private final String workflowContextPath;

        /**
         * <p>
         * Builds a new instance.
         * </p>
         *
         * @param wcp the workflow context path
         */
        private UrlProviderHelper(final String wcp) {
            workflowContextPath = wcp;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getUrl(final ConvertibleNut nut) {
            return get(IOUtils.mergePath(workflowContextPath, nut.getName()));
        }
    }
}
