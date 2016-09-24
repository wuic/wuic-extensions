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


package com.github.wuic.engine.yuicompressor;

import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.EvaluatorException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * {@code ErrorReporter} which logs warning and errors when some Javascript code
 * is compressed using the YUI Compressor library.
 * </p>
 * 
 * @author Guillaume DROUET
 * @since 0.1.0
 */
public class JavascriptYuiCompressorErrorReporter implements ErrorReporter {
    
    /**
     * Logger.
     */
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * The nut name associated to the compressed content.
     */
    private final String nutName;

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param nutName the nut name
     */
    public JavascriptYuiCompressorErrorReporter(final String nutName) {
        this.nutName = nutName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void warning(final String message,
            final String sourceName,
            final int line,
            final String lineSource,
            final int lineOffset) {
        if (line < 0) {
            log.debug("Nut name: {} - Source name: {} - Message: {} ", nutName, sourceName, message);
        } else {
            log.debug("Nut name: {} - Line Number: {} - Column: {} - Message: {} ", nutName, sourceName, line, lineOffset, message);
        }
    }
 
    /**
     * {@inheritDoc}
     */
    @Override
    public void error(final String message,
            final String sourceName,
            final int line,
            final String
            lineSource,
            final int lineOffset) {
        if (line < 0) {
            log.error("Source name : {} - Message : {} ", sourceName, message);
        } else {
            log.error("Source name : {} - Line Number : {} - Column : {} - Message : {} ", sourceName, line, lineOffset, message);
        }
    }
 
    /**
     * {@inheritDoc}
     */
    public EvaluatorException runtimeError(final String message,
            final String sourceName,
            final int line,
            final String lineSource,
            final int lineOffset) {
        error(message, sourceName, line, lineSource, lineOffset);
        return new EvaluatorException(message);
    }
}
