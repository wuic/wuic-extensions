/*
 * "Copyright (c) 2014   Capgemini Technology Services (hereinafter "Capgemini")
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


package com.github.wuic.thymeleaf;

import com.github.wuic.WuicFacade;
import com.github.wuic.exception.WuicException;
import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.util.HtmlUtil;
import com.github.wuic.util.IOUtils;
import com.github.wuic.util.UrlProvider;
import com.github.wuic.util.UrlProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.Arguments;
import org.thymeleaf.dom.Macro;
import org.thymeleaf.dom.Element;
import org.thymeleaf.processor.ProcessorResult;
import org.thymeleaf.processor.attr.AbstractAttrProcessor;

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
 * @version 1.1
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
        final String workflow = element.getAttributeValue(attributeName);

        if (wuicFacade.allowsMultipleConfigInTagSupport()) {
            wuicFacade.clearTag(workflow);
        }

        final UrlProvider urlProvider = urlProviderFactory.create(IOUtils.mergePath(wuicFacade.getContextPath(), workflow));

        try {
            int cpt = 0;
            final List<ConvertibleNut> nuts = wuicFacade.runWorkflow(workflow);

            // Insert import statements into the top
            for (final ConvertibleNut nut : nuts) {
                element.insertChild(cpt++, new Macro(HtmlUtil.writeScriptImport(nut, urlProvider)));
            }
        } catch (WuicException we) {
            log.error("WUIC import processor has failed", we);
        } catch (IOException ioe) {
            log.error("WUIC import processor has failed", ioe);
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
