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


package com.github.wuic.extensions.http2.undertow;

import com.github.wuic.servlet.PushService;
import io.undertow.server.ServerConnection;
import io.undertow.servlet.spec.HttpServletRequestImpl;
import io.undertow.util.HeaderMap;
import io.undertow.util.Methods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.util.Collection;

/**
 * <p>
 * A {@link PushService} implementation based on Undertow API.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.3
 */
public class UndertowPushService implements PushService {

    /**
     * Logger.
     */
    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * {@inheritDoc}
     */
    @Override
    public void push(final HttpServletRequest request, final HttpServletResponse response, final Collection<String> paths) {

        // Try to extract the native request
        ServerConnection undertowRequest = null;
        ServletRequest inner = request;

        while (inner instanceof HttpServletRequestWrapper) {
            inner = HttpServletRequestWrapper.class.cast(inner).getRequest();
        }

        if (inner instanceof HttpServletRequestImpl) {
            undertowRequest = HttpServletRequestImpl.class.cast(inner).getExchange().getConnection();
        }

        // Failed to extract native request, unable to push
        if (undertowRequest == null) {
            logger.warn("The given request {} is not an instance of undertow internal {} or is not a {} wrapping it.",
                    request, HttpServletRequestImpl.class.getName(), HttpServletRequest.class.getName(), new IllegalStateException());
        } else if (undertowRequest.isPushSupported()) {
            for (final String path : paths) {
                undertowRequest.pushResource(path, Methods.GET, new HeaderMap());
            }
        }
    }
}
