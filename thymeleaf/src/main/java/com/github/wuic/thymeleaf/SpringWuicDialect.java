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


package com.github.wuic.thymeleaf;

import com.github.wuic.WuicFacade;
import com.github.wuic.nut.Nut;
import com.github.wuic.util.IOUtils;
import com.github.wuic.util.UrlProvider;
import org.springframework.web.servlet.resource.ResourceUrlProvider;

/**
 * <p>
 * This dialect can be used when thymeleaf is integrated with spring so it can rely on spring resource URL provider and
 * the {@link WuicFacade} conifigured inside the framework.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.0
 * @since 0.5.0
 */
public class SpringWuicDialect extends WuicDialect {

    /**
     * The resource URL provider from spring.
     */
    private final ResourceUrlProvider urlProvider;

    /**
     * The facade.
     */
    private final WuicFacade wuicFacade;

    /**
     * Cache or not.
     */
    private final Boolean cache;

    /**
     * <p>
     * Builds a new instance
     * </p>
     *
     * @param resourceUrlProvider the resource URL provider
     * @param wf the underlying {@link WuicFacade}
     * @param c cache or not
     */
    public SpringWuicDialect(final ResourceUrlProvider resourceUrlProvider, final WuicFacade wf, final Boolean c) {
        this.urlProvider = resourceUrlProvider;
        this.wuicFacade = wf;
        this.cache = !c;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ImportProcessor importProcessor() {
        return new SpringImportProcessor();
    }

    /**
     * <p>
     * The {@link ImportProcessor} that relies on {@link WuicFacade} provided by spring framework. Should be reviewed
     * with 'https://github.com/wuic/wuic/issues/133'.
     * </p>
     *
     * @author Guillaume DROUET
     * @version 1.0
     * @since 0.5.0
     */
    public final class SpringImportProcessor extends ImportProcessor {

        /**
         * {@inheritDoc}
         */
        @Override
        protected UrlProvider urlProvider(String workflowId) {
            return new SpringUrlProvider(workflowId);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected WuicFacade wuicFacade() {
            return wuicFacade;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected Boolean wuicServletMultipleConfInTagSupport() {
            return cache;
        }
    }

    /**
     * <p>
     * The {@link UrlProvider} that relied on spring framework.
     * </p>
     *
     * @author Guillaume DROUET
     * @version 1.0
     * @since 0.5.0
     */
    public final class SpringUrlProvider implements UrlProvider {

        /**
         * The base path of any resource.
         */
        private final String workflowContextPath;

        /**
         * <p>
         * Builds a new instance
         * </p>
         *
         * @param wcp the workflow ID
         */
        public SpringUrlProvider(final String wcp) {
            this.workflowContextPath = IOUtils.mergePath(wuicFacade.getContextPath(), wcp);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getUrl(final Nut nut) {
            return urlProvider.getForLookupPath(IOUtils.mergePath(workflowContextPath, nut.getName()));
        }
    }
}
