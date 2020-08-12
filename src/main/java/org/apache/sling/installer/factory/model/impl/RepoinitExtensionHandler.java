package org.apache.sling.installer.factory.model.impl;

import org.apache.sling.feature.Extension;
import org.apache.sling.feature.ExtensionType;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.spi.context.ExtensionHandler;
import org.apache.sling.feature.spi.context.ExtensionHandlerContext;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;

import java.util.Dictionary;
import java.util.Hashtable;

@Component
public class RepoinitExtensionHandler implements ExtensionHandler {
    private static final String REPOINIT_FACTORY_PID = "org.apache.sling.jcr.repoinit.RepositoryInitializer~";

    @Override
    public boolean handle(ExtensionHandlerContext context, Extension extension, Feature feature) throws Exception {
        if (!extension.getName().equals(Extension.EXTENSION_NAME_REPOINIT) ||
                extension.getType() != ExtensionType.TEXT) {
            return false;
        }

        final String configPid = REPOINIT_FACTORY_PID.concat(feature.getId().toMvnName().replace('-', '_'));
        final Dictionary<String, Object> props = new Hashtable<>();
        props.put("scripts", extension.getText());
        props.put(Constants.SERVICE_RANKING, 200);

        context.addConfiguration(null, configPid, props);
        return true;
    }
}
