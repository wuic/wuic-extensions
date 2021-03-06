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

import com.github.wuic.ProcessContext;
import com.github.wuic.WuicFacade;
import com.github.wuic.engine.EngineType;
import com.github.wuic.exception.WuicException;
import com.github.wuic.servlet.ServletProcessContext;
import com.github.wuic.nut.Nut;
import com.github.wuic.util.UrlMatcher;
import com.github.wuic.util.UrlUtils;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.resource.PathResourceResolver;
import org.springframework.web.servlet.resource.ResourceResolverChain;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.util.List;

/**
 * <p>
 * This resolver relies on WUIC to get resources through existing nuts. When running a workflow, some {@link EngineType}
 * are {@link #SKIP skipped} because spring user may configure spring based transformers that do the same thing.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.0
 */
public class WuicPathResourceResolver extends PathResourceResolver {

    /**
     * {@link EngineType} to skip.
     */
    private static final EngineType[] SKIP = new EngineType[] { EngineType.BINARY_COMPRESSION, EngineType.CACHE, };

    /**
     * The facade managed by spring container.
     */
    private WuicFacade wuicFacade;

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param wf the facade
     */
    public WuicPathResourceResolver(final WuicFacade wf) {
        wuicFacade = wf;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Resource resolveResource(final HttpServletRequest request,
                                    final String s,
                                    final List<? extends Resource> resources,
                                    final ResourceResolverChain resourceResolverChain) {
        final UrlMatcher matcher = UrlUtils.urlMatcher(s);

        if (!matcher.matches()) {
            return null;
        } else {
            try {
                return internalResolve(matcher, request == null ? ProcessContext.DEFAULT : new ServletProcessContext(request));
            } catch (UnsupportedEncodingException we) {
                throw new IllegalArgumentException(we);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String resolveUrlPath(final String s,
                                 final List<? extends Resource> resources,
                                 final ResourceResolverChain resourceResolverChain) {
        return s;
    }

    /**
     * <p>
     * Resolves the nut from the given {@link UrlMatcher} and wrap it to a spring resource.
     * </p>
     *
     * @param processContext the process context
     * @param matcher the matcher
     * @return the resource that wraps the nut
     * @throws UnsupportedEncodingException should not happen
     */
    private WuicResource internalResolve(final UrlMatcher matcher, final ProcessContext processContext)
            throws UnsupportedEncodingException {

        try {
            final Nut nut = wuicFacade.runWorkflow(matcher.getWorkflowId(), matcher.getNutName(), processContext, SKIP);

            return new WuicResource(nut);
        } catch (WuicException we) {
            logger.debug(String.format("Unable to resolve nut with name '%s' in workflow '%s'",
                    matcher.getNutName(), matcher.getWorkflowId()), we);
            return null;
        }
    }
}
