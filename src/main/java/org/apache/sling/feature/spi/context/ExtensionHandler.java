package org.apache.sling.feature.spi.context;

import org.apache.sling.feature.Extension;
import org.apache.sling.feature.Feature;

public interface ExtensionHandler {
    public boolean handle(ExtensionHandlerContext context, Extension extension, Feature feature) throws Exception;
}
