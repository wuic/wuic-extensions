/*
 * "Copyright (c) 2013   Capgemini Technology Services (hereinafter "Capgemini")
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


package com.github.wuic.engine.impl.embedded;

import com.github.wuic.resource.WuicResource;
import com.github.wuic.configuration.BadConfigurationException;
import com.github.wuic.configuration.Configuration;
import com.github.wuic.configuration.SpriteConfiguration;
import com.github.wuic.engine.EngineRequest;
import com.github.wuic.engine.PackerEngine;
import com.github.wuic.engine.Region;
import com.github.wuic.engine.SpriteProvider;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.imageio.ImageIO;

/**
 * <p>
 * This engine generates a sprite for different images. The generated sprite
 * will concern only one image if the engine is configured to aggregate files.
 * </p>
 * 
 * @author Guillaume DROUET
 * @version 1.3
 * @since 0.2.0
 */
public class CGSpriteAggregatorEngine extends PackerEngine {

    /**
     * Sprite provider.
     */
    private SpriteProvider spriteProvider;
    
    /**
     * The configuration.
     */
    private SpriteConfiguration configuration;

    /**
     * The aggregator which merges the images.
     */
    private CGImageAggregatorEngine aggregatorEngine;
    
    /**
     * <p>
     * Builds a new aggregator engine.
     * </p>
     * 
     * @param config the configuration
     * @throws BadConfigurationException if a bad configuration is detected
     */
    public CGSpriteAggregatorEngine(final Configuration config)
            throws BadConfigurationException {
        if (config instanceof SpriteConfiguration) {
            configuration = (SpriteConfiguration) config;
            spriteProvider = configuration.createSpriteProvider();
            aggregatorEngine = new CGImageAggregatorEngine(config);
            setDimensionPacker(configuration.createDimensionPacker());
        } else {
            final String message = config + " must be an instance of " + SpriteConfiguration.class.getName();
            throw new BadConfigurationException(message);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public List<WuicResource> parse(final EngineRequest request) throws IOException {

        // Generate the sprite file with the URL of the final image
        final StringBuilder url = new StringBuilder(request.getContextPath());
        url.append(request.getGroup().getId());
        
        /*
         * Create a resource for each image if the configuration says that no
         * aggregation should be done
         */
        if (!works()) {
            final List<WuicResource> retval = new ArrayList<WuicResource>();
            
            // Calculate type and dimensions of the final image
            for (WuicResource file : request.getResources()) {
                // Clear previous work
                spriteProvider.init(file.getName());
                
                InputStream is = null;
                
                try {
                    is = file.openStream();
                    
                    final BufferedImage buff = ImageIO.read(is);
                    spriteProvider.addRegion(new Region(0, 0, buff.getWidth() - 1, buff.getHeight() - 1), file.getName());
                    
                    final WuicResource resource = spriteProvider.getSprite(url.toString(), request.getGroup().getId());
                    resource.addReferencedResource(file);
                    retval.add(resource);
                } finally {
                    if (is != null) {
                        is.close();
                    }
                }
            }
            
            return retval;
        } else {
            // Clear previous work
            spriteProvider.init(CGImageAggregatorEngine.AGGREGATION_NAME);
    
            final Map<Region, WuicResource> packed = pack(request.getResources());
            
            for (Entry<Region, WuicResource> result : packed.entrySet()) {
                spriteProvider.addRegion(result.getKey(), result.getValue().getName());
            }


            final WuicResource retval = spriteProvider.getSprite(url.toString(), request.getGroup().getId());
            final List<WuicResource> image = aggregatorEngine.parse(request);

            if (image.size() != 1) {
                throw new IllegalStateException("One aggregated image is expected when aggregator engine is called. Actual : " + image.size());
            }

            retval.addReferencedResource(image.get(0));

            return Arrays.asList(retval);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Configuration getConfiguration() {
        return configuration;
    }

    @Override
    public Boolean works() {
        return configuration.aggregate();
    }

}
