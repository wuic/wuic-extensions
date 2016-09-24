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


package com.github.wuic.thymeleaf;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.TemplateProcessingParameters;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.resourceresolver.IResourceResolver;
import org.thymeleaf.templateresolver.TemplateResolver;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;

/**
 * <p>
 * Filter that call thymeleaf when filtering the template.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.0
 */
public class ThymeleafFilterTest implements Filter {

    /**
     * Servlet context.
     */
    private ServletContext servletContext;

    /**
     * Template engine.
     */
    private TemplateEngine templateEngine;

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(final FilterConfig filterConfig) throws ServletException {
        servletContext = filterConfig.getServletContext();
        templateEngine = new TemplateEngine();
        templateEngine.setTemplateResolver(createTemplateResolver());
        templateEngine.setDialect(new WuicDialect(ThymeleafTest.FACADE));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
            throws IOException, ServletException {
        try {
            templateEngine.process("index", createContext(request, response), response.getWriter());
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    /**
     * <p>
     * Creates the template resolver.
     * </p>
     *
     * @return the resolver
     */
    private TemplateResolver createTemplateResolver() {
        final TemplateResolver templateResolver = new TemplateResolver();
        templateResolver.setResourceResolver(new IResourceResolver() {

            @Override
            public String getName() {
                return "index.html";
            }

            @Override
            public InputStream getResourceAsStream(TemplateProcessingParameters templateProcessingParameters, String resourceName) {
                return getClass().getResourceAsStream("/thymeleaf/index.html");
            }
        });

        return templateResolver;
    }

    /**
     * <p>
     * Creates a new context.
     * </p>
     *
     * @param request the request
     * @param response the response
     * @return the web context wrapping both request and response
     */
    protected WebContext createContext(final ServletRequest request, final ServletResponse response) {
        return new WebContext((HttpServletRequest) request, (HttpServletResponse) response, servletContext, request.getLocale());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void destroy() {
    }
}
