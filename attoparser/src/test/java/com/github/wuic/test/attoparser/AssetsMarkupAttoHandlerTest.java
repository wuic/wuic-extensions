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


package com.github.wuic.test.attoparser;

import com.github.wuic.engine.attoparser.AssetsMarkupAttoParser;
import com.github.wuic.engine.core.AssetsMarkupHandler;
import com.github.wuic.util.StringUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.StringReader;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>
 * Tests the {@link com.github.wuic.engine.attoparser.AssetsMarkupAttoParser} notification capabilities.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.3
 */
@RunWith(JUnit4.class)
public class AssetsMarkupAttoHandlerTest {

    /**
     * Timeout.
     */
    @Rule
    public Timeout globalTimeout = Timeout.seconds(60);

    /**
     * <p>
     * Handler factory.
     * </p>
     *
     * @author Guillaume DROUET
     * @since 0.5.3
     */
    interface AdapterFactory {

        /**
         * <p>
         * Builds a new instance.
         * </p>
         *
         * @param lines the matrix
         * @param linkBuilder the link
         * @param attributesMap the attributes
         * @param statementBuilder the captured statement
         */
        Adapter create(String[] lines, StringBuilder linkBuilder, Map<String, String> attributesMap, StringBuilder statementBuilder);
    }

    /**
     * <p>
     * Default handler.
     * </p>
     *
     * @author Guillaume DROUET
     * @since 0.5.3
     */
    private static class Adapter implements AssetsMarkupHandler {

        /**
         * Lines.
         */
        protected final String[] lines;

        /**
         * Link.
         */
        protected final StringBuilder linkBuilder;

        /**
         * Attributes.
         */
        protected final Map<String, String> attributesMap;

        /**
         * Captured statement.
         */
        protected final StringBuilder statementBuilder;

        /**
         * <p>
         * Builds a new instance.
         * </p>
         *
         * @param lines the matrix
         * @param linkBuilder the link
         * @param attributesMap the attributes
         * @param statementBuilder the captured statement
         */
        public Adapter(final String[] lines,
                       final StringBuilder linkBuilder,
                       final Map<String, String> attributesMap,
                       final StringBuilder statementBuilder) {
            this.lines = lines;
            this.linkBuilder = linkBuilder;
            this.attributesMap = attributesMap;
            this.statementBuilder = statementBuilder;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void handleComment(final char[] content, final int startLine, final int startColumn, final int length) {
            Assert.fail("handleComment() not supposed to be called");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void handleJavascriptContent(final char[] content,
                                            final Map<String, String> attributes,
                                            final int startLine,
                                            final int startColumn,
                                            final int endLine,
                                            final int endColumn) {
            Assert.fail("handleJavascriptContent() not supposed to be called");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void handleScriptLink(final String link,
                                     final Map<String, String> attributes,
                                     final int startLine,
                                     final int startColumn,
                                     final int endLine,
                                     final int endColumn) {
            Assert.fail("handleScriptLink() not supposed to be called");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void handleLink(final String link,
                               final Map<String, String> attributes,
                               final int startLine,
                               final int startColumn,
                               final int endLine,
                               final int endColumn) {
            Assert.fail("handleLink() not supposed to be called");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void handleCssContent(final char[] content,
                                     final Map<String, String> attributes,
                                     final int startLine,
                                     final int startColumn,
                                     final int endLine,
                                     final int endColumn) {
            Assert.fail("handleCssContent() not supposed to be called");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void handleImgLink(final String link,
                                  final Map<String, String> attributes,
                                  final int startLine,
                                  final int startColumn,
                                  final int endLine,
                                  final int endColumn) {
            Assert.fail("handleImgLink() not supposed to be called");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void handleImport(final String workflowId,
                                 final Map<String, String> attributes,
                                 final int startLine,
                                 final int startColumn,
                                 final int endLine,
                                 final int endColumn) {
            Assert.fail("handleImport() not supposed to be called");
        }
    }

    /**
     * <p>
     * Javascript link resource handler.
     * </p>
     *
     * @author Guillaume DROUET
     * @since 0.5.3
     */
    public static class JsHandling extends Adapter {

        /**
         * <p>
         * Builds a new instance.
         * </p>
         *
         * @param lines the matrix
         * @param linkBuilder the link
         * @param attributesMap the attributes
         * @param statementBuilder the captured statement
         */
        public JsHandling(final String[] lines,
                          final StringBuilder linkBuilder,
                          final Map<String, String> attributesMap,
                          final StringBuilder statementBuilder) {
            super(lines, linkBuilder, attributesMap, statementBuilder);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void handleJavascriptContent(final char[] content,
                                            final Map<String, String> attributes,
                                            final int startLine,
                                            final int startColumn,
                                            final int endLine,
                                            final int endColumn) {
            linkBuilder.append(content);
            attributesMap.putAll(attributes);
            statementBuilder.append(StringUtils.substringMatrix(lines, startLine, startColumn, endLine, endColumn));
        }
    }

    /**
     * <p>
     * Javascript resource handler.
     * </p>
     *
     * @author Guillaume DROUET
     * @since 0.5.3
     */
    public static class JsSrcHandling extends Adapter {

        /**
         * <p>
         * Builds a new instance.
         * </p>
         *
         * @param lines the matrix
         * @param linkBuilder the link
         * @param attributesMap the attributes
         * @param statementBuilder the captured statement
         */
        public JsSrcHandling(final String[] lines,
                             final StringBuilder linkBuilder,
                             final Map<String, String> attributesMap,
                             final StringBuilder statementBuilder) {
            super(lines, linkBuilder, attributesMap, statementBuilder);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void handleScriptLink(final String link,
                                     final Map<String, String> attributes,
                                     final int startLine,
                                     final int startColumn,
                                     final int endLine,
                                     final int endColumn) {
            linkBuilder.append(link);
            attributesMap.putAll(attributes);
            statementBuilder.append(StringUtils.substringMatrix(lines, startLine, startColumn, endLine, endColumn));
        }
    }

    /**
     * <p>
     * CSS link resource handler.
     * </p>
     *
     * @author Guillaume DROUET
     * @since 0.5.3
     */
    public static class CssHandling extends Adapter {

        /**
         * <p>
         * Builds a new instance.
         * </p>
         *
         * @param lines the matrix
         * @param linkBuilder the link
         * @param attributesMap the attributes
         * @param statementBuilder the captured statement
         */
        public CssHandling(final String[] lines,
                           final StringBuilder linkBuilder,
                           final Map<String, String> attributesMap,
                           final StringBuilder statementBuilder) {
            super(lines, linkBuilder, attributesMap, statementBuilder);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void handleCssContent(final char[] content,
                                     final Map<String, String> attributes,
                                     final int startLine,
                                     final int startColumn,
                                     final int endLine,
                                     final int endColumn) {
            linkBuilder.append(content);
            attributesMap.putAll(attributes);
            statementBuilder.append(StringUtils.substringMatrix(lines, startLine, startColumn, endLine, endColumn));
        }
    }

    /**
     * <p>
     * CSS resource handler.
     * </p>
     *
     * @author Guillaume DROUET
     * @since 0.5.3
     */
    public static class CssSrcHandling extends Adapter {

        /**
         * <p>
         * Builds a new instance.
         * </p>
         *
         * @param lines the matrix
         * @param linkBuilder the link
         * @param attributesMap the attributes
         * @param statementBuilder the captured statement
         */
        public CssSrcHandling(final String[] lines,
                              final StringBuilder linkBuilder,
                              final Map<String, String> attributesMap,
                              final StringBuilder statementBuilder) {
            super(lines, linkBuilder, attributesMap, statementBuilder);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void handleLink(final String link,
                               final Map<String, String> attributes,
                               final int startLine,
                               final int startColumn,
                               final int endLine,
                               final int endColumn) {
            linkBuilder.append(link);
            attributesMap.putAll(attributes);
            statementBuilder.append(StringUtils.substringMatrix(lines, startLine, startColumn, endLine, endColumn));
        }
    }

    /**
     * <p>
     * Image resource handler.
     * </p>
     *
     * @author Guillaume DROUET
     * @since 0.5.3
     */
    public static class ImageHandling extends Adapter {

        /**
         * <p>
         * Builds a new instance.
         * </p>
         *
         * @param lines the matrix
         * @param linkBuilder the link
         * @param attributesMap the attributes
         * @param statementBuilder the captured statement
         */
        public ImageHandling(final String[] lines,
                             final StringBuilder linkBuilder,
                             final Map<String, String> attributesMap,
                             final StringBuilder statementBuilder) {
            super(lines, linkBuilder, attributesMap, statementBuilder);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void handleImgLink(final String link,
                                  final Map<String, String> attributes,
                                  final int startLine,
                                  final int startColumn,
                                  final int endLine,
                                  final int endColumn) {
            linkBuilder.append(link);
            attributesMap.putAll(attributes);
            statementBuilder.append(StringUtils.substringMatrix(lines, startLine, startColumn, endLine, endColumn));
        }
    }

    /**
     * <p>
     * Comment handler.
     * </p>
     *
     * @author Guillaume DROUET
     * @since 0.5.3
     */
    public static class CommentHandling extends Adapter {

        /**
         * <p>
         * Builds a new instance.
         * </p>
         *
         * @param lines the matrix
         * @param linkBuilder the link
         * @param attributesMap the attributes
         * @param statementBuilder the captured statement
         */
        public CommentHandling(final String[] lines,
                             final StringBuilder linkBuilder,
                             final Map<String, String> attributesMap,
                             final StringBuilder statementBuilder) {
            super(lines, linkBuilder, attributesMap, statementBuilder);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void handleComment(final char[] content,
                                  final int startLine,
                                  final int startColumn,
                                  final int length) {
            final AtomicInteger endLine = new AtomicInteger();
            final AtomicInteger endColumn = new AtomicInteger();
            StringUtils.reachEndLineAndColumn(lines, startLine, startColumn, length, endLine, endColumn);
            statementBuilder.append(StringUtils.substringMatrix(lines, startLine, startColumn, endLine.get(), endColumn.get()));
        }
    }

    /**
     * <p>
     * Import resource handler.
     * </p>
     *
     * @author Guillaume DROUET
     * @since 0.5.3
     */
    public static class ImportHandling extends Adapter {

        /**
         * <p>
         * Builds a new instance.
         * </p>
         *
         * @param lines the matrix
         * @param linkBuilder the link
         * @param attributesMap the attributes
         * @param statementBuilder the captured statement
         */
        public ImportHandling(final String[] lines,
                             final StringBuilder linkBuilder,
                             final Map<String, String> attributesMap,
                             final StringBuilder statementBuilder) {
            super(lines, linkBuilder, attributesMap, statementBuilder);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void handleImport(final String wId,
                                 final Map<String, String> attributes,
                                 final int startLine,
                                 final int startColumn,
                                 final int endLine,
                                 final int endColumn) {
            linkBuilder.append(wId);
            attributesMap.putAll(attributes);
            statementBuilder.append(StringUtils.substringMatrix(lines, startLine, startColumn, endLine, endColumn));
        }
    }

    /**
     * <p>
     * Tests inline import.
     * </p>
     */
    @Test
    public void testInlineImport() {
        testImport(">");
    }

    /**
     * <p>
     * Tests standalone import.
     * </p>
     */
    @Test
    public void testStandaloneImport() {
        testImport("/>");
    }

    /**
     * <p>
     * Tests standard HTML import.
     * </p>
     */
    @Test
    public void testImport() {
        testImport("></wuic:html-import>");
    }

    /**
     * <p>
     * Tests auto close.
     * </p>
     */
    @Test
    public void testBalancedImport() {
        testImport("><script>");
    }

    /**
     * <p>
     * Runs several assertions for an import closed in a specified way.
     * </p>
     *
     * @param closeTag the closing string representation of the tag
     */
    public void testImport(final String closeTag) {
        boolean ignore = false;

        final AdapterFactory factory = new AdapterFactory() {

            /**
             * {@inheritDoc}
             */
            @Override
            public Adapter create(final String[] lines,
                                  final StringBuilder linkBuilder,
                                  final Map<String, String> attributesMap,
                                  final StringBuilder statementBuilder) {
                return new ImportHandling(lines, linkBuilder, attributesMap, statementBuilder);
            }
        };

        for (int i = 0; i <= 1; i++) {
            final Map<String, String> attributes = new LinkedHashMap<String, String>();
            assertHandling("<wuic:html-import " + (ignore ? "" : "workflowId=foo") + closeTag, "foo", attributes, ignore, factory);
            assertHandling("<wuic:html-import " + (ignore ? "" : "workflowId='foo'") + closeTag, "foo", attributes, ignore, factory);
            assertHandling("<wuic:html-import " + (ignore ? "" : "workflowId=\"foo\"") + closeTag, "foo", attributes, ignore, factory);
            assertHandling("<wuic:html-import    " + (ignore ? "" : "workflowId=foo   ") + closeTag, "foo", attributes, ignore, factory);
            assertHandling("<wuic:html-import    " + (ignore ? "" : "workflowId=  'foo'") + closeTag, "foo", attributes, ignore, factory);
            assertHandling("<wuic:html-import " + (ignore ? "" : "workflowId=\"foo\"   ") + closeTag, "foo", attributes, ignore, factory);

            attributes.put("data-wuic-skip", "true");
            assertHandling("<wuic:html-import " + (ignore ? "" : "workflowId=foo") + "   data-wuic-skip= 'true'  " + closeTag, "foo", attributes, ignore, factory);
            assertHandling("<wuic:html-import  data-wuic-skip= 'true' " + (ignore ? "" : "workflowId='foo'") + closeTag, "foo", attributes, ignore, factory);
            assertHandling("<wuic:html-import  data-wuic-skip= 'true'  " + (ignore ? "" : "workflowId=\"foo\"") + "  " + closeTag, "foo", attributes, ignore, factory);
            assertHandling("<wuic:html-import   data-wuic-skip= 'true' " + (ignore ? "" : "workflowId=foo") + "   " + closeTag, "foo", attributes, ignore, factory);
            assertHandling("<wuic:html-import  data-wuic-skip= 'true'    " + (ignore ? "" : "workflowId=  'foo'") + closeTag, "foo", attributes, ignore, factory);
            assertHandling("<wuic:html-import data-wuic-skip= 'true'   " + (ignore ? "" : "workflowId=\"foo\"") + closeTag, "foo", attributes, ignore, factory);
            ignore = !ignore;
        }
    }

    /**
     * <p>
     * Runs several assertions for a comment.
     * </p>
     */
    @Test
    public void testComment() {
        final AdapterFactory factory = new AdapterFactory() {

            /**
             * {@inheritDoc}
             */
            @Override
            public Adapter create(final String[] lines,
                                  final StringBuilder linkBuilder,
                                  final Map<String, String> attributesMap,
                                  final StringBuilder statementBuilder) {
                return new CommentHandling(lines, linkBuilder, attributesMap, statementBuilder);
            }
        };

        assertComment("<!-- -->", "<!-- -->", factory);
        assertComment("<!-- > -->", "<!-- > -->", factory);
        assertComment("<!-- <!--- -->", "<!-- <!--- -->", factory);
        assertComment("<foo><!-- --><bar>", "<!-- -->", factory);
        assertComment("<foo><!-- -->", "<!-- -->", factory);
        assertComment("<!-- --><bar>", "<!-- -->", factory);

        assertComment("<!-- \n -->", "<!-- \n -->", factory);
        assertComment("\n<!-- > \n-->", "<!-- > \n-->", factory);
        assertComment("<!-- <!--- -->\n", "<!-- <!--- -->", factory);
        assertComment("<foo>\n<!-- -->\n<bar>", "<!-- -->", factory);
        assertComment("<foo>\n<!--\n -->", "<!--\n -->", factory);
        assertComment("<!-- \n-->\n<bar>", "<!-- \n-->", factory);
    }

    /**
     * <p>
     * Runs assertions for comments.
     * </p>
     *
     * @param html the HTML to parse
     * @param factory adapter factory
     */
    public void assertComment(final String html,
                              final String comment,
                              final AdapterFactory factory) {
        final StringBuilder statementBuilder = new StringBuilder();
        final String[] lines = html.split("\n");
        final Adapter handler = factory.create(lines, new StringBuilder(), new HashMap<String, String>(), statementBuilder);

        new AssetsMarkupAttoParser().parse(new StringReader(html), handler);
        Assert.assertEquals(html, comment, statementBuilder.toString());
    }

    /**
     * <p>
     * Tests inline image.
     * </p>
     */
    @Test
    public void testInlineImg() {
        testImg(">");
    }

    /**
     * <p>
     * Tests standalone image.
     * </p>
     */
    @Test
    public void testStandaloneImg() {
        testImg("/>");
    }

    /**
     * <p>
     * Tests standard HTML image.
     * </p>
     */
    @Test
    public void testImg() {
        testImg("></img>");
    }

    /**
     * <p>
     * Tests auto close.
     * </p>
     */
    @Test
    public void testBalancedImg() {
        testImg("><script>");
    }

    /**
     * <p>
     * Runs several assertions for an image closed in a specified way.
     * </p>
     *
     * @param closeTag the closing string representation of the tag
     */
    public void testImg(final String closeTag) {
        boolean ignore = false;

        final AdapterFactory factory = new AdapterFactory() {

            /**
             * {@inheritDoc}
             */
            @Override
            public Adapter create(final String[] lines,
                                  final StringBuilder linkBuilder,
                                  final Map<String, String> attributesMap,
                                  final StringBuilder statementBuilder) {
                return new ImageHandling(lines, linkBuilder, attributesMap, statementBuilder);
            }
        };

        for (int i = 0; i <= 1; i++) {
            final Map<String, String> attributes = new LinkedHashMap<String, String>();
            assertHandling("<img " + (ignore ? "" : "src=foo.png") + closeTag, "foo.png", attributes, ignore, factory);
            assertHandling("<img " + (ignore ? "" : "src='foo.png'") + closeTag, "foo.png", attributes, ignore, factory);
            assertHandling("<img " + (ignore ? "" : "src=\"foo.png\"") + closeTag, "foo.png", attributes, ignore, factory);
            assertHandling("<img    " + (ignore ? "" : "src=foo.png   ") + closeTag, "foo.png", attributes, ignore, factory);
            assertHandling("<img    " + (ignore ? "" : "src=  'foo.png'") + closeTag, "foo.png", attributes, ignore, factory);
            assertHandling("<img " + (ignore ? "" : "src=\"foo.png\"   ") + closeTag, "foo.png", attributes, ignore, factory);

            attributes.put("width", "10px");
            attributes.put("height", "15px");
            assertHandling("<img " + (ignore ? "" : "src=foo.png") + "   width= '10px'  height = 15px" + closeTag, "foo.png", attributes, ignore, factory);
            assertHandling("<img  width= '10px'  height =\"15px\" " + (ignore ? "" : "src='foo.png'") + closeTag, "foo.png", attributes, ignore, factory);
            assertHandling("<img  width= '10px'  " + (ignore ? "" : "src=\"foo.png\"") + "  height = 15px" + closeTag, "foo.png", attributes, ignore, factory);
            assertHandling("<img   width= '10px' " + (ignore ? "" : "src=foo.png") + " height = 15px  " + closeTag, "foo.png", attributes, ignore, factory);
            assertHandling("<img  width= '10px'  height = 15px  " + (ignore ? "" : "src=  'foo.png'") + closeTag, "foo.png", attributes, ignore, factory);
            assertHandling("<img width= '10px'  height = 15px " + (ignore ? "" : "src=\"foo.png\"") + closeTag, "foo.png", attributes, ignore, factory);
            ignore = !ignore;
        }
    }

    /**
     * <p>
     * Tests inline CSS.
     * </p>
     */
    @Test
    public void testSrcInlineCss() {
        testSrcCss(">");
    }

    /**
     * <p>
     * Tests standalone CSS.
     * </p>
     */
    @Test
    public void testSrcStandaloneCss() {
        testSrcCss("/>");
    }

    /**
     * <p>
     * Tests standard HTML CSS.
     * </p>
     */
    @Test
    public void testSrcCss() {
        testSrcCss("></link>");
    }

    /**
     * <p>
     * Tests auto close.
     * </p>
     */
    @Test
    public void testSrcBalancedCss() {
        testSrcCss("><style>");
    }

    /**
     * <p>
     * Runs several assertions for a CSS closed in a specified way.
     * </p>
     *
     * @param closeTag the closing string representation of the tag
     */
    public void testSrcCss(final String closeTag) {
        boolean ignore = false;

        final AdapterFactory factory = new AdapterFactory() {

            /**
             * {@inheritDoc}
             */
            @Override
            public Adapter create(final String[] lines,
                                  final StringBuilder linkBuilder,
                                  final Map<String, String> attributesMap,
                                  final StringBuilder statementBuilder) {
                return new CssSrcHandling(lines, linkBuilder, attributesMap, statementBuilder);
            }
        };

        for (int i = 0; i <= 1; i++) {
            final Map<String, String> attributes = new LinkedHashMap<String, String>();

            assertHandling("<link " + (ignore ? "" : "href=foo.css") + closeTag, "foo.css", attributes, ignore, factory);
            assertHandling("<link " + (ignore ? "" : "href='foo.css'") + closeTag, "foo.css", attributes, ignore, factory);
            assertHandling("<link " + (ignore ? "" : "href=\"foo.css\"") + closeTag, "foo.css", attributes, ignore, factory);
            assertHandling("<link    " + (ignore ? "" : "href=foo.css   ") + closeTag, "foo.css", attributes, ignore, factory);
            assertHandling("<link    " + (ignore ? "" : "href=  'foo.css'") + closeTag, "foo.css", attributes, ignore, factory);
            assertHandling("<link " + (ignore ? "" : "href=\"foo.css\"   ") + closeTag, "foo.css", attributes, ignore, factory);

            attributes.put("title", "CSS");
            attributes.put("rel", "stylesheet");
            assertHandling("<link " + (ignore ? "" : "href=foo.css") + "   title= 'CSS'  rel = stylesheet" + closeTag, "foo.css", attributes, ignore, factory);
            assertHandling("<link  title= 'CSS'  rel =\"stylesheet\" " + (ignore ? "" : "href='foo.css'") + closeTag, "foo.css", attributes, ignore, factory);
            assertHandling("<link  title= 'CSS'  " + (ignore ? "" : "href=\"foo.css\"") + "  rel = stylesheet" + closeTag, "foo.css", attributes, ignore, factory);
            assertHandling("<link   title= 'CSS' " + (ignore ? "" : "href=foo.css") + " rel = stylesheet  " + closeTag, "foo.css", attributes, ignore, factory);
            assertHandling("<link  title= 'CSS'  rel = stylesheet  " + (ignore ? "" : "href=  'foo.css'") + closeTag, "foo.css", attributes, ignore, factory);
            assertHandling("<link title= 'CSS'  rel = stylesheet " + (ignore ? "" : "href=\"foo.css\"") +  closeTag, "foo.css", attributes, ignore, factory);
            ignore = !ignore;
        }
    }

    /**
     * <p>
     * Tests inline CSS.
     * </p>
     */
    @Test
    public void testInlineCss() {
        testCss(">", "");
    }

    /**
     * <p>
     * Tests standalone JS.
     * </p>
     */
    @Test
    public void testStandaloneCss() {
        testCss("/>", "");
    }

    /**
     * <p>
     * Tests standard CSS.
     * </p>
     */
    @Test
    public void testCss() {
        final String css = ".foo {\n"
                + "width:100%;\n"
                + "}\n";
        testCss(">" + css + "</style>", css);
    }

    /**
     * <p>
     * Tests auto close.
     * </p>
     */
    @Test
    public void testBalancedCss() {
        // img is the CDATA of <style>
        testCss("><img>", "<img>");
    }

    /**
     * <p>
     * Runs several assertions for an CSS closed in a specified way.
     * </p>
     *
     * @param closeTag the closing string representation of the tag
     * @param expect the expected content
     */
    public void testCss(final String closeTag, final String expect) {
        final boolean ignore = expect.isEmpty();

        final AdapterFactory factory = new AdapterFactory() {

            /**
             * {@inheritDoc}
             */
            @Override
            public Adapter create(final String[] lines,
                                  final StringBuilder linkBuilder,
                                  final Map<String, String> attributesMap,
                                  final StringBuilder statementBuilder) {
                return new CssHandling(lines, linkBuilder, attributesMap, statementBuilder);
            }
        };

        final Map<String, String> attributes = new LinkedHashMap<String, String>();

        assertHandling("<style " + closeTag, expect, attributes, ignore, factory);
        assertHandling("<style " + closeTag, expect, attributes, ignore, factory);
        assertHandling("<style " + closeTag, expect, attributes, ignore, factory);
        assertHandling("<style    " + closeTag, expect, attributes, ignore, factory);
        assertHandling("<style    " + closeTag, expect, attributes, ignore, factory);
        assertHandling("<style " + closeTag, expect, attributes, ignore, factory);
    }

    /**
     * <p>
     * Tests inline JS.
     * </p>
     */
    @Test
    public void testSrcInlineJs() {
        testSrcJs(">");
    }

    /**
     * <p>
     * Tests standalone JS.
     * </p>
     */
    @Test
    public void testSrcStandaloneJs() {
        testSrcJs("/>");
    }

    /**
     * <p>
     * Runs several assertions for an javascript closed in a specified way.
     * </p>
     *
     * @param closeTag the closing string representation of the tag
     */
    public void testSrcJs(final String closeTag) {
        boolean ignore = false;

        final AdapterFactory factory = new AdapterFactory() {

            /**
             * {@inheritDoc}
             */
            @Override
            public Adapter create(final String[] lines,
                                  final StringBuilder linkBuilder,
                                  final Map<String, String> attributesMap,
                                  final StringBuilder statementBuilder) {
                return new JsSrcHandling(lines, linkBuilder, attributesMap, statementBuilder);
            }
        };

        for (int i = 0; i <= 1; i++) {
            final Map<String, String> attributes = new LinkedHashMap<String, String>();

            assertHandling("<script " + (ignore ? "" : "src=foo.js") + closeTag, "foo.js", attributes, ignore, factory);
            assertHandling("<script " + (ignore ? "" : "src='foo.js'") + closeTag, "foo.js", attributes, ignore, factory);
            assertHandling("<script " + (ignore ? "" : "src=\"foo.js\"") + closeTag, "foo.js", attributes, ignore, factory);
            assertHandling("<script    " + (ignore ? "" : "src=foo.js   ") + closeTag, "foo.js", attributes, ignore, factory);
            assertHandling("<script    " + (ignore ? "" : "src=  'foo.js'") + closeTag, "foo.js", attributes, ignore, factory);
            assertHandling("<script " + (ignore ? "" : "src=\"foo.js\"   ") + closeTag, "foo.js", attributes, ignore, factory);

            attributes.put("async", "true");
            attributes.put("type", "text/javascript");
            assertHandling("<script " + (ignore ? "" : "src=foo.js") + "   async= 'true'  type = text/javascript" + closeTag, "foo.js", attributes, ignore, factory);
            assertHandling("<script  async= 'true'  type =\"text/javascript\" " + (ignore ? "" : "src='foo.js'") + closeTag, "foo.js", attributes, ignore, factory);
            assertHandling("<script  async= 'true'  " + (ignore ? "" : "src=\"foo.js\"") + "  type = text/javascript" + closeTag, "foo.js", attributes, ignore, factory);
            assertHandling("<script   async= 'true' " + (ignore ? "" : "src=foo.js") + " type = text/javascript  " + closeTag, "foo.js", attributes, ignore, factory);
            assertHandling("<script  async= 'true'  type = text/javascript  " + (ignore ? "" : "src=  'foo.js'") + closeTag, "foo.js", attributes, ignore, factory);
            assertHandling("<script async= 'true'  type = text/javascript " + (ignore ? "" : "src=\"foo.js\"") +  closeTag, "foo.js", attributes, ignore, factory);
            ignore = !ignore;
        }
    }

    /**
     * <p>
     * Tests inline JS.
     * </p>
     */
    @Test
    public void testInlineJs() {
        testJs(">", "");
    }

    /**
     * <p>
     * Tests standalone JS.
     * </p>
     */
    @Test
    public void testStandaloneJs() {
        testJs("/>", "");
    }

    /**
     * <p>
     * Tests standard JS.
     * </p>
     */
    @Test
    public void testJs() {
        final String js = "if (1 > 0 && 1 < 2) {\n"
                + "alert('ok');\n"
                + "}\n";
        testJs(">" + js + "</script>", js);
    }

    /**
     * <p>
     * Tests auto close.
     * </p>
     */
    @Test
    public void testBalancedJs() {
        // <style> is the CDATA of <script>
        testJs("><style>", "<style>");
    }

    /**
     * <p>
     * Runs several assertions for an javascript closed in a specified way.
     * </p>
     *
     * @param closeTag the closing string representation of the tag
     * @param expect the expected content
     */
    public void testJs(final String closeTag, final String expect) {
        final boolean ignore = expect.isEmpty();

        final AdapterFactory factory = new AdapterFactory() {

            /**
             * {@inheritDoc}
             */
            @Override
            public Adapter create(final String[] lines,
                                  final StringBuilder linkBuilder,
                                  final Map<String, String> attributesMap,
                                  final StringBuilder statementBuilder) {
                return new JsHandling(lines, linkBuilder, attributesMap, statementBuilder);
            }
        };

        final Map<String, String> attributes = new LinkedHashMap<String, String>();

        assertHandling("<script " + closeTag, expect, attributes, ignore, factory);
        assertHandling("<script " + closeTag, expect, attributes, ignore, factory);
        assertHandling("<script " + closeTag, expect, attributes, ignore, factory);
        assertHandling("<script    " + closeTag, expect, attributes, ignore, factory);
        assertHandling("<script    " + closeTag, expect, attributes, ignore, factory);
        assertHandling("<script " + closeTag, expect, attributes, ignore, factory);

        attributes.put("async", "true");
        attributes.put("type", "text/javascript");
        assertHandling("<script    async= 'true'  type = text/javascript" + closeTag, expect, attributes, ignore, factory);
        assertHandling("<script  async= 'true'  type =\"text/javascript\" " + closeTag, expect, attributes, ignore, factory);
        assertHandling("<script  async= 'true'    type = text/javascript" + closeTag, expect, attributes, ignore, factory);
        assertHandling("<script   async= 'true'  type = text/javascript  " + closeTag, expect, attributes, ignore, factory);
        assertHandling("<script  async= 'true'  type = text/javascript  " + closeTag, expect, attributes, ignore, factory);
        assertHandling("<script async= 'true'  type = text/javascript " +  closeTag, expect, attributes, ignore, factory);
    }

    /**
     * <p>
     * Runs assertions for image handling.
     * </p>
     *
     * @param html the HTML to parse
     * @param src the resource URL
     * @param map additional attributes
     * @param ignore {@code true} if handler should not be notified, {@code false} otherwise
     * @param factory adapter factory
     */
    public void assertHandling(final String html,
                               final String src,
                               final Map<String, String> map,
                               final boolean ignore,
                               final AdapterFactory factory) {
        final StringBuilder linkBuilder = new StringBuilder();
        final Map<String, String> attributesMap = new HashMap<String, String>();
        final StringBuilder statementBuilder = new StringBuilder();
        final String[] lines = html.split("\n");
        final Adapter handler = factory.create(lines, linkBuilder, attributesMap, statementBuilder);

        new AssetsMarkupAttoParser().parse(new StringReader(html), handler);

        if (ignore) {
            Assert.assertEquals("", linkBuilder.toString());
            Assert.assertEquals(new LinkedHashMap<String, String>(), attributesMap);
            Assert.assertEquals("", statementBuilder.toString());
        } else {
            Assert.assertEquals(html, src, linkBuilder.toString());
            Assert.assertEquals(html, map, attributesMap);
            Assert.assertTrue(statementBuilder.toString(), html.startsWith(statementBuilder.toString()));
        }
    }
}
