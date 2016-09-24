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


package com.github.wuic.spring.test;

import com.github.wuic.ApplicationConfig;
import com.github.wuic.NutTypeFactory;
import com.github.wuic.ProcessContext;
import com.github.wuic.config.ObjectBuilderInspector;
import com.github.wuic.context.ContextBuilder;
import com.github.wuic.WuicFacade;
import com.github.wuic.WuicFacadeBuilder;
import com.github.wuic.context.ContextBuilderConfigurator;
import com.github.wuic.context.SimpleContextBuilderConfigurator;
import com.github.wuic.exception.WuicException;
import com.github.wuic.nut.Nut;
import com.github.wuic.nut.dao.core.ClasspathNutDao;
import com.github.wuic.nut.dao.spring.SpringNutDao;
import com.github.wuic.servlet.WuicServletContextListener;
import com.github.wuic.spring.WuicFacadeBuilderFactory;
import com.github.wuic.spring.WuicPathResourceResolver;
import com.github.wuic.spring.WuicVersionStrategy;
import com.github.wuic.util.NutUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.handler.AbstractHandlerMapping;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;
import org.springframework.web.servlet.resource.ResourceResolver;
import org.springframework.web.servlet.resource.ResourceResolverChain;
import org.springframework.web.servlet.resource.VersionResourceResolver;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>
 * Test for spring support.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.0
 */
@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource(properties = ApplicationConfig.INSPECT + "=false")
@ContextConfiguration
public class SpringSupportTest {

    /**
     * <p>
     * Configuration class for test purpose.
     * </p>
     *
     * @author Guillaume DROUET
     * @since 0.5.3
     */
    @Configuration
    public static class ConfigTest {

        /**
         * <p>
         * Creates a mocked {@code ServletContext}.
         * </p>
         *
         * @return the servlet context
         */
        @Bean
        public ServletContext servletContext() {
            return new MockServletContext();
        }

        /**
         * <p>
         * Builds a new {@link ContextBuilderConfigurator} to be injected.
         * </p>
         *
         * @return the bean
         */
        @Bean
        public ContextBuilderConfigurator contextBuilderConfigurator() {
            return new Configurator();
        }

        /**
         * <p>
         * Builds a new {@link ObjectBuilderInspector} to be injected.
         * </p>
         *
         * @return the bean
         */
        @Bean
        public ObjectBuilderInspector objectBuilderInspector() {
            return new Inspector();
        }

        /**
         * <p>
         * Creates a new {@link WuicFacadeBuilderFactory}.
         * </p>
         *
         * @param servletContext the servlet context
         * @return the bean
         */
        @Bean
        public WuicFacadeBuilderFactory wuicFacadeBuilderFactory(final ServletContext servletContext) {
            // Triggers manually context listener
            new WuicServletContextListener().contextInitialized(new ServletContextEvent(servletContext));
            return new WuicFacadeBuilderFactory();
        }
    }

    /**
     * <p>
     * A configurator injected via spring support.
     * </p>
     *
     * @author Guillaume DROUET
     * @since 0.5.3
     */
    public static class Configurator extends SimpleContextBuilderConfigurator {

        /**
         * Number of inspection.
         */
        private int count;

        /**
         * {@inheritDoc}
         */
        @Override
        public int internalConfigure(final ContextBuilder ctxBuilder) {
            ctxBuilder.inspector(new ObjectBuilderInspector() {
                @Override
                public <T> T inspect(final T object) {
                    count++;
                    return object;
                }
            });

            return super.internalConfigure(ctxBuilder);
        }
    }

    /**
     * <p>
     * A inspector injected via spring support.
     * </p>
     *
     * @author Guillaume DROUET
     * @since 0.5.3
     */
    public static class Inspector implements ObjectBuilderInspector {

        /**
         * Number of inspection.
         */
        private int count;

        /**
         * {@inheritDoc}
         */
        @Override
        public <T> T inspect(final T object) {
            count++;
            return object;
        }
    }

    /**
     * Just expose the protected method.
     *
     * @author Guillaume DROUET
     * @since 0.5.0
     */
    private static final class ExposedResourceHandlerRegistry extends ResourceHandlerRegistry {

        /**
         * Builds a new instance.
         */
        private ExposedResourceHandlerRegistry() {
            super(new GenericWebApplicationContext(), new MockServletContext());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected AbstractHandlerMapping getHandlerMapping() {
            return super.getHandlerMapping();
        }
    }

    /**
     * Resource pattern.
     */
    private static final String PATTERN = "/resources/**";

    /**
     * Registry.
     */
    private ExposedResourceHandlerRegistry registry;

    /**
     * Registration from registry.
     */
    private ResourceHandlerRegistration registration;

    /**
     * Facade used during tests.
     */
    private WuicFacade wuicFacade;

    /**
     * The {@link WuicFacadeBuilderFactory}.
     */
    @Autowired
    private WuicFacadeBuilderFactory wuicFacadeBuilderFactory;

    /**
     * The configurator bean.
     */
    @Autowired
    private Configurator configurator;

    /**
     * The inspector bean.
     */
    @Autowired
    private Inspector inspector;

    /**
     * The servlet context.
     */
    @Autowired
    private ServletContext servletContext;

    /**
     * Timeout.
     */
    @Rule
    public Timeout globalTimeout = Timeout.seconds(60);

    /**
     * Creates facade and registry.
     *
     * @throws Exception if test fails
     */
    @Before
    public void tearUp() throws Exception {
        wuicFacade = new WuicFacadeBuilder()
                .contextBuilder()
                .tag(getClass())
                .processContext(ProcessContext.DEFAULT)
                .contextNutDaoBuilder(ClasspathNutDao.class)
                .property(ApplicationConfig.BASE_PATH, "/statics")
                .toContext()
                .heap("foo", ContextBuilder.getDefaultBuilderId(ClasspathNutDao.class), new String[] { "foo.js" })
                .releaseTag()
                .toFacade()
                .build();

        registry = new ExposedResourceHandlerRegistry();
        registration = registry.addResourceHandler(PATTERN);
    }

    /**
     * <p>
     * Tests for {@link SpringNutDao}.
     * </p>
     *
     * @throws IOException if any I/O error occurs
     */
    @Test
    public void daoTest() throws IOException {
        final SpringNutDao dao = new SpringNutDao();
        dao.basePath("/statics");
        dao.init(false, false, null);
        dao.setServletContext(servletContext);
        dao.setNutTypeFactory(new NutTypeFactory(Charset.defaultCharset().displayName()));
        final List<Nut> nuts = dao.create("*.js", ProcessContext.DEFAULT);
        Assert.assertEquals(1, nuts.size());

        for (final Nut nut : nuts) {
            Assert.assertNotNull(nut.getParentFile());
            Assert.assertNotEquals(0L, (long) NutUtils.getVersionNumber(nut));
            dao.newInputStream(nut.getInitialName(), ProcessContext.DEFAULT).close();
            Assert.assertTrue(dao.exists(nut.getInitialName(), ProcessContext.DEFAULT));
        }

    }

    /**
     * Tests for {@link WuicPathResourceResolver}.
     *
     * @throws Exception if test fails
     */
    @Test
    public void resolverTest() throws Exception {
        registration.resourceChain(true).addResolver(new WuicPathResourceResolver(wuicFacade));
        final SimpleUrlHandlerMapping handlerMapping = (SimpleUrlHandlerMapping) this.registry.getHandlerMapping();
        Assert.assertNotNull("Expects that an handler mapping is registered", handlerMapping);
        ResourceHttpRequestHandler handler = (ResourceHttpRequestHandler) handlerMapping.getUrlMap().get(PATTERN);
        Assert.assertNotNull("Expects an handler matches pattern " + PATTERN, handler);
        final List<ResourceResolver> resolvers = handler.getResourceResolvers();
        Assert.assertEquals("Expects two resolvers: cache and path", 2, resolvers.size());
        Assert.assertNotNull(resolvers.get(1).resolveResource(new MockHttpServletRequest(), "/foo/aggregate.js", null, null));
        Assert.assertNull(resolvers.get(1).resolveResource(new MockHttpServletRequest(), "/foo/bad.js", null, null));
    }

    /**
     * Tests for {@link WuicVersionStrategy}.
     *
     * @throws Exception if test fails
     */
    @Test
    public void versionTest() throws Exception {
        final ResourceResolver mock = Mockito.mock(ResourceResolver.class);
        final Resource resource = new FileSystemResource(getClass().getResource("/statics/foo.js").getFile());
        Mockito.when(mock.resolveResource(Mockito.any(HttpServletRequest.class), Mockito.anyString(), Mockito.anyList(), Mockito.any(ResourceResolverChain.class)))
                .thenReturn(resource);
        final VersionResourceResolver versionResourceResolver = new VersionResourceResolver()
                .addVersionStrategy(new WuicVersionStrategy(), "/**/*");
        registration.addResourceLocations("classpath:/").resourceChain(true).addResolver(versionResourceResolver).addResolver(mock);
        final SimpleUrlHandlerMapping handlerMapping = (SimpleUrlHandlerMapping) this.registry.getHandlerMapping();
        Assert.assertNotNull("Expects that an handler mapping is registered", handlerMapping);
        ResourceHttpRequestHandler handler = (ResourceHttpRequestHandler) handlerMapping.getUrlMap().get(PATTERN);
        Assert.assertNotNull("Expects an handler matches pattern " + PATTERN, handler);
        final List<ResourceResolver> resolvers = handler.getResourceResolvers();
        final ResourceResolverChain chain = Mockito.mock(ResourceResolverChain.class);
        Mockito.when(chain.resolveUrlPath(Mockito.anyString(), Mockito.anyList())).thenReturn("foo/aggregate.js");
        Mockito.when(chain.resolveResource(Mockito.any(HttpServletRequest.class), Mockito.anyString(), Mockito.anyList())).thenReturn(resource);

        Assert.assertEquals("Expects four resolvers: cache, version, mocked path and path", 4, resolvers.size());
        Assert.assertEquals("foo/" + resource.lastModified() + "/aggregate.js", resolvers.get(1).resolveUrlPath("/foo/aggregate.js", null, chain));
    }

    /**
     * <p>
     * Tests that {@link ContextBuilderConfigurator} beans have been installed.
     * </p>
     *
     * @throws WuicException if test fails
     */
    @Test
    public void beanSupportTest() throws WuicException {
        wuicFacadeBuilderFactory.create().build();
        Assert.assertNotEquals(0, configurator.count);
        Assert.assertNotEquals(0, inspector.count);
    }

    /**
     * <p>
     * Tests that spring property sources are used.
     * </p>
     *
     * @throws WuicException if test fails
     */
    @Test
    public void propertySupportTest() throws WuicException  {
        final AtomicInteger countInspect = new AtomicInteger();
        final AtomicInteger countAggregate = new AtomicInteger();

        // We add this custom resolver after the spring resolver so if a supplied property is resolved from spring resolver, this instance is never called
        WuicServletContextListener.getWuicFacadeBuilder(servletContext).addPropertyResolver(new com.github.wuic.util.PropertyResolver() {

            /**
             * {@inheritDoc}
             */
            @Override
            public String resolveProperty(final String key) {
                if (ApplicationConfig.AGGREGATE.equals(key)) {
                    countAggregate.incrementAndGet();
                } else if (ApplicationConfig.INSPECT.equals(key)) {
                    countInspect.incrementAndGet();
                }

                return null;
            }
        });

        wuicFacadeBuilderFactory.create().build();

        // inspect property is defined in spring properties
        Assert.assertEquals(0, countInspect.get());

        // aggregate property is not defined in spring properties
        Assert.assertNotEquals(0, countAggregate.get());
    }
}
