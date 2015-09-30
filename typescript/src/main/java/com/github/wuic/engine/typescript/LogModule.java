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

import io.apigee.trireme.core.NodeModule;
import io.apigee.trireme.core.NodeRuntime;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.mozilla.javascript.annotations.JSFunction;

import java.lang.reflect.InvocationTargetException;

/**
 * <p>
 * A module which exposes SLF4J to log messages inside NodeJS execution. This class is mainly inspired from
 * github.com/apigee/trireme/blob/master/samples/java-hello/src/main/java/io/apigee/trireme/samples/hello/HelloModule.java.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.1
 */
public class LogModule implements NodeModule {

    /**
     * The reused {@link LogModuleImpl} object.
     */
    private Scriptable s;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getModuleName() {
        return "slf4j-logger";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Scriptable registerExports(final Context cx, final Scriptable global, final NodeRuntime runtime)
            throws InvocationTargetException, IllegalAccessException, InstantiationException {
        if (s == null) {
            ScriptableObject.defineClass(global, LogModuleImpl.class);
            s = cx.newObject(global, LogModuleImpl.LOG.getName());
        }

        return s;
    }

    /**
     * <p>
     * Log module implementation.
     * </p>
     *
     * @author Guillaume DROUET
     * @since 0.5.1
     */
    public static class LogModuleImpl extends ScriptableObject {

        /**
         * The logger.
         */
        private static final Logger LOG = LoggerFactory.getLogger(LogModuleImpl.class);

        /**
         * {@inheritDoc}
         */
        @Override
        public String getClassName() {
            return LOG.getName();
        }

        /**
         * <p>
         * Logs a warning message with {@link #LOG} object.
         * </p>
         *
         * @param cx the context
         * @param thisObj the contextualized 'this'
         * @param args function arguments
         * @param func the function itself
         */
        @JSFunction
        @SuppressWarnings("unused")
        public static void logWarning(final Context cx, final Scriptable thisObj, final Object[] args, final Function func) {
            LOG.warn(String.valueOf(args[0]));
        }
    }
}
