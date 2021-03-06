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


package com.github.wuic.engine.attoparser;

import com.github.wuic.ApplicationConfig;
import com.github.wuic.EnumNutType;
import com.github.wuic.NutType;
import com.github.wuic.config.Alias;
import com.github.wuic.config.BooleanConfigParam;
import com.github.wuic.config.Config;
import com.github.wuic.engine.EngineRequest;
import com.github.wuic.engine.EngineService;
import com.github.wuic.engine.EngineType;
import com.github.wuic.engine.core.AbstractCompressorEngine;
import com.github.wuic.exception.WuicException;
import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.util.Input;
import com.github.wuic.util.Output;
import org.attoparser.IMarkupHandler;
import org.attoparser.ParseException;
import org.attoparser.minimize.MinimizeHtmlMarkupHandler;
import org.attoparser.output.OutputMarkupHandler;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;

/**
 * <p>
 * This engine minify the HTML code thanks to AttoParser library.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.3
 */
@EngineService(injectDefaultToWorkflow = true)
@Alias("attoparserCompressor")
public class AttoParserHtmlMinimizerEngine extends AbstractCompressorEngine {

    /**
     * Minimize mode (white spaces only or not).
     */
    private MinimizeHtmlMarkupHandler.MinimizeMode minimizeMode;

    /**
     * <p>
     * Initializes this instance.
     * </p>
     *
     * @param whiteSpaceOnly removes only white spaces or not
     * @param compress compress ot not
     */
    @Config
    public void init(@BooleanConfigParam(propertyKey = ApplicationConfig.WHITE_SPACE_ONLY, defaultValue = false) Boolean whiteSpaceOnly,
                     @BooleanConfigParam(propertyKey = ApplicationConfig.COMPRESS, defaultValue = true) Boolean compress) {
        init(compress);
        this.minimizeMode = whiteSpaceOnly ? MinimizeHtmlMarkupHandler.MinimizeMode.ONLY_WHITE_SPACE : MinimizeHtmlMarkupHandler.MinimizeMode.COMPLETE;
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
    public boolean transform(final Input is,
                             final Output os,
                             final ConvertibleNut nut,
                             final EngineRequest request)
            throws IOException {
        // The output handler will be the last in the handler chain
        final Writer osw = os.writer();
        final IMarkupHandler outputHandler = new OutputMarkupHandler(osw);

        // The minimizer handler will do its job before events reach output handler
        final IMarkupHandler handler = new MinimizeHtmlMarkupHandler(minimizeMode, outputHandler);

        try {
            AssetsMarkupAttoParser.PARSER.parse(is.reader(), handler);
            osw.flush();
        } catch (ParseException pe) {
            WuicException.throwBadArgumentException(pe);
        }

        return true;
    }
}
