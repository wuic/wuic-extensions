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


package com.github.wuic.spring;

import com.github.wuic.nut.Nut;
import org.springframework.core.io.AbstractResource;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;

/**
 * <p>
 * This class exposes a {@link Nut} to spring through its {@code Resource} API.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.0
 */
public class WuicResource extends AbstractResource {

    /**
     * The nut behind the resource.
     */
    private Nut nut;

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param n the nut
     */
    public WuicResource(final Nut n) {
        nut = n;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return nut.getInitialName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream getInputStream() throws IOException {
        return nut.openStream().inputStream();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getFilename() {
        return nut.getInitialName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long lastModified() throws IOException {
        try {
            return nut.getVersionNumber().get();
        } catch (InterruptedException ie) {
            throw new IOException(ie);
        } catch (ExecutionException ee) {
            throw new IOException(ee);
        }
    }
}
