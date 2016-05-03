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


package com.github.wuic.engine.typescript;

import com.github.wuic.NutType;
import com.github.wuic.config.BooleanConfigParam;
import com.github.wuic.config.Config;
import com.github.wuic.config.StringConfigParam;
import com.github.wuic.engine.EngineRequest;
import com.github.wuic.engine.EngineService;
import com.github.wuic.engine.core.AbstractConverterEngine;
import com.github.wuic.engine.core.CommandLineConverterEngine;
import com.github.wuic.exception.WuicException;
import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.util.BiFunction;
import com.github.wuic.util.IOUtils;
import io.apigee.trireme.core.NodeEnvironment;
import io.apigee.trireme.core.NodeException;
import io.apigee.trireme.core.NodeScript;
import io.apigee.trireme.core.ScriptFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static com.github.wuic.ApplicationConfig.ECMA_SCRIPT_VERSION;
import static com.github.wuic.ApplicationConfig.RESOLVED_FILE_DIRECTORY_AS_WORKING_DIR;
import static com.github.wuic.ApplicationConfig.USE_NODE_JS;

/**
 * <p>
 * This class can convert Typescript files using the Typescript4j library.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.1
 */
@EngineService(injectDefaultToWorkflow = true)
public class TypeScriptConverterEngine extends AbstractConverterEngine
        implements BiFunction<CommandLineConverterEngine.CommandLineInfo, EngineRequest, Boolean> {

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
    private String ecmaScriptVersion;

    /**
     * Node environment to run tsc on top of rhino.
     */
    private NodeEnvironment env;

    /**
     * Script to execute in node environment.
     */
    private NodeScript script;

    /**
     * Try to use the directory containing source files as parent directory for generated content.
     */
    private Boolean resolvedFileDirectoryAsWorkingDirectory;

    /**
     * <p>
     * Initializes a new instance.
     * </p>
     *
     * @param esv           the ECMA script version
     * @param useNodeJs     use node.js command line or not
     * @param srdaws        try to reuse source directory to generate files
     * @throws WuicException if the engine cannot be initialized
     */
    @Config
    public void init(@StringConfigParam(propertyKey = ECMA_SCRIPT_VERSION, defaultValue = "ES3") final String esv,
                     @BooleanConfigParam(propertyKey = USE_NODE_JS, defaultValue = false) final Boolean useNodeJs,
                     @BooleanConfigParam(propertyKey = RESOLVED_FILE_DIRECTORY_AS_WORKING_DIR, defaultValue = true) final Boolean srdaws)
            throws WuicException {
        ecmaScriptVersion = esv;
        resolvedFileDirectoryAsWorkingDirectory = srdaws;

        if (!useNodeJs) {
            env = new NodeEnvironment();
            env.setDefaultClassCache();
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
    public InputStream transform(final InputStream is, final ConvertibleNut nut, final EngineRequest request)
            throws IOException {
        return CommandLineConverterEngine.execute(is, nut, request, this, resolvedFileDirectoryAsWorkingDirectory);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean apply(final CommandLineConverterEngine.CommandLineInfo commandLineInfo, final EngineRequest request) {
        final File compilationResult = commandLineInfo.getCompilationResult();
        final List<String> pathsToCompile = commandLineInfo.getPathsToCompile();
        OutputStream argsOutputStream = null;
        Boolean retval = Boolean.TRUE;
        final File workingDir = compilationResult.getParentFile();

        try {
            log.debug("absolute path: {}", workingDir.getAbsolutePath());

            final File argsFile = new File(workingDir, ARGS_FILE);
            argsOutputStream = new FileOutputStream(argsFile);

            for (final String pathToCompile : pathsToCompile) {
                argsOutputStream.write((pathToCompile + " ").getBytes());
            }

            // Do not generate source map in best effort
            if (!request.isBestEffort()) {
                argsOutputStream.write(" --sourcemap".getBytes());
            }

            argsOutputStream.write((" -t " + ecmaScriptVersion + " --out ").getBytes());
            argsOutputStream.write(compilationResult.getAbsolutePath().getBytes());
            IOUtils.close(argsOutputStream);

            if (env == null) {
                retval = node(workingDir, compilationResult);
            } else {
                retval = rhino(workingDir.getAbsolutePath());
            }

            if (!compilationResult.exists()) {
                log.error("{} does not exists, which means that some errors break compilation. Check log above to see them.");
                retval = Boolean.FALSE;
            }
        } catch (IOException ioe) {
            WuicException.throwBadStateException(ioe);
        } catch (InterruptedException ie) {
            WuicException.throwBadStateException(ie);
        } catch (NodeException ne) {
            WuicException.throwBadStateException(ne);
        } catch (ExecutionException ee) {
            WuicException.throwBadStateException(ee);
        } finally {
            IOUtils.close(argsOutputStream);
        }

        return retval;
    }

    /**
     * <p>
     * Runs tsc compiler command ('npm install -g typescript' is required).
     * </p>
     *
     * @param workingDir the working directory
     * @param compilationResult compilation result
     * @throws IOException if a copy/read IO operation fails
     * @throws InterruptedException if command execution is interrupted
     * @return {@code true} if compilation has succeeded, {@code false} otherwise
     */
    private Boolean node(final File workingDir, final File compilationResult)
            throws IOException, InterruptedException {

        // Creates the command line to execute tsc tool
        return CommandLineConverterEngine.process("tsc '@'" + ARGS_FILE, workingDir, compilationResult);
    }

    /**
     * <p>
     * Invokes tsc compiler on top of rhino using trireme.
     * </p>
     *
     * @param workingDirectory the directory where script will be executed
     *
     * @throws IOException if a copy/read IO operation fails
     * @throws NodeException if node compatibility layers fails
     * @throws InterruptedException if command execution is interrupted
     * @throws ExecutionException if command execution fails
     * @return {@code true} if no exception is thrown
     */
    private Boolean rhino(final String workingDirectory)
            throws IOException, NodeException, InterruptedException, ExecutionException {
        if (script == null || !workingDirectory.equals(script.getWorkingDirectory())) {

            final StringBuilder sb = new StringBuilder();
            sb.append("var compile=require('typescript-compiler');var logger=require('slf4j-logger');compile([],'@");
            sb.append(ARGS_FILE);
            sb.append("',");

            final URL resource = IOUtils.class.getResource("/wuic/tsc/lib/lib.d.ts");

            // Copy the file to the working directory if not stored on the file system, otherwise give the absolute path
            if (resource.toString().startsWith("file:")) {
                sb.append("{ defaultLibFilename: '");
                sb.append(new File(resource.getFile()).getAbsolutePath().replace('\\', '/'));
                sb.append("'}");
            } else {
                sb.append("null");

                final File lib = new File(workingDirectory, "lib");

                if (!lib.mkdirs()) {
                    log.debug("{} may already exists", lib.getAbsolutePath());
                }

                final OutputStream libDOs = new FileOutputStream(new File(lib, "lib.d.ts"));
                final InputStream libDIs = resource.openStream();

                try {
                    IOUtils.copyStream(libDIs, libDOs);
                } finally {
                    IOUtils.close(libDIs, libDOs);
                }
            }

            sb.append(",function(e){logger.logWarning(e.messageText);});");

            script = env.createScript("", sb.toString(), null);
            script.setWorkingDirectory(workingDirectory);
        }

        // Wait for the script to complete
        final ScriptFuture f = script.execute();
        f.get();
        return Boolean.TRUE;
    }
}
