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


package com.github.wuic.ssh.test;

import com.github.wuic.ApplicationConfig;
import com.github.wuic.ProcessContext;
import com.github.wuic.WuicFacadeBuilder;
import com.github.wuic.exception.WuicException;
import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.nut.Nut;
import com.github.wuic.config.bean.xml.FileXmlContextBuilderConfigurator;
import com.jcraft.jsch.JSchException;
import org.apache.sshd.SshServer;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.sftp.SftpSubsystem;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.File;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * Tests for FTP module.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.3.1
 */
@RunWith(JUnit4.class)
public class SshTest {

    /**
     * SSH server.
     */
    private static SshServer sshdServer;

    /**
     * Timeout.
     */
    @Rule
    public Timeout globalTimeout = Timeout.seconds(60);

    /**
     * <p>
     * Starts the server.
     * </p>
     */
    @BeforeClass
    public static void tearUp() throws IOException {

        // Default server on port 9876
        sshdServer = SshServer.setUpDefaultServer();
        sshdServer.setPort(9876);

        // Host key
        final File hostKey = File.createTempFile("hostkey" + System.nanoTime(), ".ser");
        sshdServer.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(hostKey.getAbsolutePath()));

        // Use cmd on windows, /bin/sh otherwise
/*        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            sshdServer.setShellFactory(new ProcessShellFactory(new String[]{ "cmd" }));
        } else {
            sshdServer.setShellFactory(new ProcessShellFactory(new String[]{ "/bin/sh", "-i", "-l" }));
        }
  */
        // Mock several additional configurations
        final SShMockConfig mock = new SShMockConfig();
        sshdServer.setCommandFactory(mock);
        sshdServer.setPasswordAuthenticator(mock);
        sshdServer.setPublickeyAuthenticator(mock);

        /*List<NamedFactory<UserAuth>> userAuthFactories = new ArrayList<NamedFactory<UserAuth>>();
        userAuthFactories.add(new UserAuthNone.Factory());
        sshd.setUserAuthFactories(userAuthFactories);
        sshd.setPublickeyAuthenticator(new PublickeyAuthenticator());


        sshdServer.setCommandFactory(new ScpCommandFactory());
         */
        List<NamedFactory<Command>> namedFactoryList = new ArrayList<NamedFactory<Command>>();
        namedFactoryList.add(new SftpSubsystem.Factory());
        sshdServer.setSubsystemFactories(namedFactoryList);

        // Run server
        sshdServer.start();

        // Copy nuts
        final String basePath = SshTest.class.getResource("/chosen.css").getFile();
        System.setProperty(ApplicationConfig.BASE_PATH, basePath.substring(0, basePath.lastIndexOf("/")));
    }

    /**
     * <p>
     * Stops the server
     * </p>
     *
     * @throws InterruptedException if the thread in charge of stopping the service is interrupted
     */
    @AfterClass
    public static void tearDown() throws InterruptedException {
        sshdServer.stop();
    }

    /**
     * <p>
     * Tests the SSH access nuts with an embedded server.
     * </p>
     *
     * @throws JSchException if SSH session could not be opened
     * @throws WuicException if WUIC request fails
     * @throws InterruptedException if the SSH server does not respond in time
     * @throws IOException if any I/O error occurs
     * @throws JAXBException if test fails
     */
    @Test
    public void sshTest() throws JSchException, IOException, InterruptedException, WuicException, JAXBException {
        final List<ConvertibleNut> group = new WuicFacadeBuilder()
                .contextBuilderConfigurators(new FileXmlContextBuilderConfigurator(getClass().getResource("/wuic.xml")))
                .build()
                .runWorkflow("css-imagecss-image", ProcessContext.DEFAULT);

        Assert.assertFalse(group.isEmpty());

        for (final Nut res : group) {
            Assert.assertTrue(res.openStream().execution().toString().length() > 0);
        }
    }
}
