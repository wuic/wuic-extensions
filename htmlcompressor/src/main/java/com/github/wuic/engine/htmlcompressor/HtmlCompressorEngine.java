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


package com.github.wuic.engine.htmlcompressor;

import com.github.wuic.EnumNutType;
import com.github.wuic.NutType;
import com.github.wuic.config.Alias;
import com.github.wuic.config.BooleanConfigParam;
import com.github.wuic.config.Config;
import com.github.wuic.engine.EngineRequest;
import com.github.wuic.engine.EngineService;
import com.github.wuic.engine.EngineType;
import com.github.wuic.engine.core.AbstractCompressorEngine;
import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.util.Input;
import com.github.wuic.util.Output;
import com.googlecode.htmlcompressor.compressor.HtmlCompressor;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static com.github.wuic.ApplicationConfig.PRESERVE_LINE_BREAK;

/**
 * <p>
 * This engine is based on HTML compressor to be able to compress HTML code.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.0
 */
@EngineService(injectDefaultToWorkflow = true)
@Alias("htmlcompressor")
public class HtmlCompressorEngine extends AbstractCompressorEngine {

    /**
     * Compressor.
     */
    private HtmlCompressor compressor;

    /**
     * <p>
     * Initializes a new instance.
     * </p>
     *
     * @param preserveLb preserve the line break points or not
     */
    @Config
    public void preserveLineBreak(@BooleanConfigParam(propertyKey = PRESERVE_LINE_BREAK, defaultValue = false) Boolean preserveLb) {
        compressor = new HtmlCompressor();
        compressor.setPreserveLineBreaks(preserveLb);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<NutType> getNutTypes() {
        return Arrays.asList(getNutTypeFactory().getNutType(EnumNutType.HTML));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EngineType getEngineType() {
        return EngineType.MINIFICATION;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean transform(final Input is, final Output os, final ConvertibleNut convertible, final EngineRequest request) throws IOException {
        os.writer().write(compressor.compress(is.execution().toString()));
        return true;
    }
}
