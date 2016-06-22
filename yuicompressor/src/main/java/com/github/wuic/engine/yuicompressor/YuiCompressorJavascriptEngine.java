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


package com.github.wuic.engine.yuicompressor;

import com.github.wuic.ApplicationConfig;
import com.github.wuic.NutType;
import com.github.wuic.config.Alias;
import com.github.wuic.config.BooleanConfigParam;
import com.github.wuic.config.Config;
import com.github.wuic.config.IntegerConfigParam;
import com.github.wuic.engine.EngineRequest;
import com.github.wuic.engine.EngineService;
import com.github.wuic.engine.EngineType;
import com.github.wuic.engine.core.AbstractCompressorEngine;
import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.util.IOUtils;
import com.yahoo.platform.yui.compressor.JavaScriptCompressor;

import java.io.Reader;
import java.io.Writer;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;

/**
 * <p>
 * This class can compress Javascript files using the YUI compressor library.
 * </p>
 * 
 * @author Guillaume DROUET
 * @since 0.1.0
 */
@EngineService(injectDefaultToWorkflow = true)
@Alias("yuicompressorJavascript")
public class YuiCompressorJavascriptEngine extends AbstractCompressorEngine {
    
    /**
     * Marker that identifies zones where characters are escaped.
     */
    private static final String ESCAPE_MARKER = "WUIC_ESCAPE_BACKSLASH";
    
    /**
     * Special characters (to prefix with a backslash) to be escaped before compression.
     */
    private static final String[] TO_ESCAPE = {"n", "t", "r"};
    
    /**
     * Position of line break insertion (-1 if not \n should be inserted).
     */
    private Integer lineBreakPos;

    /**
     * Disable micro-optimization.
     */
    private Boolean disableOptimization;

    /**
     * Be verbose when compressing.
     */
    private Boolean verbose;

    /**
     * Keep all unnecessary semicolons.
     */
    private Boolean preserveSemiColons;

    /**
     * Obfuscate the code or not.
     */
    private Boolean munge;

    /**
     * <p>
     * Initializes a new instance.
     * </p>
     *
     * @param lbp line break position
     * @param disableOptim disable micro optimizations
     * @param verb be verbose when compressing
     * @param keepSemiColons keep unnecessary semicolons
     * @param obfuscate obfuscate the code or not
     */
    @Config
    public void init(@IntegerConfigParam(propertyKey = ApplicationConfig.LINE_BREAK_POS, defaultValue = -1) final Integer lbp,
            @BooleanConfigParam(propertyKey = ApplicationConfig.DISABLE_OPTIMIZATIONS, defaultValue = true) final Boolean disableOptim,
            @BooleanConfigParam(propertyKey = ApplicationConfig.VERBOSE, defaultValue = false) final Boolean verb,
            @BooleanConfigParam(propertyKey = ApplicationConfig.PRESERVE_SEMICOLONS, defaultValue = true) final Boolean keepSemiColons,
            @BooleanConfigParam(propertyKey = ApplicationConfig.OBFUSCATE, defaultValue = true) final Boolean obfuscate) {
        setRenameExtensionPrefix(".min");
        lineBreakPos = lbp;
        disableOptimization = disableOptim;
        verbose = verb;
        preserveSemiColons = keepSemiColons;
        munge = obfuscate;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void transform(final InputStream source, final OutputStream target, final ConvertibleNut convertibleNut, final EngineRequest request)
            throws IOException {
        Reader in = null;
        StringWriter out = null;
        Writer targetOut = null;
        
        try {
            // Stream to read from the source
            in = new InputStreamReader(switchSpecialChars(source, Boolean.FALSE, request.getCharset()), request.getCharset());
            
            // Create the compressor using the source stream
            final JavaScriptCompressor compressor =
                    new JavaScriptCompressor(in, new JavascriptYuiCompressorErrorReporter(convertibleNut.getName()));
            
            // Now close the stream read
            in.close();
            in = null;
            
            // Write into a temporary buffer with escaped special characters
            out = new StringWriter();
            
            // Compress the script into the temporary buffer
            compressor.compress(out,
                    lineBreakPos,
                    munge,
                    verbose,
                    preserveSemiColons,
                    disableOptimization);
            
            // Stream to write into the target with backed special characters
            final InputStream bis = new ByteArrayInputStream(out.getBuffer().toString().getBytes());
            final InputStream restore = switchSpecialChars(bis, Boolean.TRUE, request.getCharset());
            targetOut = new OutputStreamWriter(target);

            IOUtils.copyStreamToWriterIoe(restore, targetOut, request.getCharset());
        } finally {
            IOUtils.close(in);
            IOUtils.close(out);
            IOUtils.close(targetOut);
        }
    }

    /**
     * <p>
     * This methods escapes or restore the \n, \r and \t characters of the given
     * stream and returns the result of the operation. Fixes escape issue with
     * YUICompressor. 
     * </p>
     * 
     * @param source the source to escape
     * @param restore flag that indicates if we escape or restore
     * @param charset the charset
     * @return the result
     * @throws IOException if an I/O error occurs
     */
    private InputStream switchSpecialChars(final InputStream source, final Boolean restore, final String charset) throws IOException {
        final Reader parser = new InputStreamReader(source, charset);
        ByteArrayOutputStream streamParser = null;
        
        try {
            streamParser = new ByteArrayOutputStream();
            final char[] buffer = new char[com.github.wuic.util.IOUtils.WUIC_BUFFER_LEN];
            int offset;
            
            while ((offset = parser.read(buffer)) != -1) {
                String read = new String(buffer, 0, offset);
                
                for (String c : TO_ESCAPE) {
                    if (restore) {
                        read = read.replace(ESCAPE_MARKER + c, "\\" + c);
                    } else {
                        read = read.replace("\\" + c, ESCAPE_MARKER + c);
                    }
                }
                
                streamParser.write(read.getBytes());
            }
        } finally {
            IOUtils.close(streamParser);
        }
        
        return new ByteArrayInputStream(streamParser.toByteArray());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<NutType> getNutTypes() {
        return Arrays.asList(NutType.JAVASCRIPT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EngineType getEngineType() {
        return EngineType.MINIFICATION;
    }
}
