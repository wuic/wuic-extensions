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

import org.apache.sshd.server.Command;
import org.apache.sshd.server.CommandFactory;
import org.apache.sshd.server.PasswordAuthenticator;
import org.apache.sshd.server.PublickeyAuthenticator;
import org.apache.sshd.server.command.ScpCommandFactory;
import org.apache.sshd.server.command.UnknownCommand;
import org.apache.sshd.server.session.ServerSession;

import java.io.File;
import java.security.PublicKey;

/**
 * <p>
 * Class used in unit tests as a mock for {@code PasswordAuthenticator}, {@code PublickeyAuthenticator}
 * and {@code CommandFactory}.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.3.1
 */
public class SShMockConfig implements CommandFactory, PasswordAuthenticator, PublickeyAuthenticator {

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean authenticate(final String s, final PublicKey publicKey, final ServerSession serverSession) {
        return Boolean.TRUE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean authenticate(final String s, final String s2, final ServerSession serverSession) {
        return Boolean.TRUE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Command createCommand(final String s) {
        return new ScpCommandFactory().createCommand(s);
    }
}
