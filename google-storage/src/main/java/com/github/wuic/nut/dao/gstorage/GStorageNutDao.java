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


package com.github.wuic.nut.dao.gstorage;

import com.github.wuic.ApplicationConfig;
import com.github.wuic.NutType;
import com.github.wuic.config.BooleanConfigParam;
import com.github.wuic.config.ConfigConstructor;
import com.github.wuic.config.IntegerConfigParam;
import com.github.wuic.config.ObjectConfigParam;
import com.github.wuic.config.StringConfigParam;
import com.github.wuic.exception.NutNotFoundException;
import com.github.wuic.exception.wrapper.StreamException;
import com.github.wuic.nut.AbstractNut;
import com.github.wuic.nut.AbstractNutDao;
import com.github.wuic.nut.Nut;
import com.github.wuic.nut.dao.NutDaoService;
import com.github.wuic.nut.setter.ProxyUrisPropertySetter;
import com.github.wuic.util.IOUtils;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.storage.Storage;
import com.google.api.services.storage.StorageScopes;
import com.google.api.services.storage.model.Objects;
import com.google.api.services.storage.model.StorageObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>
 * A {@link com.github.wuic.nut.dao.NutDao} implementation for Google Cloud Storage accesses.
 * </p>
 *
 * @author Corentin AZELART
 * @author Guillaume DROUET
 * @version 1.5
 * @since 0.3.3
 */
@NutDaoService
public class GStorageNutDao extends AbstractNutDao implements ApplicationConfig {

    /**
     * Logger.
     */
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * The client connected to the Google Cloud Storage.
     */
    private Storage storage;

    /**
     * Bucket name.
     */
    private String bucketName;

    /**
     * Google credential for OAuth2.
     */
    private GoogleCredential googleCredential;

    /**
     * Private key path location.
     */
    private String privateKeyFile;

    /**
     * Google service account id.
     */
    private String serviceAccountId;

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param bucket                    the bucket name
     * @param accountId                 the Google user access ID
     * @param path                      the root path
     * @param basePathAsSysProp         {@code true} if the base path is a system property
     * @param pollingInterval           the interval for polling operations in seconds (-1 to deactivate)
     * @param proxyUris                 the proxies URIs in front of the nut
     * @param keyFile                   the private key path location
     * @param contentBasedVersionNumber {@code true} if version number is computed from nut content, {@code false} if based on timestamp
     */
    @ConfigConstructor
    public GStorageNutDao(@StringConfigParam(propertyKey = BASE_PATH, defaultValue = "") final String path,
                          @BooleanConfigParam(defaultValue = false, propertyKey = BASE_PATH_AS_SYS_PROP) final Boolean basePathAsSysProp,
                          @ObjectConfigParam(defaultValue = "", propertyKey = PROXY_URIS, setter = ProxyUrisPropertySetter.class) final String[] proxyUris,
                          @IntegerConfigParam(defaultValue = -1, propertyKey = POLLING_INTERVAL) final int pollingInterval,
                          @StringConfigParam(defaultValue = "", propertyKey = CLOUD_BUCKET) final String bucket,
                          @StringConfigParam(defaultValue = "", propertyKey = LOGIN) final String accountId,
                          @StringConfigParam(defaultValue = "", propertyKey = PASSWORD) final String keyFile,
                          @BooleanConfigParam(defaultValue = false, propertyKey = CONTENT_BASED_VERSION_NUMBER) final Boolean contentBasedVersionNumber) {
        super(path, basePathAsSysProp, proxyUris, pollingInterval, contentBasedVersionNumber);
        bucketName = bucket;
        privateKeyFile = keyFile;
        serviceAccountId = accountId;
    }

    /**
     * Check if OAuth token is always alive.
     *
     * @throws IOException if token can't be refresh
     */
    private void checkGoogleOAuth2() throws IOException {
        // Check if we have build Google credential
        if (googleCredential == null) {
            this.buildOAuth2();
        }

        // Check token
        if (googleCredential.getExpiresInSeconds() == null || googleCredential.getExpirationTimeMilliseconds() == 0) {
            googleCredential.refreshToken();
        }
    }

    /**
     * Build OAuth 2 Google Credential.
     *
     * @throws IOException if we have a problem to build Google credential
     */
    private void buildOAuth2() throws IOException {
        final NetHttpTransport netHttpTransport = new NetHttpTransport();
        final JsonFactory jsonFactory = new JacksonFactory();

        try {
            // Configure Google credential
            final GoogleCredential.Builder builder = new GoogleCredential.Builder();
            builder.setTransport(netHttpTransport);
            builder.setJsonFactory(jsonFactory);
            builder.setServiceAccountId(serviceAccountId);

            // TODO : raise exception if private key not found
            final String keyPath = IOUtils.mergePath("/", privateKeyFile);
            builder.setServiceAccountPrivateKeyFromP12File(new File(getClass().getResource(keyPath).getFile()));
            builder.setServiceAccountScopes(Arrays.asList(StorageScopes.DEVSTORAGE_FULL_CONTROL));

            // Build Google credential
            googleCredential = builder.build();

            // Build Google Storage connector
            storage = new Storage.Builder(netHttpTransport, jsonFactory, googleCredential).setApplicationName("Wuic").build();
        } catch (GeneralSecurityException gse) {
            // Security exception (local check)
            throw new IOException("Can't build Google credential on bucket " + bucketName + " for key : " + getBasePath(), gse);
        } catch (IOException ioe) {
            // Private key path not found
            throw new IOException("Can't build Google credential on bucket " + bucketName + " for key : " + getBasePath() + " check your private key file", ioe);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> listNutsPaths(final String pattern) throws StreamException {

        try {
            // Check if we are ready to read on Google Storage
            this.checkGoogleOAuth2();

            return recursiveSearch(getBasePath(), Pattern.compile(pattern));
        } catch (IOException ioe) {
            throw new StreamException(ioe);
        }
    }

    /**
     * <p>
     * Searches recursively in the given path any files matching the given entry.
     * </p>
     *
     * @param path    the path
     * @param pattern the pattern to match
     * @return the list of matching files
     * @throws StreamException if the client can't move to a directory or any I/O error occurs
     */
    private List<String> recursiveSearch(final String path, final Pattern pattern) throws StreamException {
        Objects objectListing;

        try {
            objectListing = storage.objects().list(bucketName).execute();
        } catch (IOException ioe) {
            throw new StreamException(new IOException(String.format("Can't get Google Storage Object on bucket %s for nut key : %s", bucketName, path), ioe));
        }

        final List<String> retval = new ArrayList<String>();
        for (final StorageObject storageObject : objectListing.getItems()) {
            // Ignore directories, all nuts are in the listing
            if (!storageObject.getName().endsWith("/")) {
                final Matcher matcher = pattern.matcher(storageObject.getName());

                if (matcher.find()) {
                    retval.add(storageObject.getName());
                }
            }
        }

        return retval;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Nut accessFor(final String realPath, final NutType type) throws StreamException {
        // Create nut
        return new GStorageNut(realPath, type, getVersionNumber(realPath));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Long getLastUpdateTimestampFor(final String path) throws StreamException {
        log.info("Polling GStorage nut '{}'", path);
        try {
            final String response = storage.objects().get(bucketName, path).execute().getMd5Hash();
            log.info("Last MD5 response : {}", response);
            return storage.objects().get(bucketName, path).execute().getGeneration();
        } catch (final IOException ioe) {
            throw new StreamException(ioe);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void finalize() throws Throwable {
        try {
            log.debug("Release Google credential ticket...");

            // For security, we force Google credential expiration to now.
            googleCredential.setExpirationTimeMilliseconds(new Long(0));
        } finally {
            super.finalize();
        }
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
    public InputStream newInputStream(final String path) throws StreamException {
        try {
            // Try to get a Storage object
            return storage.objects().get(bucketName, path).executeMediaAsInputStream();
        } catch (IOException ioe) {
            throw new StreamException(ioe);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean exists(final String path) throws StreamException {
        try {
            // Try to get a Storage object
            storage.objects().get(bucketName, path).executeMediaAsInputStream().close();
            return true;
        } catch (IOException ioe) {
            return false;
        }
    }

    /**
     * <p>
     * Nut for GStorage.
     * </p>
     *
     * @author Guillaume DROUET
     * @version 1.0
     * @since 0.5.0
     */
    private final class GStorageNut extends AbstractNut {

        /**
         * <p>
         * Creates a new instance.
         * </p>
         *
         * @param name the name
         * @param nt the {@link NutType}
         * @param v the version number
         */
        private GStorageNut(final String name, final NutType nt, final Future<Long> v) {
            super(name, nt, Boolean.FALSE, Boolean.TRUE, Boolean.TRUE, v);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public InputStream openStream() throws NutNotFoundException {
            try {
                // Try to get a Storage object
                final Storage.Objects.Get storageObject = storage.objects().get(bucketName, getInitialName());

                // Download path
                return storageObject.executeMediaAsInputStream();
            } catch (IOException ioe) {
                throw new NutNotFoundException(ioe);
            }
        }
    }
}
