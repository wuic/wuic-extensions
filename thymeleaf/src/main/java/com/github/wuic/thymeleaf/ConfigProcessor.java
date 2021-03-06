
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


package com.github.wuic.thymeleaf;

import com.github.wuic.ProcessContext;
import com.github.wuic.WuicFacade;
import com.github.wuic.exception.WuicException;
import com.github.wuic.servlet.HttpUtil;
import com.github.wuic.servlet.ServletProcessContext;
import com.github.wuic.config.bean.xml.ReaderXmlContextBuilderConfigurator;
import org.thymeleaf.Arguments;
import org.thymeleaf.context.IWebContext;
import org.thymeleaf.dom.Element;
import org.thymeleaf.processor.element.AbstractRemovalElementProcessor;
import org.thymeleaf.util.DOMUtils;

import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.JAXBException;
import java.io.Reader;
import java.io.StringReader;

/**
 * <p>
 * This processor evaluates the XML configuration described in the first element child and injects it to the global
 * configuration.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.4.2
 */
public class ConfigProcessor extends AbstractRemovalElementProcessor {

    /**
     * The WUIC facade.
     */
    private WuicFacade wuicFacade;

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param wf the WUIC facade
     */
    public ConfigProcessor(final WuicFacade wf) {
        super("config");
        wuicFacade = wf;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean getRemoveElementAndChildren(final Arguments arguments, final Element element) {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean removeHostElementIfChildNotRemoved(final Arguments arguments, final Element element) {
        try {
            // Let's load the wuic.xml file and configure the builder with it
            final Reader reader = new StringReader(DOMUtils.getHtml5For(element.getFirstElementChild()));
            final HttpServletRequest req = IWebContext.class.cast(arguments.getContext()).getHttpServletRequest();
            final ProcessContext pc = new ServletProcessContext(req);
            wuicFacade.configure(new ReaderXmlContextBuilderConfigurator.Simple(
                    reader,
                    HttpUtil.INSTANCE.computeUniqueTag(req),
                    wuicFacade.allowsMultipleConfigInTagSupport(),
                    pc));
        } catch (WuicException we) {
            WuicException.throwBadStateException(we);
        } catch (JAXBException se) {
            WuicException.throwBadArgumentException(new IllegalArgumentException(
                    "First DOM element child is not a valid XML to describe WUIC configuration", se));
        }

        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getPrecedence() {
        return 0;
    }
}
