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


package com.github.wuic.spring.test;

import com.github.wuic.WuicFacade;
import com.github.wuic.WuicFacadeBuilder;
import com.github.wuic.exception.WuicException;
import com.github.wuic.spring.WuicHandlerMapping;
import com.github.wuic.spring.WuicPathResourceResolver;
import com.github.wuic.spring.WuicVersionStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.resource.ResourceResolver;
import org.springframework.web.servlet.resource.ResourceResolverChain;
import org.springframework.web.servlet.resource.ResourceTransformer;
import org.springframework.web.servlet.resource.ResourceUrlProvider;
import org.springframework.web.servlet.resource.VersionResourceResolver;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * <p>
 * Pieces of codes explained in spring tutorial.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.0
 */
public class SpringTutorial {

    // tag::SpringWuicFacade[]
    @Autowired
    private ApplicationContext applicationContext;

    @Bean
    public WuicFacade wuicFacade() throws WuicException {
        return new WuicFacadeBuilder()
                .contextPath("/resources/")
                .wuicConfigurationPath(getClass().getResource("/wuic.xml"))
                .build();
    }
    // end::SpringWuicFacade[]

    // tag::MyWebConfig[]
    @Configuration
    @EnableWebMvc
    public class MyWebConfig extends WebMvcConfigurerAdapter {

    }
    // end::MyWebConfig[]

    // tag::handleWuicResources[]
    @Bean
    public SimpleUrlHandlerMapping handleWuicResources(final ServletContext servletContext,
                                                       final ResourceUrlProvider resourceUrlProvider,
                                                       final WuicFacade wuicFacade) {
        final ResourceResolver versionResourceResolver =
                new VersionResourceResolver().addVersionStrategy(new WuicVersionStrategy(), "/**/*");

        final ResourceResolver pathResourceResolver =
                new WuicPathResourceResolver(applicationContext.getBean(WuicFacade.class));

        return new WuicHandlerMapping(applicationContext,
                servletContext,
                resourceUrlProvider,
                wuicFacade,
                Arrays.asList(versionResourceResolver, pathResourceResolver),
                Collections.<ResourceTransformer>emptyList());
    }
    // end::handleWuicResources[]

    // tag::addResourceHandlers[]
    public void addResourceHandlers(final ResourceHandlerRegistry registry) {
        final ResourceResolver myResolver = new MyResourceResolver();
        registry.addResourceHandler("/other/**")
                .resourceChain(true)
                .addResolver(myResolver);
    }
    // end::addResourceHandlers[]

    private class MyResourceResolver implements org.springframework.web.servlet.resource.ResourceResolver {

        @Override
        public Resource resolveResource(final HttpServletRequest request,
                                        final String requestPath,
                                        final List<? extends Resource> locations,
                                        final ResourceResolverChain chain) {
            return null;
        }

        @Override
        public String resolveUrlPath(final String resourcePath,
                                     final List<? extends Resource> locations,
                                     final ResourceResolverChain chain) {
            return null;
        }
    }
}
