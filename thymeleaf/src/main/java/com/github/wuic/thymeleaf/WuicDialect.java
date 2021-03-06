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

import com.github.wuic.WuicFacade;
import com.github.wuic.util.UrlProviderFactory;
import com.github.wuic.util.UrlUtils;
import org.thymeleaf.dialect.AbstractDialect;
import org.thymeleaf.processor.IProcessor;

import java.util.HashSet;
import java.util.Set;

/**
 * <p>
 * The thymeleaf dialect for WUIC. Actually creates the only one "import" processor.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.4.1
 */
public class WuicDialect extends AbstractDialect {


    /**
     * The resource URL provider from spring.
     */
    private final UrlProviderFactory urlProviderFactory;

    /**
     * The facade.
     */
    private final WuicFacade wuicFacade;

    /**
     * <p>
     * Builds a new instance
     * </p>
     *
     * @param up the URL provider
     * @param wf the underlying {@link WuicFacade}
     */
    public WuicDialect(final UrlProviderFactory up, final WuicFacade wf) {
        this.urlProviderFactory = up;
        this.wuicFacade = wf;
    }

    /**
     * <p>
     * Builds a new instance
     * </p>
     *
     * @param wf the underlying {@link WuicFacade}
     */
    public WuicDialect(final WuicFacade wf) {
        this(UrlUtils.urlProviderFactory(), wf);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPrefix() {
        return "wuic";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<IProcessor> getProcessors() {
        final Set<IProcessor> processors = new HashSet<IProcessor>();
        processors.add(new ImportProcessor(urlProviderFactory, wuicFacade));
        processors.add(new ConfigProcessor(wuicFacade));
        return processors;
    }
}
