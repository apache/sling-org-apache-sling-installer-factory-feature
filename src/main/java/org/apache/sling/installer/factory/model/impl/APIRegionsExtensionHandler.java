package org.apache.sling.installer.factory.model.impl;

import org.apache.sling.feature.Extension;
import org.apache.sling.feature.ExtensionType;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.extension.apiregions.api.ApiRegions;
import org.apache.sling.feature.extension.apiregions.launcher.LauncherProperties;
import org.apache.sling.feature.spi.context.ExtensionHandler;
import org.apache.sling.feature.spi.context.ExtensionHandlerContext;
import org.osgi.service.component.annotations.Component;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@Component
public class APIRegionsExtensionHandler implements ExtensionHandler {
    private static final String REGION_FACTORY_PID = "org.apache.sling.feature.apiregions.factory~";

    private static final String PROP_idbsnver = "mapping.bundleid.bsnver";
    private static final String PROP_bundleFeatures = "mapping.bundleid.features";
    private static final String PROP_featureRegions = "mapping.featureid.regions";
    private static final String PROP_regionPackage = "mapping.region.packages";

    @Override
    public boolean handle(ExtensionHandlerContext context, Extension extension, Feature feature) throws Exception {
        if (!extension.getName().equals(ApiRegions.EXTENSION_NAME) ||
                extension.getType() != ExtensionType.JSON) {
            return false;
        }

        final ApiRegions regions = ApiRegions.parse(extension.getJSONStructure().asJsonArray());

        final String configPid = REGION_FACTORY_PID.concat(feature.getId().toMvnName().replace('-', '_'));
        final Dictionary<String, Object> props = new Hashtable<>();
        props.put(PROP_idbsnver, convert(LauncherProperties.getBundleIDtoBSNandVersionMap(feature, context.getArtifactManager())));
        props.put(PROP_bundleFeatures, convert(LauncherProperties.getBundleIDtoFeaturesMap(feature)));
        props.put(PROP_featureRegions, convert(LauncherProperties.getFeatureIDtoRegionsMap(regions)));
        props.put(PROP_regionPackage, convert(LauncherProperties.getRegionNametoPackagesMap(regions)));

        context.addConfiguration(null, configPid, props);
        return true;
    }

    private String[] convert(final Properties props) {
        final List<String> result = new ArrayList<>();

        for(final Map.Entry<Object, Object> entry : props.entrySet()) {
            result.add(entry.getKey().toString().concat("=").concat(entry.getValue().toString()));
        }
        return result.toArray(new String[result.size()]);
    }
}
