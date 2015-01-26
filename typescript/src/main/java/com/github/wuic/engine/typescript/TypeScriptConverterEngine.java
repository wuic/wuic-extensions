/*
* "Copyright (c) 2015   Capgemini Technology Services (hereinafter "Capgemini")
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


package com.github.wuic.engine.typescript;

import com.github.wuic.ApplicationConfig;
import com.github.wuic.NutType;
import com.github.wuic.config.BooleanConfigParam;
import com.github.wuic.config.ConfigConstructor;
import com.github.wuic.engine.EngineRequest;
import com.github.wuic.engine.EngineService;
import com.github.wuic.engine.EngineType;
import com.github.wuic.engine.core.AbstractConverterEngine;
import com.github.wuic.engine.core.TextAggregatorEngine;
import com.github.wuic.nut.ByteArrayNut;
import com.github.wuic.nut.CompositeNut;
import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.util.IOUtils;
import com.github.wuic.util.NumberUtils;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ro.isdc.wro.WroRuntimeException;
import ro.isdc.wro.extensions.processor.js.NodeTypeScriptProcessor;
import ro.isdc.wro.extensions.processor.js.RhinoTypeScriptProcessor;
import ro.isdc.wro.model.resource.Resource;
import ro.isdc.wro.util.WroUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.atomic.AtomicReference;

/**
 * <p>
 * This class can convert Typescript files using the Typescript4j library.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.0
 * @since 0.5.1
 */
@EngineService(injectDefaultToWorkflow = true)
public class TypeScriptConverterEngine extends AbstractConverterEngine {

    /**
     * The logger.
     */
    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * The node.js based compiler.
     */
    private final NodeTypeScriptProcessor nodeProcessor;

    /**
     * The rhino based compiler
     */
    private final RhinoTypeScriptProcessor rhinoTypeScriptProcessor;

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param convert      if this engine is enabled or not
     * @param useNodeJs    use node.js command line or not
     * @param asynchronous computes version number asynchronously or not
     */
    @ConfigConstructor
    public TypeScriptConverterEngine(@BooleanConfigParam(propertyKey = ApplicationConfig.CONVERT, defaultValue = true) final Boolean convert,
                                     @BooleanConfigParam(propertyKey = ApplicationConfig.USE_NODE_JS, defaultValue = false) final Boolean useNodeJs,
                                     @BooleanConfigParam(propertyKey = ApplicationConfig.COMPUTE_VERSION_ASYNCHRONOUSLY, defaultValue = true) final Boolean asynchronous) {
        super(convert, asynchronous);

        if (useNodeJs) {
            final String osName = System.getProperty("os.name");
            final boolean isWindows = osName != null && osName.contains("Windows");
            nodeProcessor = new InternalNodeProcessor(isWindows);
            rhinoTypeScriptProcessor = null;
        } else {
            nodeProcessor = null;
            rhinoTypeScriptProcessor = new RhinoTypeScriptProcessor();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected NutType targetNutType() {
        return NutType.JAVASCRIPT;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<NutType> getNutTypes() {
        return Arrays.asList(NutType.TYPESCRIPT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EngineType getEngineType() {
        return EngineType.CONVERTER;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void transform(final InputStream is, final OutputStream os, final ConvertibleNut nut, final EngineRequest request)
            throws IOException {
        if (nodeProcessor == null) {
            rhinoTypeScriptProcessor.process(new InputStreamReader(is), new OutputStreamWriter(os));
        } else {
            // Compile typescript and read generated source map file
            final ByteArrayOutputStream bos = new ByteArrayOutputStream();

            // Read the entire combined stream and write in separate file each nut of the composition
            final InternalInputStreamReader isr = new InternalInputStreamReader(is, request);

            nodeProcessor.process(isr, new OutputStreamWriter(bos));

            if (!request.isBestEffort()) {
                for (final ConvertibleNut n : isr.getRefNuts()) {
                    nut.addReferencedNut(n);
                }
            }

            IOUtils.copyStream(new ByteArrayInputStream(bos.toByteArray()), os);
        }
    }

    /**
     * <p>
     * An internal processor that deals with source map generation and multiple files compilation.
     * </p>
     *
     * @author Guillaume DROUET
     * @version 1.0
     * @since 0.5.1
     */
    private final class InternalNodeProcessor extends NodeTypeScriptProcessor {

        /**
         * Windows or not.
         */
        private final boolean isWindows;

        /**
         * <p>
         * Builds a new instance
         * </p>
         *
         * @param windows {@code true} if we run on windows, {@code false} otherwise
         */
        private InternalNodeProcessor(final boolean windows) {
            isWindows = windows;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void process(final Resource resource, final Reader reader, final Writer writer) throws IOException {
            final InternalInputStreamReader internal = InternalInputStreamReader.class.cast(reader);
            InputStream sourceMapInputStream = null;
            InputStream resultInputStream = null;
            final AtomicReference<OutputStream> out = new AtomicReference<OutputStream>();
            final List<String> pathsToCompile = internal.getPathsToCompile();

            try {
                final File compilationResult = new File(internal.getWorkingDir(), TextAggregatorEngine.aggregationName(NutType.JAVASCRIPT));
                log.debug("absolute path: {}", internal.getWorkingDir().getAbsolutePath());

                // Do not generate source map if we are in best effort
                final boolean be = internal.getEngineRequest().isBestEffort();

                // Creates the command line to execute tsc tool
                final int startIndex = (isWindows ? NumberUtils.FOUR : NumberUtils.TWO) - (be ? 1 : 0);
                final String[] commandLine = new String[NumberUtils.TWO + startIndex + pathsToCompile.size()];

                if (isWindows) {
                    commandLine[0] = "cmd";
                    commandLine[1] = "/c";
                    commandLine[NumberUtils.TWO] = "tsc";

                    if (!be) {
                        commandLine[NumberUtils.THREE] = "--sourcemap";
                    }
                } else {
                    commandLine[0] = "tsc";

                    if (!be) {
                        commandLine[1] = "--sourcemap";
                    }
                }

                for (int i = 0; i < pathsToCompile.size(); i++) {
                    commandLine[i + startIndex] = pathsToCompile.get(i);
                }

                commandLine[commandLine.length - NumberUtils.TWO] = "--out";
                commandLine[commandLine.length - 1] = compilationResult.getAbsolutePath();

                log.debug("CommandLine arguments: {}", Arrays.asList(commandLine));
                final Process process = new ProcessBuilder(commandLine).redirectErrorStream(true).start();

                // Execute tsc tool to generate the source map and javascript file
                // this won't return till `out' stream being flushed!
                final int exitStatus = process.waitFor();
                resultInputStream = new FileInputStream(compilationResult);
                IOUtils.copyStreamToWriterIoe(resultInputStream, writer, "UTF-8");

                if (exitStatus != 0) {
                    log.error("exitStatus: {}", exitStatus);
                    String errorMessage = org.apache.commons.io.IOUtils.toString(process.getInputStream());
                    throw new WroRuntimeException(errorMessage).logError();
                }

                // Read the generated source map
                if (!be) {
                    final File sourceMapFile = new File(compilationResult.getAbsolutePath() + ".map");
                    sourceMapInputStream = new FileInputStream(sourceMapFile);
                    final String sourceMapName = sourceMapFile.getName();
                    final ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    IOUtils.copyStream(sourceMapInputStream, bos);
                    final ConvertibleNut sourceMapNut = new ByteArrayNut(bos.toByteArray(), sourceMapName, NutType.MAP, 0L);
                    internal.getRefNuts().add(sourceMapNut);
                }
            } catch (final Exception e) {
                throw WroRuntimeException.wrap(e);
            } finally {
                // Free resources
                IOUtils.close(sourceMapInputStream, out.get(), resultInputStream);
                FileUtils.deleteQuietly(internal.getWorkingDir());

                // return for later reuse
                reader.close();
                writer.close();
            }
        }
    }

    /**
     * <p>
     * This internal class provides extra features related to {@link CompositeNut.CompositeInputStream} class.
     * This will help to read a set of combined steams and be able to write each stream in a separate file.
     * </p>
     *
     * @author Guillaume DROUET
     * @version 1.0
     * @since 0.5.1
     */
    private final class InternalInputStreamReader extends InputStreamReader implements Observer {

        /**
         * An reference to the current written output stream.
         */
        private final AtomicReference<OutputStream> out;

        /**
         * The composite stream.
         */
        private final CompositeNut.CompositeInputStream cn;

        /**
         * Directory where files are written.
         */
        private final File workingDir;

        /**
         * Paths to compile.
         */
        private final List<String> pathsToCompile;

        /**
         * Referenced nuts.
         */
        private final List<ConvertibleNut> refNuts;

        /**
         * The engine request.
         */
        private final EngineRequest engineRequest;

        /**
         * <p>
         * Builds a new instance with an expected {@link CompositeNut.CompositeInputStream}.
         * </p>
         *
         * @param in an instance of {@link CompositeNut.CompositeInputStream}
         * @param request the request bound to this reader
         * @throws IOException if any I/O error occurs
         */
        private InternalInputStreamReader(final InputStream in, final EngineRequest request)
            throws IOException {
            super(in);

            if (in instanceof CompositeNut.CompositeInputStream) {
                cn = CompositeNut.CompositeInputStream.class.cast(in);
            } else {
                throw new IllegalArgumentException("Nut must be an instance of " + CompositeNut.CompositeInputStream.class.getName());
            }

            out = new AtomicReference<OutputStream>();
            workingDir = WroUtil.createTempDirectory();
            refNuts = new ArrayList<ConvertibleNut>();
            pathsToCompile = new ArrayList<String>();
            engineRequest = request;

            // Read the stream and collect referenced nuts
            cn.addObserver(this);
            IOUtils.copyStream(cn, new OutputStream() {
                @Override
                public void write(final int b) throws IOException {
                }
            });
            cn.removeObserver(this);
        }

        /**
         * <p>
         * Gets the paths to compile.
         * </p>
         *
         * @return the files path
         */
        private List<String> getPathsToCompile() {
            return pathsToCompile;
        }

        /**
         * <p>
         * Gets the collected nuts.
         * </p>
         *
         * @return the nuts to be referenced
         */
        private List<ConvertibleNut> getRefNuts() {
            return refNuts;
        }

        /**
         * <p>
         * Gets the directory where files are written.
         * </p>
         *
         * @return the working directory
         */
        private File getWorkingDir() {
            return workingDir;
        }

        /**
         * <p>
         * Gets the request.
         * </p>
         *
         * @return the request
         */
        private EngineRequest getEngineRequest() {
            return engineRequest;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void update(final Observable o, final Object arg) {
            final CompositeNut.CompositeInputStreamReadEvent e = CompositeNut.CompositeInputStreamReadEvent.class.cast(arg);

            try {
                OutputStream os = out.get();

                // Write new nut
                if (os == null) {
                    final File path = new File(workingDir, e.getNut().getName());
                    os = new FileOutputStream(path);
                    out.set(os);
                    os.write(e.getRead());
                    pathsToCompile.add(path.getAbsolutePath());
                } else if (e.getRead() == -1) {
                    // End of copy for current nut
                    os.close();
                    refNuts.add(e.getNut());
                    out.set(null);
                } else {
                    // Copying
                    os.write(e.getRead());
                }
            } catch (IOException ioe) {
                throw new IllegalStateException(ioe);
            }
        }
    }
}
