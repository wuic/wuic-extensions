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


package com.github.wuic.engine.closure;

import com.github.wuic.ApplicationConfig;
import com.github.wuic.EnumNutType;
import com.github.wuic.NutType;
import com.github.wuic.config.Alias;
import com.github.wuic.config.BooleanConfigParam;
import com.github.wuic.config.Config;
import com.github.wuic.config.StringConfigParam;
import com.github.wuic.engine.EngineRequest;
import com.github.wuic.engine.EngineService;
import com.github.wuic.engine.EngineType;
import com.github.wuic.engine.core.AbstractCompressorEngine;
import com.github.wuic.engine.core.SourceMapLineInspector;
import com.github.wuic.exception.WuicException;
import com.github.wuic.nut.InMemoryNut;
import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.nut.SourceMapNutImpl;
import com.github.wuic.util.Input;
import com.github.wuic.util.Output;
import com.google.javascript.jscomp.AbstractCommandLineRunner;
import com.google.javascript.jscomp.CompilationLevel;
import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.SourceFile;

import java.io.CharArrayWriter;
import java.io.PrintStream;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;

/**
 * <p>
 * This class can compress Javascript files using the closure compiler library.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.3
 */
@EngineService(injectDefaultToWorkflow = true)
@Alias("closure")
public class ClosureCompilerEngine extends AbstractCompressorEngine {

    /**
     * Compilation level.
     */
    private CompilationLevel compilationLevel;

    /**
     * Debug compilation.
     */
    private Boolean debug;

    /**
     * <p>
     * Initializes a new instance.
     * </p>
     *
     * @param cl the compilation level
     * @param debug debugs the compilation
     */
    @Config
    public void init(@StringConfigParam(propertyKey = ApplicationConfig.COMPILATION_LEVEL, defaultValue = "SIMPLE_OPTIMIZATIONS") final String cl,
                     @BooleanConfigParam(propertyKey = ApplicationConfig.DEBUG_COMPILATION, defaultValue = false) final Boolean debug) {
        setRenameExtensionPrefix(".min");
        this.compilationLevel = CompilationLevel.valueOf(cl);
        this.debug = debug;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean transform(final Input source, final Output output, final ConvertibleNut convertibleNut, final EngineRequest request)
            throws IOException {

        // Configures options
        final CompilerOptions options = new CompilerOptions();
        options.setSourceMapOutputPath("");
        this.compilationLevel.setOptionsForCompilationLevel(options);

        if (debug) {
            this.compilationLevel.setDebugOptionsForCompilationLevel(options);
        }

        // Create the compressor using the source stream
        final String inputSource = convertibleNut.getInitialName();
        final ByteArrayOutputStream errorBos = new ByteArrayOutputStream();
        final PrintStream errorPs = new PrintStream(errorBos);
        final com.google.javascript.jscomp.Compiler compiler = new com.google.javascript.jscomp.Compiler(errorPs);

        // The nut name is used here so that any warnings or errors will cite line numbers in terms of input.
        SourceFile input = SourceFile.fromReader(inputSource, source.reader());

        // Compress the script into the temporary buffer
        compiler.compile(AbstractCommandLineRunner.getBuiltinExterns(CompilerOptions.Environment.BROWSER), Arrays.asList(input), options);
        errorPs.flush();

        final Writer target = output.writer();
        target.write(compiler.toSource());

        // Create source map
        final CharArrayWriter caw = new CharArrayWriter();
        final String sourceMapName = convertibleNut.getName() + ".map";

        compiler.getSourceMap().appendTo(caw, sourceMapName);
        caw.flush();
        final ConvertibleNut sourceMapNut =
                new InMemoryNut(caw.toCharArray(), sourceMapName, getNutTypeFactory().getNutType(EnumNutType.MAP), 0L, false);

        try {
            convertibleNut.setSource(new SourceMapNutImpl(
                    request.getHeap(), convertibleNut, sourceMapNut, request.getProcessContext(), false, getNutTypeFactory().getCharset()));
        } catch (WuicException we) {
            WuicException.throwStreamException(new IOException(we));
        }

        SourceMapLineInspector.writeSourceMapComment(request, target, sourceMapName);

        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<NutType> getNutTypes() {
        return Arrays.asList(getNutTypeFactory().getNutType(EnumNutType.JAVASCRIPT));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EngineType getEngineType() {
        return EngineType.MINIFICATION;
    }
}
