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


package com.github.wuic.engine.hazelcast;

import com.hazelcast.config.ClasspathXmlConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;

import java.io.InputStream;

/**
 * <p>
 * This class is used by default to retrieve the cache for WUIC from Hazelcast framework. To perform it, a
 * new {@code HazelcastInstance} is created with the hazelcast.xml path located at the root of the 'classpath'.
 * If no XML file is found, a default instance is created. When the {@code HazelcastInstance} is created, a
 * {@code IMap} named 'wuiCache' is returned.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.3
 */
public class DefaultHazelcastProvider implements WuicHazelcastProvider {

    /**
     * {@inheritDoc}
     */
    @Override
    public IMap getCache() {
        final InputStream is = DefaultHazelcastProvider.class.getResourceAsStream("/hazelcast.xml");
        final HazelcastInstance instance = is == null
                ? Hazelcast.newHazelcastInstance() : Hazelcast.newHazelcastInstance(new ClasspathXmlConfig("hazelcast.xml"));

        return instance.getMap("wuicCache");
    }
}
