package org.apache.sling.installer.factory.model.impl;

import org.apache.felix.utils.manifest.Clause;
import org.apache.felix.utils.manifest.Directive;
import org.apache.felix.utils.manifest.Parser;
import org.apache.sling.feature.Extension;
import org.apache.sling.feature.ExtensionType;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.spi.context.ExtensionHandler;
import org.apache.sling.feature.spi.context.ExtensionHandlerContext;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Component
public class BinaryArtifactExtensionHandler implements ExtensionHandler {
    private static final String BINARY_EXTENSIONS_PROP = "org.apache.sling.feature.binary.extensions";

    private final Map<String, Map<String, String>> binaryExtensions;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Activate
    public BinaryArtifactExtensionHandler(BundleContext bc) {
        Map<String, Map<String, String>> be = new HashMap<>();

        // Syntax: system-fonts;dir:=abc;overwrite:=true,customer-fonts;dir:=eft
        Clause[] extClauses = Parser.parseHeader(bc.getProperty(BINARY_EXTENSIONS_PROP));
        for (Clause c : extClauses) {
            Map<String,String> cfg = new HashMap<>();

            for (Directive d : c.getDirectives()) {
                cfg.put(d.getName(), d.getValue());
            }
            be.put(c.getName(), Collections.unmodifiableMap(cfg));
        }

        binaryExtensions = Collections.unmodifiableMap(be);
    }

    @Override
    public boolean handle(ExtensionHandlerContext context, Extension extension, Feature feature) throws Exception {
        if (extension.getType() != ExtensionType.ARTIFACTS ||
                binaryExtensions.get(extension.getName()) == null) {
            return false;
        } else {
            // Binary extension
            // TODO unzip

            return true;
        }
    }
}
