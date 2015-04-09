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


package com.github.wuic.nut.dao.ssh;

import com.github.wuic.ApplicationConfig;
import com.github.wuic.NutType;
import com.github.wuic.ProcessContext;
import com.github.wuic.config.BooleanConfigParam;
import com.github.wuic.config.ConfigConstructor;
import com.github.wuic.config.IntegerConfigParam;
import com.github.wuic.config.ObjectConfigParam;
import com.github.wuic.config.StringConfigParam;
import com.github.wuic.exception.WuicException;
import com.github.wuic.nut.AbstractNut;
import com.github.wuic.nut.AbstractNutDao;
import com.github.wuic.nut.dao.NutDaoService;
import com.github.wuic.nut.Nut;
import com.github.wuic.nut.setter.ProxyUrisPropertySetter;

import java.io.IOException;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.Future;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * A {@link com.github.wuic.nut.dao.NutDao} implementation for SSH accesses.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.6
 * @since 0.3.1
 */
@NutDaoService
public class SshNutDao extends AbstractNutDao implements ApplicationConfig {

    /**
     * Default SSH port.
     */
    private static final int DEFAULT_PORT = 22;

    /**
     * SFTP channel usage.
     */
    private static final String SFTP_CHANNEL = "sftp";

    /**
     * Common exception message.
     */
    private static final String CANNOT_LOAD_MESSAGE = "Can't load the file remotely with SSH FTP";

    /**
     * The logger.
     */
    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * The SSH session.
     */
    private Session session;

    /**
     * Path considered as regular expression or not.
     */
    private Boolean regularExpression;

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param regex consider paths as regex or not
     * @param host the host name
     * @param p the port
     * @param path default the path
     * @param user the user name ({@code null} to skip the the authentication)
     * @param pwd the password (will be ignored if user is {@code null})
     * @param pollingInterval the interval for polling operations in seconds (-1 to deactivate)
     * @param proxyUris the proxies URIs in front of the nut
     * @param contentBasedVersionNumber {@code true} if version number is computed from nut content, {@code false} if based on timestamp
     * @param computeVersionAsynchronously (@code true} if version number can be computed asynchronously, {@code false} otherwise
     * @param fixedVersionNumber fixed version number, {@code null} if version number is computed from content or is last modification date
     */
    @ConfigConstructor
    public SshNutDao(@BooleanConfigParam(defaultValue = false, propertyKey = REGEX) final Boolean regex,
                     @StringConfigParam(defaultValue = "localhost", propertyKey = SERVER_DOMAIN) final String host,
                     @IntegerConfigParam(defaultValue = DEFAULT_PORT, propertyKey = SERVER_PORT) final int p,
                     @StringConfigParam(propertyKey = BASE_PATH, defaultValue = ".") final String path,
                     @BooleanConfigParam(defaultValue = false, propertyKey = BASE_PATH_AS_SYS_PROP) final Boolean basePathAsSysProp,
                     @StringConfigParam(defaultValue = "", propertyKey = LOGIN) final String user,
                     @StringConfigParam(defaultValue = "", propertyKey = PASSWORD) final String pwd,
                     @ObjectConfigParam(defaultValue = "", propertyKey = PROXY_URIS, setter = ProxyUrisPropertySetter.class) final String[] proxyUris,
                     @IntegerConfigParam(defaultValue = -1, propertyKey = POLLING_INTERVAL) final int pollingInterval,
                     @BooleanConfigParam(defaultValue = false, propertyKey = CONTENT_BASED_VERSION_NUMBER) final Boolean contentBasedVersionNumber,
                     @BooleanConfigParam(defaultValue = true, propertyKey = COMPUTE_VERSION_ASYNCHRONOUSLY) final Boolean computeVersionAsynchronously,
                     @StringConfigParam(defaultValue = "", propertyKey = ApplicationConfig.FIXED_VERSION_NUMBER) final String fixedVersionNumber) {
        super(path, basePathAsSysProp, proxyUris, pollingInterval,
                new VersionNumberStrategy(contentBasedVersionNumber, computeVersionAsynchronously, fixedVersionNumber));
        regularExpression = regex;

        final JSch jsch = new JSch();

        try {
            session = jsch.getSession(user, host, p);
            session.setPassword(pwd);
            final Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
        } catch (JSchException je) {
            throw new IllegalStateException("Can't open SSH session", je);
        }
    }

    /**
     * <p>
     * Connects the current session.
     * </p>
     *
     * @throws JSchException if connection fails
     */
    private void connect() throws JSchException {
        if (!session.isConnected()) {
            session.connect();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<String> listNutsPaths(final String pattern) throws IOException {
        ChannelSftp channel = null;

        try {
            connect();

            if (regularExpression) {
                channel = (ChannelSftp) session.openChannel(SFTP_CHANNEL);
                channel.connect();
                channel.cd(getBasePath());
                final List<ChannelSftp.LsEntry> list = channel.ls(pattern);
                final List<String> retval = new ArrayList<String>(list.size());

                for (final ChannelSftp.LsEntry entry : list) {
                    retval.add(entry.getFilename());
                }

                return retval;
            } else {
                return Arrays.asList(pattern);
            }
        } catch (JSchException je) {
            WuicException.throwStreamException(new IOException(je));
            return null;
        } catch (SftpException se) {
            WuicException.throwStreamException(new IOException(se));
            return null;
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Nut accessFor(final String path, final NutType type, final ProcessContext processContext) throws IOException {
        return new SshNut(path, type, getVersionNumber(path, processContext));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Long getLastUpdateTimestampFor(final String path) throws IOException {
        ChannelSftp channel = null;

        try {
            channel = (ChannelSftp) session.openChannel(SFTP_CHANNEL);
            channel.connect();
            channel.cd(getBasePath());
            return (long) channel.stat(path).getMTime();
        } catch (JSchException je) {
            WuicException.throwStreamException(new IOException(je));
            return null;
        } catch (SftpException se) {
            WuicException.throwStreamException(new IOException(CANNOT_LOAD_MESSAGE, se));
            return null;
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void finalize() throws Throwable {

        // Disconnect the session if this instance is not referenced anymore
        if (session.isConnected()) {
            session.disconnect();
        }

        super.finalize();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return String.format("%s with base path %s", getClass().getName(), getBasePath());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream newInputStream(final String path, final ProcessContext processContext) throws IOException {
        ChannelSftp channel = null;

        try {
            channel = open();
            return channel.get(path);
        } catch (SftpException se) {
            WuicException.throwStreamException(new IOException("An SSH FTP error prevent remote file loading", se));
            return null;
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean exists(final String path, final ProcessContext processContext) throws IOException {
        ChannelSftp channel = null;
        boolean exception = false;
        try {
            channel = open();
        } catch (SftpException se) {
            exception = true;
            WuicException.throwStreamException(new IOException("An SSH FTP error prevent remote file loading", se));
            return false;
        } finally {
            if (exception) {
                channel.disconnect();
            }
        }

        try {
            channel.lstat(path);
            return Boolean.TRUE;
        } catch (SftpException se) {
            log.debug("A path does not exists", se);
            return Boolean.FALSE;
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
    }

    /**
     * <p>
     * Open the returned channel and change directory to base path.
     * </p>
     *
     * @throws IOException if an I/O error occurs
     * @throws SftpException if connection could not be opened
     * @return the channel
     */
    public ChannelSftp open() throws IOException, SftpException {
        boolean exception = false;
        ChannelSftp channel = null;

        try {
            connect();

            channel = (ChannelSftp) session.openChannel(SFTP_CHANNEL);
            channel.connect();
            channel.cd(getBasePath());
            exception = true;
            return channel;
        } catch (JSchException je) {
            WuicException.throwStreamException(new IOException(CANNOT_LOAD_MESSAGE, je));
            return null;
        } finally {
            if (exception) {
                channel.disconnect();
            }
        }
    }

    /**
     * <p>
     * Nut for SSH.
     * </p>
     *
     * @author Guillaume DROUET
     * @version 1.0
     * @since 0.5.0
     */
    private final class SshNut extends AbstractNut {

        /**
         * <p>
         * Creates a new instance.
         * </p>
         *
         * @param name the name
         * @param nt the {@link NutType}
         * @param v the version number
         */
        private SshNut(final String name, final NutType nt, final Future<Long> v) {
            super(name, nt, v);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public InputStream openStream() throws IOException {
            ChannelSftp channel;

            try {
                connect();

                channel = (ChannelSftp) session.openChannel(SFTP_CHANNEL);
                channel.connect();
                channel.cd(getBasePath());
                return channel.get(getInitialName());
            } catch (JSchException je) {
                WuicException.throwStreamException(new IOException(CANNOT_LOAD_MESSAGE, je));
                return null;
            } catch (SftpException se) {
                WuicException.throwStreamException(new IOException("An SSH FTP error prevent remote file loading", se));
                return null;
            }
        }
    }
}
