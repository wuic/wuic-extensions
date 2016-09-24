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


package com.github.wuic.engine.attoparser;

import com.github.wuic.engine.core.AssetsMarkupHandler;
import com.github.wuic.util.NumberUtils;
import org.attoparser.AbstractMarkupHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * <p>
 * This class is a specialized handler from <i>atto project</i> that parses any HTML document to detect assets (image,
 * javascript, CSS), comments and the special {@code wuic:html-import} built-in element.
 * </p>
 *
 * <p>
 * Any implementation of {@link AssetsMarkupHandler} can be used to construct this instance in order to see its
 * following method invoked when a new asset has been detected:
 * <ul>
 *     <li>{@link AssetsMarkupHandler#handleComment(char[], int, int, int)}</li>
 *     <li>{@link AssetsMarkupHandler#handleImgLink(String, java.util.Map, int, int, int, int)}</li>
 *     <li>{@link AssetsMarkupHandler#handleCssContent(char[], java.util.Map, int, int, int, int)}</li>
 *     <li>{@link AssetsMarkupHandler#handleLink(String, java.util.Map, int, int, int, int)}</li>
 *     <li>{@link AssetsMarkupHandler#handleJavascriptContent(char[], java.util.Map, int, int, int, int)}</li>
 *     <li>{@link AssetsMarkupHandler#handleScriptLink(String, java.util.Map, int, int, int, int)}</li>
 *     <li>{@link AssetsMarkupHandler#handleImport(String, java.util.Map, int, int, int, int)}</li>
 * </ul>
 *
 * The handler is configured to accept content not strictly respecting well-formed document rules.
 * This allows to handle especially tags that are never closed.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.3
 */
public class AssetsMarkupAttoHandler extends AbstractMarkupHandler implements Comparator<String> {

    /**
     * <p>
     * Represents an handled asset group.
     * </p>
     *
     * @author Guillaume DROUET
     * @since 0.5.3
     */
    private static final class Handled {

        /**
         * The handled asset group.
         */
        private Group group;

        /**
         * The attributes except the one containing the link.
         */
        private final Map<String, String> attributes;

        /**
         * The line where group starts.
         */
        private int startLine;

        /**
         * The column where group starts.
         */
        private int startColumn;

        /**
         * The line where group ends.
         */
        private int endLine;

        /**
         * The column where group ends.
         */
        private int endColumn;

        /**
         * The content is any.
         */
        private char[] content;

        /**
         * The link attribute value.
         */
        private String link;

        /**
         * <p>
         * Builds a new instance.
         * </p>
         */
        private Handled() {
            this.attributes = new LinkedHashMap<String, String>();
        }
    }

    /**
     * The group currently handled.
     */
    private Handled current;

    /**
     * The group to auto close.
     */
    private Handled autoClose;

    /**
     * The logger.
     */
    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * The handler to be notified.
     */
    private final AssetsMarkupHandler handler;

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param h the handler
     */
    public AssetsMarkupAttoHandler(final AssetsMarkupHandler h) {
        this.handler = h;
    }

    /**
     * <p>
     * Call the correct handler's method according to the group currently detected.
     * </p>
     */
    private void fireEvent() {
        switch (current.group) {

            // Script asset
            case JS_GROUP:
                fireScriptGroup();
                break;

            // Link asset
            case LINK_GROUP:
                fireLinkGroup();
                break;

            // CSS asset
            case STYLE_GROUP:
                fireCssGroup();
                break;

            // Image group
            case IMG_GROUP:
                fireImgGroup();
                break;

            // Built-in import component
            case IMPORT_GROUP:
                fireImportGroup();
                break;
        }

        // Notification done: clear info
        clear();
    }

    /**
     * Fires script event.
     */
    private void fireScriptGroup() {
        if (current.link != null) {
            // Link resource
            handler.handleScriptLink(
                    current.link, current.attributes, current.startLine, current.startColumn, current.endLine, current.endColumn);
        } else if (current.content != null) {
            // Content resource
            handler.handleJavascriptContent(
                    current.content, current.attributes, current.startLine, current.startColumn, current.endLine, current.endColumn);
        } else {
            // Invalid
            logger.info("Script between L. {} / C. {} and L. {} and C. does not refer a resource link or a resource content, ignoring...",
                    current.startLine, current.startColumn, current.endLine, current.endColumn);
        }
    }

    /**
     * <p>
     * Fires link event.
     * </p>
     */
    public void fireLinkGroup() {
        if (current.link != null) {
            // Link resource
            handler.handleLink(
                    current.link, current.attributes, current.startLine, current.startColumn, current.endLine, current.endColumn);
        } else {
            // Invalid
            logger.info("Link between L. {} / C. {} and L. {} and C. does not refer a resource link, ignoring...",
                    current.startLine, current.startColumn, current.endLine, current.endColumn);
        }
    }

    /**
     * <p>
     * Fires CSS event.
     * </p>
     */
    private void fireCssGroup() {
        if (current.content != null) {
            // Content resource
            handler.handleCssContent(
                    current.content, current.attributes, current.startLine, current.startColumn, current.endLine, current.endColumn);
        } else {
            // Invalid
            logger.info("Link between L. {} / C. {} and L. {} and C. does not refer a resource content, ignoring...",
                    current.startLine, current.startColumn, current.endLine, current.endColumn);
        }
    }

    /**
     * <p>
     * Fires image event.
     * </p>
     */
    private void fireImgGroup() {
        if (current.link != null) {
            // Resource link
            handler.handleImgLink(
                    current.link, current.attributes, current.startLine, current.startColumn, current.endLine, current.endColumn);
        } else {
            // Invalid
            logger.info("Image between L. {} / C. {} and L. {} and C. does not refer a resource link, ignoring...",
                    current.startLine, current.startColumn, current.endLine, current.endColumn);
        }
    }

    /**
     * <p>
     * Fires import event.
     * </p>
     */
    private void fireImportGroup() {
        if (current.link != null) {
            // Import
            handler.handleImport(
                    current.link, current.attributes, current.startLine, current.startColumn, current.endLine, current.endColumn);
        } else {
            // Invalid
            logger.info("Import between L. {} / C. {} and L. {} and C. does not refer a resource link, ignoring...",
                    current.startLine, current.startColumn, current.endLine, current.endColumn);
        }
    }

    /**
     * <p>
     * Clears the group currently handled.
     * </p>
     */
    private void clear() {
        current = null;
    }

    /**
     * <p>
     * Marks the beginning of an handled asset.
     * </p>
     *
     * @param buffer the buffer containing the name
     * @param nameOffset the name offset
     * @param nameLen the name length
     * @param line the starting line
     * @param col the starting column
     */
    public void start(final char[] buffer,
                      final int nameOffset,
                      final int nameLen,
                      final int line,
                      final int col) {

        // auto close
        if (current != null) {
            end(current.endLine, current.endColumn);
        }

        final String tag = new String(buffer, nameOffset, nameLen);
        final int position = Arrays.binarySearch(Group.TAGS, tag, this);

        // Not a supported group
        if (position > -1) {
            current = new Handled();
            current.group = Group.values()[position];
            current.startLine = line;
            current.startColumn = col;
        }
    }

    /**
     * <p>
     * Marks the end of an handled asset.
     * </p>
     *
     * @param line the ending line
     * @param col the ending column
     */
    private void end(final int line, final int col) {
        if (current != null)  {
            current.endLine = line;
            current.endColumn = col;
            fireEvent();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compare(final String o1, final String o2) {
        return o1.compareToIgnoreCase(o2);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleComment(final char[] buffer,
                              final int contentOffset,
                              final int contentLen,
                              final int outerOffset,
                              final int outerLen,
                              final int line,
                              final int col) {
        final char[] content = new char[contentLen];
        System.arraycopy(buffer, contentOffset, content, 0, contentLen);
        int len = outerLen;

        // Ignores new line characters
        for (int i = 0; i < content.length; i++) {
            final char c = content[i];

            // New line
            if (c == '\n') {
                len--;

                // Handle \r\n case
                if (i > 0 && content[i - 1] == '\r') {
                    len--;
                }
            }
        }

        handler.handleComment(content, line, col, len);
        clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleOpenElementStart(final char[] buffer,
                                       final int nameOffset,
                                       final int nameLen,
                                       final int line,
                                       final int col) {
        start(buffer, nameOffset, nameLen, line, col);
    }

    @Override
    public void handleOpenElementEnd(final char[] buffer,
                                     final int nameOffset,
                                     final int nameLen,
                                     final int line,
                                     final int col) {
        if (current != null) {
            current.endLine = line;
            current.endColumn = col + 1;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleStandaloneElementStart(final char[] buffer,
                                             final int nameOffset,
                                             final int nameLen,
                                             final boolean minimized,
                                             final int line,
                                             final int col) {
        start(buffer, nameOffset, nameLen, line, col);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleCloseElementEnd(final char[] buffer,
                                      final int nameOffset,
                                      final int nameLen,
                                      final int line,
                                      final int col) {
        end(line, col + 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleAutoCloseElementEnd(final char[] buffer,
                                          final int nameOffset,
                                          final int nameLen,
                                          final int line,
                                          final int col) {
        if (autoClose != null) {
            current = autoClose;
            autoClose = null;
        }

        end(line, col);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleStandaloneElementEnd(final char[] buffer,
                                           final int nameOffset,
                                           final int nameLen,
                                           final boolean minimized,
                                           final int line,
                                           final int col) {
        end(line, col + (minimized ? NumberUtils.TWO : 1));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleUnmatchedCloseElementEnd(final char[] buffer,
                                               final int nameOffset,
                                               final int nameLen,
                                               final int line,
                                               final int col) {
        clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleAttribute(final char[] buffer,
                                final int nameOffset,
                                final int nameLen,
                                final int nameLine,
                                final int nameCol,
                                final int operatorOffset,
                                final int operatorLen,
                                final int operatorLine,
                                final int operatorCol,
                                final int valueContentOffset,
                                final int valueContentLen,
                                final int valueOuterOffset,
                                final int valueOuterLen,
                                final int valueLine,
                                final int valueCol) {
        if (current != null) {
            final String name = new String(buffer, nameOffset, nameLen);
            final String value = new String(buffer, valueContentOffset, valueContentLen);

            if (current.group.getLinkAttribute().equals(name)) {
                current.link = value;
            } else {
                current.attributes.put(name, value);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleText(final char[] buffer, final int offset, final int len, final int line, final int col) {
        if (current != null) {
            current.content = new char[len];
            System.arraycopy(buffer, offset, current.content, 0, len);
        }
    }

    /**
     * <p>
     * This enumeration defines metadata for all handled assets.
     * Order of declaration is important because binary research is performed based on {@code ordinal()}  method.
     * </p>
     *
     * @author Guillaume DROUET
     * @since 0.5.3
     */
    private enum Group {

        /**
         * Image asset.
         */
        IMG_GROUP("img", "src"),

        /**
         * Link (like CSS) reference asset.
         */
        LINK_GROUP("link", "href"),

        /**
         * Javascript asset.
         */
        JS_GROUP("script", "src"),

        /**
         * CSS content asset.
         */
        STYLE_GROUP("style", null),

        /**
         * Built-in WUIC {@code wuic:html-import}.
         */
        IMPORT_GROUP("wuic:html-import", "workflowId");

        /**
         * All the tag names for binary research.
         */
        private static final String[] TAGS = new String[values().length];

        static {
            // Build the array from all enumeration
            for (final Group group : values()) {
                TAGS[group.ordinal()] = group.getTagName();
            }
        }

        /**
         * The tag name.
         */
        private String tagName;

        /**
         * The resource link name.
         */
        private String linkAttribute;

        /**
         * <p>
         * Builds a new instance.
         * </p>
         *
         * @param tag the tag name
         * @param linkAttr the attribute name referencing the resource link
         */
        private Group(final String tag, final String linkAttr) {
            tagName = tag;
            linkAttribute = linkAttr;
        }

        /**
         * <p>
         * Returns the tag name.
         * </p>
         *
         * @return the tag name
         */
        String getTagName() {
            return tagName;
        }

        /**
         * <p>
         * Returns the link attribute.
         * </p>
         *
         * @return the link attribute
         */
        String getLinkAttribute() {
            return linkAttribute;
        }
    }
}
