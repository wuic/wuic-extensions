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


package com.github.wuic.extensions.http2.jetty;

import com.github.wuic.servlet.PushService;
import org.eclipse.jetty.server.PushBuilder;
import org.eclipse.jetty.server.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collection;

/**
 * <p>
 * A {@link PushService} implementation based on jetty API.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.2
 */
public class JettyPushService implements PushService {

    /**
     * Logger.
     */
    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * {@inheritDoc}
     */
    @Override
    public void push(final HttpServletRequest request, final HttpServletResponse response, final Collection<String> paths) {
        final Request jettyRequest = Request.getBaseRequest(request);

        if (jettyRequest == null) {
            logger.warn("The given request {} is not an instance of jetty internal {} or is not a {} wrapping it.",
                    request, Request.class.getName(), HttpServletRequest.class.getName(), new IllegalStateException());
        } else if (jettyRequest.isPushSupported()) {
            final PushBuilder pushBuilder = jettyRequest.getPushBuilder();

            for (final String path : paths) {
                pushBuilder.path(path).push();
            }
        }
    }
}
