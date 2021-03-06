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


package com.github.wuic.nut.dao.webjars;

import com.github.wuic.config.Alias;
import com.github.wuic.config.BooleanConfigParam;
import com.github.wuic.config.Config;
import com.github.wuic.config.ObjectConfigParam;
import com.github.wuic.config.StringConfigParam;
import com.github.wuic.exception.WuicException;
import com.github.wuic.nut.dao.NutDaoService;
import com.github.wuic.nut.dao.core.PathNutDao;
import com.github.wuic.nut.setter.ProxyUrisPropertySetter;
import com.github.wuic.path.DirectoryPath;
import com.github.wuic.path.Path;
import com.github.wuic.util.IOUtils;
import com.github.wuic.webjars.path.WebJarDirectoryPathFactory;
import org.webjars.WebJarAssetLocator;

import java.io.IOException;

import static com.github.wuic.ApplicationConfig.BASE_PATH;
import static com.github.wuic.ApplicationConfig.PROXY_URIS;
import static com.github.wuic.ApplicationConfig.REGEX;
import static com.github.wuic.ApplicationConfig.WILDCARD;

/**
 * <p>
 * This DAO relies on webjar protocol to resolve nut paths.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.3
 */
@NutDaoService
@Alias("webjar")
public class WebJarNutDao extends PathNutDao {

    /**
     * The WebJar locator.
     */
    private WebJarAssetLocator webJarAssetLocator;

    /**
     * <p>
     * Initializes a new instance.
     * </p>
     *
     * @param base                      the directory where we have to look up
     * @param proxies                   the proxies URIs in front of the nut
     * @param regex                     if the path should be considered as a regex or not
     * @param wildcard if the path should be considered as a wildcard or not
     */
    @Config
    public void init(@StringConfigParam(defaultValue = "/", propertyKey = BASE_PATH) final String base,
                     @ObjectConfigParam(defaultValue = "", propertyKey = PROXY_URIS, setter = ProxyUrisPropertySetter.class) final String[] proxies,
                     @BooleanConfigParam(defaultValue = false, propertyKey = REGEX) final Boolean regex,
                     @BooleanConfigParam(defaultValue = false, propertyKey = WILDCARD) final Boolean wildcard) {
        // Do not poll webjars
        init(base, proxies, -1, regex, wildcard);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DirectoryPath createBaseDirectory() throws IOException {
        final Path file = IOUtils.buildPath(getBasePath(), new WebJarDirectoryPathFactory(webJarAssetLocator, getCharset()));

        if (!(file instanceof DirectoryPath)) {
            WuicException.throwBadStateException(
                    new IllegalArgumentException(String.format("%s is not a directory", getBasePath())));
        }

        return DirectoryPath.class.cast(file);
    }

    /**
     * <p>
     * Sets the asset locator.
     * </p>
     *
     * @param webJarAssetLocator the asset locator
     */
    public void setWebJarAssetLocator(final WebJarAssetLocator webJarAssetLocator) {
        this.webJarAssetLocator = webJarAssetLocator;
    }
}
