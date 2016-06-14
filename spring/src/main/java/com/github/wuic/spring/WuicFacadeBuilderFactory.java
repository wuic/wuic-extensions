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


package com.github.wuic.spring;

import com.github.wuic.WuicFacadeBuilder;
import com.github.wuic.config.ObjectBuilderInspector;
import com.github.wuic.context.ContextBuilderConfigurator;
import com.github.wuic.servlet.WuicServletContextListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.servlet.ServletContext;
import java.util.Map;

/**
 * <p>
 * This class creates a {@link WuicFacadeBuilder} based on the instance created by {@link WuicServletContextListener}
 * and makes additional configuration according to components discovered in spring context.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.3
 */
@Component
public class WuicFacadeBuilderFactory {

    /**
     * The servlet context.
     */
    @Autowired
    private ServletContext servletContext;

    /**
     * Application context.
     */
    @Autowired
    private ApplicationContext applicationContext;

    /**
     * <p>
     * Creates a new {@link WuicFacadeBuilder}. If any {@link ContextBuilderConfigurator} or {@link ObjectBuilderInspector}
     * is detected in the spring application context, it's added to the created builder.
     * </p>
     *
     * @return the {@link WuicFacadeBuilder}
     */
    public WuicFacadeBuilder create() {
        final WuicFacadeBuilder retval = new WuicFacadeBuilder(WuicServletContextListener.getWuicFacadeBuilder(servletContext));

        // Additional configurators
        final Map<String, ContextBuilderConfigurator> configurators = applicationContext.getBeansOfType(ContextBuilderConfigurator.class);
        retval.contextBuilderConfigurators(configurators.values().toArray(new ContextBuilderConfigurator[configurators.size()]));

        // Additional inspectors
        final Map<String, ObjectBuilderInspector> inspectors = applicationContext.getBeansOfType(ObjectBuilderInspector.class);
        retval.objectBuilderInspector(inspectors.values().toArray(new ObjectBuilderInspector[inspectors.size()]));

        // Inject properties
        retval.addPropertyResolver(new SpringPropertyResolver(applicationContext.getEnvironment()));

        // Additional profiles
        retval.contextBuilder().enableProfile(applicationContext.getEnvironment().getActiveProfiles()).toFacade();

        return retval;
    }
}
