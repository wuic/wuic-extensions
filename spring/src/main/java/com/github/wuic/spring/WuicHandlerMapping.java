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

import com.github.wuic.WuicFacade;
import com.github.wuic.util.IOUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;
import org.springframework.web.servlet.resource.ResourceResolver;
import org.springframework.web.servlet.resource.ResourceTransformer;
import org.springframework.web.servlet.resource.ResourceUrlProvider;
import org.springframework.web.servlet.resource.ResourceUrlProviderExposingInterceptor;

import javax.servlet.ServletContext;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * This {@link SimpleUrlHandlerMapping} can be declared as a bean in a spring web context to serve all statics resources
 * with WUIC. See: https://jira.spring.io/browse/SPR-12669 - to not face any integration issue, WUIC should be declared
 * in a dedicated handler.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.1
 */
public final class WuicHandlerMapping extends SimpleUrlHandlerMapping {

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param applicationContext  the spring application context
     * @param servletContext      the servlet context
     * @param resourceUrlProvider the spring resource URL provider
     * @param wuicFacade          a facade
     */
    public WuicHandlerMapping(final ApplicationContext applicationContext,
                              final ServletContext servletContext,
                              final ResourceUrlProvider resourceUrlProvider,
                              final WuicFacade wuicFacade,
                              final List<ResourceResolver> resourceResolvers,
                              final List<ResourceTransformer> resourceTransformers) {
        final Map<String, ResourceHttpRequestHandler> httpRequestHandlerMap = new LinkedHashMap<String, ResourceHttpRequestHandler>();
        final ResourceHttpRequestHandler resourceHandler = new ResourceHttpRequestHandler();
        resourceHandler.setServletContext(servletContext);
        resourceHandler.setApplicationContext(applicationContext);
        resourceHandler.setResourceResolvers(resourceResolvers);
        resourceHandler.setResourceTransformers(resourceTransformers);
        httpRequestHandlerMap.put(IOUtils.mergePath(wuicFacade.getContextPath(), "**"), resourceHandler);
        final HandlerInterceptor interceptor = new ResourceUrlProviderExposingInterceptor(resourceUrlProvider);
        setUrlMap(httpRequestHandlerMap);
        setInterceptors(new HandlerInterceptor[]{interceptor});
    }
}
