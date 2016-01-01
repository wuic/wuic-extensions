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


package com.github.wuic.nodejs.test;

import com.github.wuic.ApplicationConfig;
import com.github.wuic.config.BooleanConfigParam;
import com.github.wuic.config.ConfigConstructor;
import com.github.wuic.config.StringConfigParam;
import com.github.wuic.engine.EngineService;
import com.github.wuic.engine.core.CommandLineConverterEngine;
import com.github.wuic.exception.WuicException;

import java.io.File;
import java.io.IOException;

/**
 * {@link CommandLineConverterEngine} with specific working directory.
 */
@EngineService(injectDefaultToWorkflow = true)
public class BazCommandLineConverterEngine extends CommandLineConverterEngine {

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param command the command line
     * @param inputNutType the input nut type
     * @param outputNutType the output nut type
     * @param separator the path separator
     * @param convert if this engine is enabled or not
     * @param asynchronous computes version number asynchronously or not
     * @param libs additional libraries paths available in the classpath
     * @throws WuicException if the engine cannot be initialized
     * @throws IOException if any I/O error occurs
     */
    @ConfigConstructor
    public BazCommandLineConverterEngine(@BooleanConfigParam(propertyKey = ApplicationConfig.CONVERT, defaultValue = true) final Boolean convert,
                                         @BooleanConfigParam(propertyKey = ApplicationConfig.COMPUTE_VERSION_ASYNCHRONOUSLY, defaultValue = true) final Boolean asynchronous,
                                         @StringConfigParam(propertyKey = ApplicationConfig.COMMAND, defaultValue = "") final String command,
                                         @StringConfigParam(propertyKey = ApplicationConfig.INPUT_NUT_TYPE, defaultValue = "") final String inputNutType,
                                         @StringConfigParam(propertyKey = ApplicationConfig.OUTPUT_NUT_TYPE, defaultValue = "") final String outputNutType,
                                         @StringConfigParam(propertyKey = ApplicationConfig.PATH_SEPARATOR, defaultValue = " ") final String separator,
                                         @StringConfigParam(propertyKey = ApplicationConfig.LIBRARIES, defaultValue = "") final String libs) throws WuicException, IOException {
        super(convert, asynchronous, command, inputNutType, outputNutType, separator, libs);
        setWorkingDirectory(new File(getWorkingDirectory(), getClass().getSimpleName()));
    }
}
