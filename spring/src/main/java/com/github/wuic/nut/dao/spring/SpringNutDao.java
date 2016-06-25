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


package com.github.wuic.nut.dao.spring;

import com.github.wuic.ApplicationConfig;
import com.github.wuic.NutType;
import com.github.wuic.ProcessContext;
import com.github.wuic.config.Alias;
import com.github.wuic.config.Config;
import com.github.wuic.config.StringConfigParam;
import com.github.wuic.exception.WuicException;
import com.github.wuic.nut.AbstractNutDao;
import com.github.wuic.nut.Nut;
import com.github.wuic.nut.dao.NutDaoService;
import com.github.wuic.nut.dao.servlet.ServletContextHandler;
import com.github.wuic.util.IOUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.web.context.support.ServletContextResourcePatternResolver;

import javax.servlet.ServletContext;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * This DAO relies on the {@link PathMatchingResourcePatternResolver} from spring framework to access nuts.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.3
 */
@NutDaoService
@Alias("spring")
public class SpringNutDao extends AbstractNutDao implements ServletContextHandler {

    /**
     * The resolver.
     */
    private ResourcePatternResolver resolver;

    /**
     * The base path.
     */
    private Resource basePath;

    /**
     * <p>
     * Initialized the base path.
     * </p>
     *
     * @param base the base path
     */
    @Config
    public void basePath(@StringConfigParam(defaultValue = "classpath:", propertyKey = ApplicationConfig.BASE_PATH) final String base) {
        super.init(base, null, -1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setServletContext(final ServletContext sc) {
        resolver = new ServletContextResourcePatternResolver(sc);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isRelative(final String path) {
        // absolute paths are handled in a quite specific way in spring and can be delegated to it
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected List<String> listNutsPaths(final String pattern) throws IOException {
        checkBasePath();
        return toStringList(resolver.getResources(IOUtils.mergePath(getBasePath(), pattern)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Nut accessFor(final String realPath, final NutType type, final ProcessContext processContext) throws IOException {
        checkBasePath();
        return new ResourceNut(type, realPath, resolver.getResource(IOUtils.mergePath(getBasePath(), realPath)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream newInputStream(final String path, final ProcessContext processContext) throws IOException {
        checkBasePath();
        return resolver.getResource(IOUtils.mergePath(getBasePath(), path)).getInputStream();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean exists(final String path, final ProcessContext processContext) throws IOException {
        checkBasePath();
        return resolver.getResource(IOUtils.mergePath(getBasePath(), path)).exists();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Long getLastUpdateTimestampFor(final String path) throws IOException {
        checkBasePath();
        return resolver.getResource(IOUtils.mergePath(getBasePath(), path)).lastModified();
    }

    /**
     * <p>
     * Creates a list of all file names collected from the given resource array.
     * </p>
     *
     * @param resources the resources
     * @return the list
     * @throws IOException if resource can't be resolved as URL
     */
    private List<String> toStringList(final Resource ... resources) throws IOException {
        final List<String> retval = new ArrayList<String>(resources.length);

        for (final Resource resource : resources) {
            final String url = resource.getURL().toString();
            final String base = basePath.getURL().toString();
            final int basePathIndex = url.indexOf(base);
            retval.add(basePathIndex == -1 ? resource.getFilename() : url.substring(basePathIndex + base.length()));
        }

        return retval;
    }

    /**
     * <p>
     * Checks that {@link #resolver} has been initialized. If the member is not {@code null}, the method checks if the
     * base path has been initialized and do it it's not the case. Then checks that it exists.
     * </p>
     */
    private void checkBasePath() {
        if (resolver == null) {
            WuicException.throwBadStateException(new IllegalArgumentException(
                    String.format("%s not initialized. Seems setServletContext(ServletContext) has not been called", getClass().getName())));
        }

        if (basePath == null) {
            basePath = resolver.getResource(getBasePath());
        }

        if (!basePath.exists()) {
            WuicException.throwBadArgumentException(new IllegalArgumentException(String.format("%s does not exists.", getBasePath())));
        }
    }
}
