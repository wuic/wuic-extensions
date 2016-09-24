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

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * <p>
 * This filter redirects requests for resources with a path obtained when NPM dependencies are installed under
 * {@code node_modules} directory to the corresponding {@code WebJar} path. All URL matching {@code /node_modules/*}
 * pattern are captured the filter looks for a corresponding path in the {@link NpmWebJarTranslator} that collects
 * all the {@code WebJar} available in the classpath. If there is no match, the {@code FilterChain} is invoked to
 * delegate the response state to the rest of the filter chain and targeted servlet.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.3
 */
@WebFilter(filterName = "npmWebJarFilter",
        description = "A filter catching requests for resources located in npm_modules and redirects to the corresponding WebJar URL.",
        urlPatterns = "/node_modules/*")
public class NpmWebJarFilter implements Filter {

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(final FilterConfig filterConfig) throws ServletException {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
            throws IOException, ServletException {
        final String requestUri = HttpServletRequest.class.cast(request).getRequestURI();
        final String url = NpmWebJarTranslator.INSTANCE.getWebJarUrl(requestUri.substring("/node_modules/".length()));

        if (url == null) {
            chain.doFilter(request, response);
        } else {
            request.getRequestDispatcher(url).forward(request, response);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void destroy() {
    }
}
