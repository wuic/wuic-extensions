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
import com.github.wuic.servlet.HtmlParserFilter;
import com.github.wuic.servlet.ServletProcessContext;
import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.util.HtmlUtil;
import com.github.wuic.util.IOUtils;
import com.github.wuic.util.UrlProvider;
import com.github.wuic.util.UrlProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.Arguments;
import org.thymeleaf.context.IWebContext;
import org.thymeleaf.dom.Macro;
import org.thymeleaf.dom.Element;
import org.thymeleaf.processor.ProcessorResult;
import org.thymeleaf.processor.attr.AbstractAttrProcessor;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;

/**
 * <p>
 * Thymeleaf support for WUIC. This class put into the DOM the HTML statements that import nuts. The suffix processor is
 * "import".
 * </p>
 *
 * <p>
 * Usage : <pre><html wuic-import="my-workflow|my-other-workflow"></html></pre>
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.4.1
 */
public class ImportProcessor extends AbstractAttrProcessor {

    /**
     * The logger.
     */
    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * The WUIC facade.
     */
    private final WuicFacade wuicFacade;

    /**
     * The URL provider;
     */
    private final UrlProviderFactory urlProviderFactory;

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param wf the WUIC facade
     * @param up the URL provider
     */
    public ImportProcessor(final UrlProviderFactory up, final WuicFacade wf) {
        super("import");
        wuicFacade = wf;
        urlProviderFactory = up;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ProcessorResult processAttribute(final Arguments arguments, final Element element, final String attributeName) {
        final HttpServletRequest request = IWebContext.class.cast(arguments.getContext()).getHttpServletRequest();

        request.setAttribute(HtmlParserFilter.FORCE_DYNAMIC_CONTENT, "");

        final String workflow = element.getAttributeValue(attributeName);
        final String breakAggregation = element.getAttributeValue("data-wuic-break");

        if (wuicFacade.allowsMultipleConfigInTagSupport()) {
            wuicFacade.clearTag(workflow);
        }

        if (request.getAttribute(HtmlParserFilter.class.getName()) == null) {
            log.warn("data-wuic-break attribute has bean specified for the import of workflow {} but will be ignored because the page is not filtered by",
                    workflow, HtmlParserFilter.class.getName());

            final UrlProvider urlProvider = urlProviderFactory.create(IOUtils.mergePath(wuicFacade.getContextPath(), workflow));

            try {
                int cpt = 0;
                final ProcessContext pc = arguments.getContext() instanceof IWebContext ?
                        new ServletProcessContext(request) : null;
                final List<ConvertibleNut> nuts = wuicFacade.runWorkflow(workflow, urlProviderFactory, pc);

                // Insert import statements into the top
                for (final ConvertibleNut nut : nuts) {
                    element.insertChild(cpt++, new Macro(HtmlUtil.writeScriptImport(nut, urlProvider)));
                }
            } catch (WuicException we) {
                log.error("WUIC import processor has failed", we);
            } catch (IOException ioe) {
                log.error("WUIC import processor has failed", ioe);
            }
        } else {
            element.insertChild(0, new Macro("<wuic:html-import workflowId='"
                    + workflow
                    + "'"
                    + (breakAggregation == null ? " " : " data-wuic-break ")
                    + "/>"));
        }

        // Not lenient
        element.removeAttribute(attributeName);

        return ProcessorResult.ok();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getPrecedence() {
        return 0;
    }
}
