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


package com.github.wuic.nodejs.test;

import com.github.wuic.ApplicationConfig;
import com.github.wuic.config.BooleanConfigParam;
import com.github.wuic.config.Config;
import com.github.wuic.config.StringConfigParam;
import com.github.wuic.engine.EngineService;
import com.github.wuic.engine.core.CommandLineConverterEngine;
import com.github.wuic.exception.WuicException;

import java.io.IOException;

import static com.github.wuic.ApplicationConfig.RESOLVED_FILE_DIRECTORY_AS_WORKING_DIR;

/**
 * {@link CommandLineConverterEngine} with specific working directory.
 */
@EngineService(injectDefaultToWorkflow = true)
public class BarCommandLineConverterEngine extends CommandLineConverterEngine {

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param command the command line
     * @param inputNutType the input nut type
     * @param outputNutType the output nut type
     * @param separator the path separator
     * @param libs additional libraries paths available in the classpath
     * @param srdaws try to reuse source directory to generate files
     * @throws WuicException if the engine cannot be initialized
     * @throws IOException if any I/O error occurs
     */
    @Config
    public void init(@StringConfigParam(propertyKey = ApplicationConfig.COMMAND, defaultValue = "") final String command,
                     @StringConfigParam(propertyKey = ApplicationConfig.INPUT_NUT_TYPE, defaultValue = "") final String inputNutType,
                     @StringConfigParam(propertyKey = ApplicationConfig.OUTPUT_NUT_TYPE, defaultValue = "") final String outputNutType,
                     @StringConfigParam(propertyKey = ApplicationConfig.PATH_SEPARATOR, defaultValue = " ") final String separator,
                     @StringConfigParam(propertyKey = ApplicationConfig.LIBRARIES, defaultValue = "") final String libs,
                     @BooleanConfigParam(propertyKey = RESOLVED_FILE_DIRECTORY_AS_WORKING_DIR, defaultValue = true) final Boolean srdaws)
            throws WuicException, IOException {
        super.init(command, inputNutType, outputNutType, separator, libs, srdaws);
    }
}
