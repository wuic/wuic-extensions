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
import com.github.wuic.config.StringConfigParam;
import com.github.wuic.engine.EngineRequest;
import com.github.wuic.engine.EngineService;
import com.github.wuic.engine.core.AbstractConverterEngine;
import com.github.wuic.engine.core.TextAggregatorEngine;
import com.github.wuic.exception.WuicException;
import com.github.wuic.nut.ByteArrayNut;
import com.github.wuic.nut.CompositeNut;
import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.util.IOUtils;
import com.github.wuic.util.NutDiskStore;
import io.apigee.trireme.core.NodeEnvironment;
import io.apigee.trireme.core.NodeException;
import io.apigee.trireme.core.NodeScript;
import io.apigee.trireme.core.ScriptFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ExecutionException;
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
     * ARGS file name.
     */
    private static final String ARGS_FILE = "args.txt";

    /**
     * The logger.
     */
    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * The ECMA script version.
     */
    private final String ecmaScriptVersion;

    /**
     * Windows or not.
     */
    private final boolean isWindows;

    /**
     * Node environment to run tsc on top of rhino.
     */
    private NodeEnvironment env = new NodeEnvironment();

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param convert       if this engine is enabled or not
     * @param esv           the ECMA script version
     * @param useNodeJs     use node.js command line or not
     * @param asynchronous  computes version number asynchronously or not
     */
    @ConfigConstructor
    public TypeScriptConverterEngine(@BooleanConfigParam(propertyKey = ApplicationConfig.CONVERT, defaultValue = true) final Boolean convert,
                                     @StringConfigParam(propertyKey = ApplicationConfig.ECMA_SCRIPT_VERSION, defaultValue = "ES3") final String esv,
                                     @BooleanConfigParam(propertyKey = ApplicationConfig.USE_NODE_JS, defaultValue = false) final Boolean useNodeJs,
                                     @BooleanConfigParam(propertyKey = ApplicationConfig.COMPUTE_VERSION_ASYNCHRONOUSLY, defaultValue = true) final Boolean asynchronous) {
        super(convert, asynchronous);
        ecmaScriptVersion = esv;
        final String osName = System.getProperty("os.name");
        isWindows = osName != null && osName.contains("Windows");

        if (useNodeJs) {
            env = new NodeEnvironment();
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
    public void transform(final InputStream is, final OutputStream os, final ConvertibleNut nut, final EngineRequest request)
            throws IOException {
        final InternalInputStreamReader internal = new InternalInputStreamReader(is);

        // Resources to clean
        InputStream sourceMapInputStream = null;
        InputStream resultInputStream = null;
        final File workingDir = NutDiskStore.INSTANCE.getWorkingDirectory();
        final File compilationResult = new File(workingDir, TextAggregatorEngine.aggregationName(NutType.JAVASCRIPT));
        final File sourceMapFile = new File(compilationResult.getAbsolutePath() + ".map");

        final AtomicReference<OutputStream> out = new AtomicReference<OutputStream>();
        final List<String> pathsToCompile = internal.getPathsToCompile();

        try {
            log.debug("absolute path: {}", workingDir.getAbsolutePath());

            // Do not generate source map if we are in best effort
            final boolean be = request.isBestEffort();

            if (env == null) {
                node(workingDir, pathsToCompile, compilationResult, be);
            } else {
                rhino(workingDir, pathsToCompile, compilationResult, be);
            }

            if (!compilationResult.exists()) {
                log.error("{} does not exists, which means that some errors break compilation. Check log above to see them.");
                throw new IOException("Typescript compilation fails, check logs for details");
            }

            resultInputStream = new FileInputStream(compilationResult);
            IOUtils.copyStream(resultInputStream, os);

            // Read the generated source map
            if (!be) {
                sourceMapInputStream = new FileInputStream(sourceMapFile);
                final String sourceMapName = sourceMapFile.getName();
                final ByteArrayOutputStream bos = new ByteArrayOutputStream();
                IOUtils.copyStream(sourceMapInputStream, bos);
                final ConvertibleNut sourceMapNut = new ByteArrayNut(bos.toByteArray(), sourceMapName, NutType.MAP, 0L);
                internal.getRefNuts().add(sourceMapNut);
            }
        } catch (final Exception e) {
            throw new IOException(e);
        } finally {
            // Free resources
            IOUtils.close(sourceMapInputStream, out.get(), resultInputStream);
            IOUtils.delete(compilationResult);
            IOUtils.delete(sourceMapFile);
        }
    }

    /**
     * <p>
     * Runs tsc compiler command ('npm install -g typescript' is required).
     * </p>
     *
     * @param workingDir the working directory
     * @param pathsToCompile paths of files to compile
     * @param compilationResult compilation result
     * @param be best effort mode or not
     * @throws IOException if a copy/read IO operation fails
     * @throws InterruptedException if command execution is interrupted
     */
    private void node(final File workingDir, final List<String> pathsToCompile, final File compilationResult, final Boolean be)
            throws IOException, InterruptedException {
        OutputStream argsOutputStream = null;

        try {
            // Creates the command line to execute tsc tool
            final String[] commandLine = isWindows ?
                    new String[] { "cmd", "/c", "tsc", '@' + ARGS_FILE, } : new String[] { "tsc", '@' + ARGS_FILE};

            final File argsFile = new File(workingDir, ARGS_FILE);
            argsOutputStream = new FileOutputStream(argsFile);

            for (final String pathToCompile : pathsToCompile) {
                argsOutputStream.write((pathToCompile + " ").getBytes());
            }

            if (!be) {
                argsOutputStream.write("--sourcemap".getBytes());
            }

            argsOutputStream.write((" -t " + ecmaScriptVersion + " --out ").getBytes());
            argsOutputStream.write(compilationResult.getAbsolutePath().getBytes());
            IOUtils.close(argsOutputStream);

            log.debug("CommandLine arguments: {}", Arrays.asList(commandLine));
            final Process process = new ProcessBuilder(commandLine)
                    .directory(workingDir)
                    .redirectErrorStream(true)
                    .start();
            final String errorMessage = IOUtils.readString(new InputStreamReader(process.getInputStream()));

            // Execute tsc tool to generate the source map and javascript file
            // this won't return till `out' stream being flushed!
            final int exitStatus = process.waitFor();

            if (exitStatus != 0) {
                log.warn("exitStatus: {}", exitStatus);

                if (compilationResult.exists()) {
                    log.warn("errorMessage: {}", errorMessage);
                } else {
                    log.error("exitStatus: {}", exitStatus);
                    throw new IOException(errorMessage);
                }
            }
        } finally {
            IOUtils.close(argsOutputStream);
        }
    }

    /**
     * <p>
     * Invokes tsc compiler on top of rhino using trireme.
     * </p>
     *
     * @param workingDir the working directory
     * @param pathsToCompile paths of files to compile
     * @param compilationResult compilation result
     * @param be best effort mode or not
     * @throws IOException if a copy/read IO operation fails
     * @throws NodeException if node compatibility layers fails
     * @throws InterruptedException if command execution is interrupted
     * @throws ExecutionException if command execution fails
     */
    private void rhino(final File workingDir, final List<String> pathsToCompile, final File compilationResult, final Boolean be)
            throws IOException, NodeException, InterruptedException, ExecutionException {
        final StringBuilder sb = new StringBuilder();
        sb.append("var compile=require('typescript-compiler');var logger=require('slf4j-logger');compile([");
        for (final String pathToCompile : pathsToCompile) {
            sb.append("'").append(pathToCompile).append("',");
        }

        sb.replace(sb.length() - 1, sb.length(), "]");
        sb.append(",'");

        if (!be) {
            sb.append("--sourcemap ");
        }

        sb.append("-t ");
        sb.append(ecmaScriptVersion);
        sb.append(" --out ");
        sb.append(compilationResult.getName());
        sb.append("',null,function(e){logger.logWarning(e.messageText);});");

        final File lib = new File(workingDir, "lib");

        if (!lib.mkdirs()) {
            log.debug("{} may already exists", lib.getAbsolutePath());
        }

        final OutputStream libDOs = new FileOutputStream(new File(lib, "lib.d.ts"));
        final InputStream libDIs = IOUtils.class.getResourceAsStream("/wuic/tsc/lib/lib.d.ts");

        try {
            IOUtils.copyStream(libDIs, libDOs);
        } finally {
            IOUtils.close(libDIs, libDOs);
        }

        final NodeScript script = env.createScript("", sb.toString(), null);
        script.setWorkingDirectory(workingDir.getAbsolutePath());
        // Wait for the script to complete
        final ScriptFuture f = script.execute();
        f.get();
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
         * Paths to compile.
         */
        private final List<String> pathsToCompile;

        /**
         * Referenced nuts.
         */
        private final List<ConvertibleNut> refNuts;

        /**
         * <p>
         * Builds a new instance with an expected {@link CompositeNut.CompositeInputStream}.
         * </p>
         *
         * @param in an instance of {@link CompositeNut.CompositeInputStream}
         * @throws IOException if any I/O error occurs
         */
        private InternalInputStreamReader(final InputStream in)
            throws IOException {
            super(in);

            if (in instanceof CompositeNut.CompositeInputStream) {
                cn = CompositeNut.CompositeInputStream.class.cast(in);
            } else {
                throw new IllegalArgumentException("Nut must be an instance of " + CompositeNut.CompositeInputStream.class.getName());
            }

            out = new AtomicReference<OutputStream>();
            refNuts = new ArrayList<ConvertibleNut>();
            pathsToCompile = new ArrayList<String>();

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
         * {@inheritDoc}
         */
        @Override
        public void update(final Observable o, final Object arg) {
            final CompositeNut.CompositeInputStreamReadEvent e = CompositeNut.CompositeInputStreamReadEvent.class.cast(arg);

            try {
                OutputStream os = out.get();

                // Write new nut
                if (os == null) {
                    os = NutDiskStore.INSTANCE.store(e.getNut());
                    out.set(os);
                    os.write(e.getRead());
                    pathsToCompile.add(e.getNut().getName());
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
                WuicException.throwBadStateException(ioe);
            } catch (ExecutionException ee) {
                WuicException.throwBadStateException(ee);
            } catch (InterruptedException ie) {
                WuicException.throwBadStateException(ie);
            }
        }
    }
}