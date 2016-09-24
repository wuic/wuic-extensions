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


package com.github.wuic.nut.dao.spring;

import com.github.wuic.NutType;
import com.github.wuic.nut.AbstractNut;
import com.github.wuic.util.DefaultInput;
import com.github.wuic.util.FutureLong;
import com.github.wuic.util.Input;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

import java.io.IOException;

import org.slf4j.Logger;

/**
 * <p>
 * This {@link com.github.wuic.nut.Nut} is based on the {@link Resource} class from spring framework.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.3
 */
public class ResourceNut extends AbstractNut {

    /**
     * The logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceNut.class);

    /**
     * The spring resource.
     */
    private final Resource resource;

    /**
     * The parent.
     */
    private final String parent;

    /**
     * The charset.
     */
    private final String charset;

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param name the resource name
     * @param ft the file type
     * @param resource the resource
     * @param cs the charset
     */
    public ResourceNut(final NutType ft, final String name, final Resource resource, final String cs) {
        super(name, ft, new FutureLong(getVersionNumber(resource)));
        this.resource = resource;
        parent = getParent(resource);
        charset = cs;
    }

    /**
     * <p>
     * Gets the version number and swallow any {@code IOException}.
     * </p>
     *
     * @param resource the resource
     * @return the version number, {@code 0} if {@code IOException} is thrown
     */
    private static long getVersionNumber(final Resource resource) {
        try {
            return resource.lastModified();
        } catch (IOException ioe) {
            LOGGER.info("Unable to resolve version number of {}", resource.getFilename());
            LOGGER.debug("Detailed error.", ioe);
            return 0L;
        }
    }

    /**
     * <p>
     * Gets the parent file and swallow any {@code IOException}.
     * </p>
     *
     * @param resource the resource
     * @return the parent, {@code null} if {@code IOException} is thrown
     */
    private static String getParent(final Resource resource) {
        try {
            return resource.getFile().getParentFile().getAbsolutePath();
        } catch (IOException ioe) {
            LOGGER.info("Unable to resolve parent of {}", resource.getFilename());
            LOGGER.debug("Detailed error.", ioe);
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Input openStream() throws IOException {
        return new DefaultInput(resource.getInputStream(), charset);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getParentFile() {
        return parent;
    }
}
