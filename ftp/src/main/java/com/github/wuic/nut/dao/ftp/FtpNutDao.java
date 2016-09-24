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


package com.github.wuic.nut.dao.ftp;

import com.github.wuic.ApplicationConfig;
import com.github.wuic.NutType;
import com.github.wuic.ProcessContext;
import com.github.wuic.config.*;
import com.github.wuic.config.Config;
import com.github.wuic.exception.WuicException;
import com.github.wuic.nut.AbstractNutDao;
import com.github.wuic.nut.FilePathNut;
import com.github.wuic.nut.InMemoryNut;
import com.github.wuic.nut.Nut;
import com.github.wuic.nut.dao.NutDaoService;
import com.github.wuic.nut.setter.ProxyUrisPropertySetter;
import com.github.wuic.path.DirectoryPath;
import com.github.wuic.path.FilePath;
import com.github.wuic.util.IOUtils;
import com.github.wuic.util.Input;
import com.github.wuic.util.TemporaryFileManager;
import com.github.wuic.util.TemporaryFileManagerHolder;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPSClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>
 * A {@link com.github.wuic.nut.dao.NutDao} implementation for FTP accesses.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.3.1
 */
@NutDaoService
@Alias("ftp")
public class FtpNutDao extends AbstractNutDao implements TemporaryFileManagerHolder {

    /**
     * Reply code that indicates the file is unavailable.
     */
    public static final int FILE_UNAVAILABLE_CODE = 550;

    /**
     * Expected format when retrieved last modification date.
     */
    private static final DateFormat MODIFICATION_TIME_FORMAT = new SimpleDateFormat("yyyyMMddhhmmss");

    /**
     * Logger.
     */
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * The client connected to the server.
     */
    private FTPClient ftpClient;

    /**
     * The host name.
     */
    private String hostName;

    /**
     * The user name.
     */
    private String userName;

    /**
     * The password.
     */
    private String password;

    /**
     * The port.
     */
    private int port;

    /**
     * Use path as regex or not.
     */
    private Boolean regularExpression;

    /**
     * Download to the disk instead of memory.
     */
    private Boolean downloadToDisk;

    /**
     * The temporary file manager.
     */
    private TemporaryFileManager temporaryFileManager;

    /**
     * <p>
     * Initializes a new instance.
     * </p>
     *
     * @param path default the path
     * @param proxies proxy URIs serving the nut
     * @param pollingSeconds interval in seconds for polling feature (-1 to disable)
     */
    @Config
    public void init(@StringConfigParam(propertyKey = ApplicationConfig.BASE_PATH, defaultValue = "") final String path,
                     @ObjectConfigParam(defaultValue = "", propertyKey = ApplicationConfig.PROXY_URIS, setter = ProxyUrisPropertySetter.class) final String[] proxies,
                     @IntegerConfigParam(defaultValue = -1, propertyKey = ApplicationConfig.POLLING_INTERVAL) final int pollingSeconds) {
        super.init(path, proxies, pollingSeconds);
    }

    /**
     * <p>
     * Initializes the FTP data.
     * </p>
     *
     * @param ftps use FTPS or FTP protocol
     * @param host the host name
     * @param p the port
     * @param user the user name ({@code null} to skip the the authentication)
     * @param pwd the password (will be ignored if user is {@code null})
     * @param regex consider path as regex or not
     * @param dtd {@code true} if the resources should be download from the FTP to the disk and not stored in memory
     */
    @Config
    public void init(@BooleanConfigParam(defaultValue = false, propertyKey = ApplicationConfig.SECRET_PROTOCOL) final Boolean ftps,
                     @StringConfigParam(defaultValue = "localhost", propertyKey = ApplicationConfig.SERVER_DOMAIN) final String host,
                     @IntegerConfigParam(defaultValue = FTPClient.DEFAULT_PORT, propertyKey = ApplicationConfig.SERVER_PORT) final int p,
                     @StringConfigParam(defaultValue = "", propertyKey = ApplicationConfig.LOGIN) final String user,
                     @StringConfigParam(defaultValue = "", propertyKey = ApplicationConfig.PASSWORD) final String pwd,
                     @BooleanConfigParam(defaultValue = false, propertyKey = ApplicationConfig.REGEX) final Boolean regex,
                     @BooleanConfigParam(defaultValue = false, propertyKey = ApplicationConfig.DOWNLOAD_TO_DISK) final Boolean dtd) {
        ftpClient = ftps ? new FTPSClient(Boolean.TRUE) : new FTPClient();
        hostName = host;
        userName = user;
        password = pwd;
        port = p;
        regularExpression = regex;
        downloadToDisk = dtd;
    }

    /**
     * <p>
     * Opens a connection with the current FtpClient if its not already opened.
     * </p>
     *
     * @throws IOException if any I/O error occurs, the connection is refused or if the credentials are not correct
     */
    private void connect() throws IOException {
        if (!ftpClient.isConnected()) {
            log.debug("Connecting to FTP server.");

            ftpClient.connect(hostName, port);

            log.debug(ftpClient.getReplyString());

            // After connection attempt, you should check the reply code to verify success
            final int reply = ftpClient.getReplyCode();

            if (!FTPReply.isPositiveCompletion(reply)) {
                throw new IOException("FTP server refused connection.");
            }

            if (userName != null && !ftpClient.login(userName, password)) {
                throw new IOException("Bad FTP credentials.");
            }
        }
    }

    /**
     * <p>
     * Searches recursively in the given path any files matching the given entry.
     * </p>
     *
     * @param path the path
     * @param pattern the pattern to match
     * @return the list of matching files
     * @throws IOException if the client can't move to a directory or any I/O error occurs
     */
    private List<String> recursiveSearch(final String path, final Pattern pattern) throws IOException {
        if (!ftpClient.changeWorkingDirectory(path)) {
            throw new IOException("Can move to the following directory : " + path);
        } else {
            final List<String> retval = new ArrayList<String>();

            // Test each path
            for (final FTPFile file : ftpClient.listFiles()) {
                final Matcher matcher = pattern.matcher(file.getName());

                if (matcher.find()) {
                   retval.add(matcher.group());
                }
            }

            // Search in each directory
            for (final FTPFile directory : ftpClient.listDirectories()) {
                final String pwd = ftpClient.printWorkingDirectory();
                retval.addAll(recursiveSearch(directory.getName(), pattern));

                // Remove quotes around the path
                if (pwd.startsWith("\"") && pwd.endsWith("\"")) {
                    ftpClient.changeWorkingDirectory(new StringBuilder()
                            .append(pwd)
                            .deleteCharAt(pwd.length() - 1)
                            .deleteCharAt(0)
                            .toString());
                } else {
                     ftpClient.changeWorkingDirectory(pwd);
                }
            }

            return retval;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setTemporaryFileManager(final TemporaryFileManager temporaryFileManager) {
        this.temporaryFileManager = temporaryFileManager;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Nut accessFor(final String realPath, final NutType type, final ProcessContext processContext) throws IOException {
        try {
            // Connect if necessary
            connect();

            ftpClient.changeWorkingDirectory(getBasePath());

            // Download to disk
            if (downloadToDisk) {
                final File parent = new File(System.getProperty("java.io.tmpdir"), "wuic-ftp" + System.nanoTime());

                if (!parent.mkdir()) {
                    WuicException.throwStreamException(new IOException("Can't create temporary file to download resource from FTP."));
                } else {
                    // Download to file
                    final File file = new File(parent, realPath);
                    final OutputStream fos = new FileOutputStream(file);
                    IOUtils.copyStream(ftpClient.retrieveFileStream(realPath), fos);
                    fos.close();

                    // Check if download is OK
                    if (!ftpClient.completePendingCommand()) {
                        throw new IOException("FTP command not completed correctly.");
                    }

                    return new DownloadNut(parent, realPath, type, getVersionNumber(realPath, processContext), processContext);
                }
            } else {
                // Download path into memory
                final ByteArrayOutputStream baos = new ByteArrayOutputStream(IOUtils.WUIC_BUFFER_LEN);
                IOUtils.copyStream(ftpClient.retrieveFileStream(realPath), baos);

                // Check if download is OK
                if (!ftpClient.completePendingCommand()) {
                    throw new IOException("FTP command not completed correctly.");
                }

                // Create nut
                return new InMemoryNut(baos.toByteArray(), realPath, type, getVersionNumber(realPath, processContext).get(), false);
            }
        } catch (IOException ioe) {
            WuicException.throwStreamException(ioe);
        } catch (ExecutionException ee) {
            WuicException.throwStreamException(new IOException(ee));
        } catch (InterruptedException ie) {
            WuicException.throwStreamException(new IOException(ie));
        }

        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Long getLastUpdateTimestampFor(final String path) throws IOException {
        try {
            // Connect if necessary
            connect();

            log.info("Polling FTP nut '{}'", path);
            final String response = ftpClient.getModificationTime(IOUtils.mergePath(getBasePath(), path));
            log.info("Last modification response : {}", response);
            log.info("Parse the response with {} date format which could be preceded by the server code and a space",
                    MODIFICATION_TIME_FORMAT);

            return MODIFICATION_TIME_FORMAT.parse(response.substring(response.indexOf(' ') + 1)).getTime();
        } catch (ParseException pe) {
            WuicException.throwStreamException(new IOException(pe));
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void finalize() throws Throwable {
        try {
            log.debug("Disconnecting from FTP server...");

            // This object if not referenced and is going to be garbage collected.
            // Do not keep the client connected.
            ftpClient.disconnect();
        } finally {
            super.finalize();
        }
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return String.format("%s with base path %s", getClass().getName(), getBasePath());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Input newInputStream(final String path, final ProcessContext processContext) throws IOException {
        // Connect if necessary
        connect();

        ftpClient.changeWorkingDirectory(getBasePath());

        // Download path
        final InputStream is = ftpClient.retrieveFileStream(path);
        return newInput(is);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean exists(final String path, final ProcessContext processContext) throws IOException {
        // Connect if necessary
        connect();

        ftpClient.changeWorkingDirectory(getBasePath());

        final Input inputStream = newInputStream(path, processContext);
        return inputStream != null && ftpClient.getReplyCode() != FILE_UNAVAILABLE_CODE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected List<String> listNutsPaths(final String pattern) throws IOException {
        connect();
        return recursiveSearch(getBasePath(), Pattern.compile(regularExpression ? pattern : Pattern.quote(pattern)));
    }

    /**
     * <p>
     * Represents a resource download on disk that corresponds to a nut. If the files does not exists when a stream is
     * opened, the file a downloaded once again.
     * </p>
     *
     * @author Guillaume DROUET
     * @since 0.5.0
     */
    private final class DownloadNut extends FilePathNut {

        /**
         * The underlying file.
         */
        private File file;

        /**
         * The process context.
         */
        private final ProcessContext processContext;

        /**
         * <p>
         * Builds a new instance.
         * </p>
         *
         * @param parent the parent
         * @param name the path
         * @param ft the type
         * @param versionNumber the version number
         * @param pc the process context
         */
        private DownloadNut(final File parent,
                            final String name,
                            final NutType ft,
                            final Future<Long> versionNumber,
                            final ProcessContext pc)
                throws IOException {
            super(FilePath.class.cast(
                    DirectoryPath.class.cast(
                            IOUtils.buildPath(parent.getAbsolutePath(), getCharset(), temporaryFileManager)).getChild(name)), name, ft, versionNumber);
            file = new File(parent, name);
            processContext = pc;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Input openStream() throws IOException {
            if (!file.exists()) {
                final DownloadNut dn = DownloadNut.class.cast(accessFor(getInitialName(), getInitialNutType(), processContext));
                file = dn.file;
                return dn.openStream();
            } else {
                return super.openStream();
            }
        }
    }
}
