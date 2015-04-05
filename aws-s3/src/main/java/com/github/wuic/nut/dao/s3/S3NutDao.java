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


package com.github.wuic.nut.dao.s3;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.sns.model.NotFoundException;
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
import com.github.wuic.nut.Nut;
import com.github.wuic.nut.dao.NutDaoService;
import com.github.wuic.nut.setter.ProxyUrisPropertySetter;
import com.github.wuic.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>
 * A {@link com.github.wuic.nut.dao.NutDao} implementation for S3 AWS Cloud accesses.
 * </p>
 *
 * @author Corentin AZELART
 * @author Guillaume DROUET
 * @version 1.7
 * @since 0.3.3
 */
@NutDaoService
public class S3NutDao extends AbstractNutDao implements ApplicationConfig {

    /**
     * Logger.
     */
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * The client connected to the S3 AWS.
     */
    private AmazonS3Client amazonS3Client;

    /**
     * Bucket name.
     */
    private String bucketName;

    /**
     * Access key.
     */
    private String login;

    /**
     * Secret key.
     */
    private String password;

    /**
     * Use path as regex or not.
     */
    private Boolean regularExpression;

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param bucket the bucket name
     * @param accessKey the user access key
     * @param secretKey the user private key
     * @param path the root path
     * @param basePathAsSysProp {@code true} if the base path is a system property
     * @param pollingInterval the interval for polling operations in seconds (-1 to deactivate)
     * @param proxyUris the proxies URIs in front of the nut
     * @param regex consider path as regex or not
     * @param contentBasedVersionNumber  {@code true} if version number is computed from nut content, {@code false} if based on timestamp
     * @param computeVersionAsynchronously (@code true} if version number can be computed asynchronously, {@code false} otherwise
     */
    @ConfigConstructor
    public S3NutDao(@StringConfigParam(propertyKey = BASE_PATH, defaultValue = "") final String path,
                    @BooleanConfigParam(defaultValue = false, propertyKey = BASE_PATH_AS_SYS_PROP) final Boolean basePathAsSysProp,
                    @ObjectConfigParam(defaultValue = "", propertyKey = PROXY_URIS, setter = ProxyUrisPropertySetter.class) final String[] proxyUris,
                    @IntegerConfigParam(defaultValue = -1, propertyKey = POLLING_INTERVAL) final int pollingInterval,
                    @StringConfigParam(defaultValue = "", propertyKey = CLOUD_BUCKET) final String bucket,
                    @StringConfigParam(defaultValue = "", propertyKey = LOGIN)final String accessKey,
                    @StringConfigParam(defaultValue = "", propertyKey = PASSWORD) final String secretKey,
                    @BooleanConfigParam(defaultValue = false, propertyKey = REGEX) final Boolean regex,
                    @BooleanConfigParam(defaultValue = false, propertyKey = CONTENT_BASED_VERSION_NUMBER) final Boolean contentBasedVersionNumber,
                    @BooleanConfigParam(defaultValue = true, propertyKey = COMPUTE_VERSION_ASYNCHRONOUSLY) final Boolean computeVersionAsynchronously) {
        super(path, basePathAsSysProp, proxyUris, pollingInterval, contentBasedVersionNumber, computeVersionAsynchronously);
        bucketName = bucket;
        login = accessKey;
        password = secretKey;
        regularExpression = regex;
    }

    /**
     * <p>
     * Connects to S3 if not already connected.
     * </p>
     */
    public void connect() {
        if (login != null && password != null && amazonS3Client == null) {
            amazonS3Client = initClient();
        }
    }

    /**
     * <p>
     * Initializes a new client based on the instance's credentials.
     * </p>
     *
     * @return the client
     */
    public AmazonS3Client initClient() {
        return new AmazonS3Client(new BasicAWSCredentials(login, password));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> listNutsPaths(final String pattern) throws IOException {
        return recursiveSearch(getBasePath(), Pattern.compile(regularExpression ? pattern : Pattern.quote(pattern)));
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

        ObjectListing objectListing;

        try {
            final String finalSuffix =  path.equals("") ? "" : "/";
            connect();
            objectListing = amazonS3Client.listObjects(new ListObjectsRequest().withBucketName(bucketName).withPrefix(IOUtils.mergePath(path.substring(1), finalSuffix)).withDelimiter("/"));
        } catch (AmazonServiceException ase) {
            WuicException.throwStreamException(new IOException(String.format("Can't get S3Object on bucket %s for nut key : %s", bucketName, path), ase));
            return null;
        }

        final List<String> retval = new ArrayList<String>();
        for (final S3ObjectSummary s3ObjectSummary : objectListing.getObjectSummaries()) {
            // Ignore directories, all nuts are in the listing
            if (!s3ObjectSummary.getKey().endsWith("/")) {
                final Matcher matcher = pattern.matcher(s3ObjectSummary.getKey());

                if (matcher.find()) {
                    retval.add(s3ObjectSummary.getKey());
                }
            }
        }

        // Recursive search on prefixes (directories)
        for (final String s3CommonPrefix : objectListing.getCommonPrefixes()) {
            retval.addAll(recursiveSearch(s3CommonPrefix.substring(0, s3CommonPrefix.length() - 1), pattern));
        }

        return retval;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Nut accessFor(final String realPath, final NutType type, final ProcessContext processContext) throws IOException {
        // Create nut
        return new S3Nut(realPath, type, getVersionNumber(realPath, processContext));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Long getLastUpdateTimestampFor(final String path) throws IOException {
        try {
            // Connect if necessary
            connect();

            log.info("Polling S3 nut '{}'", path);
            final ObjectMetadata objectMetadata = amazonS3Client.getObjectMetadata(bucketName, path);
            final String response = objectMetadata.getLastModified().toString();
            log.info("Last modification response : {}", response);

            return objectMetadata.getLastModified().getTime();
        } catch (AmazonClientException ase) {
            WuicException.throwStreamException(new IOException(ase));
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(final Nut nut) {
        // TODO : implement S3 upload
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean saveSupported() {
        // TODO : return true once save() is implemented
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream newInputStream(final String path, final ProcessContext processContext) throws IOException {
        try {
            connect();
            return amazonS3Client.getObject(bucketName, path).getObjectContent();
        } catch (AmazonServiceException ase) {
            WuicException.throwStreamException(new IOException(String.format("Can't get S3Object on bucket %s  for nut key : %s", bucketName, path), ase));
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean exists(final String path, final ProcessContext processContext) throws IOException {
        try {
            connect();
            amazonS3Client.getObject(bucketName, path).getObjectContent().close();
            return Boolean.TRUE;
        } catch (NotFoundException nfe) {
            return Boolean.FALSE;
        } catch (AmazonServiceException ase) {
           WuicException.throwStreamException(new IOException(String.format("Can't get S3Object on bucket %s  for nut key : %s", bucketName, path), ase));
            return Boolean.FALSE;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void finalize() throws Throwable {
        try {
            log.debug("Disconnecting from S3 AWS Cloud...");

            // This object if not referenced and is going to be garbage collected.
            // Do not keep the client connected.
            if (amazonS3Client != null) {
                amazonS3Client.shutdown();
            }
        } finally {
            super.finalize();
        }
    }

    /**
     * <p>
     * Nut for S3.
     * </p>
     *
     * @author Guillaume DROUET
     * @version 1.0
     * @since 0.5.0
     */
    private final class S3Nut extends AbstractNut {

        /**
         * <p>
         * Creates a new instance.
         * </p>
         *
         * @param name the name
         * @param nt the {@link NutType}
         * @param v the version number
         */
        private S3Nut(final String name, final NutType nt, final Future<Long> v) {
            super(name, nt, v);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public InputStream openStream() throws IOException {
            // Try to get S3 object
            S3Object s3Object;

            try {
                connect();
                s3Object = amazonS3Client.getObject(bucketName, getInitialName());
            } catch (AmazonServiceException ase) {
                WuicException.throwStreamException(new IOException(String.format("Can't get S3Object on bucket %s  for nut key : %s", bucketName, getInitialName()), ase));
               return null;
            }

            S3ObjectInputStream s3ObjectInputStream = null;
            try {
                // Get S3Object content
                return s3Object.getObjectContent();
            } finally {
                // Close S3Object stream
                IOUtils.close(s3ObjectInputStream);
            }
        }
    }
}
