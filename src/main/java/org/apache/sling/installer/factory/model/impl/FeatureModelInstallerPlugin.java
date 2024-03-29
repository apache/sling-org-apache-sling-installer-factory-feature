/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.installer.factory.model.impl;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.sling.feature.Feature;
import org.apache.sling.feature.builder.BuilderContext;
import org.apache.sling.feature.builder.FeatureBuilder;
import org.apache.sling.feature.io.archive.ArchiveReader;
import org.apache.sling.feature.io.artifacts.ArtifactManager;
import org.apache.sling.feature.io.artifacts.ArtifactManagerConfig;
import org.apache.sling.feature.io.json.FeatureJSONReader;
import org.apache.sling.feature.io.json.FeatureJSONWriter;
import org.apache.sling.feature.spi.context.ExtensionHandler;
import org.apache.sling.installer.api.InstallableResource;
import org.apache.sling.installer.api.tasks.InstallTask;
import org.apache.sling.installer.api.tasks.InstallTaskFactory;
import org.apache.sling.installer.api.tasks.RegisteredResource;
import org.apache.sling.installer.api.tasks.ResourceState;
import org.apache.sling.installer.api.tasks.ResourceTransformer;
import org.apache.sling.installer.api.tasks.TaskResource;
import org.apache.sling.installer.api.tasks.TaskResourceGroup;
import org.apache.sling.installer.api.tasks.TransformationResult;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This task factory processes model resources detected by
 * the {@link FeatureModelTransformer}.
 */
@Component(service = { InstallTaskFactory.class, ResourceTransformer.class })
@Designate(ocd = FeatureModelInstallerPlugin.Config.class)
public class FeatureModelInstallerPlugin implements InstallTaskFactory, ResourceTransformer {

    @ObjectClassDefinition(name = "Apache Sling Feature Model Installer",
            description = "This component provides support for feature models to the OSGi installer")
    public @interface Config {

        @AttributeDefinition(name = "Use Apache Maven",
                description = "If enabled, missing artifacts from a feature are tried by invoking the mvn command")
        boolean useMvn() default true;

        @AttributeDefinition(name = "Repository URLs", description = "Additional repository URLs to fetch artifacts")
        String[] repositories();

        @AttributeDefinition(name = "Classifier Patterns", description = "Patterns for selecting the features to handle based on the classifier. Without a configuration all features are handled."
                + " The patterns can use an asteriks to match any characters in the classifier. The special token ':' can be used to match the empty classifier.")
        String[] classifierPatterns();
    }

    public static final String FILE_EXTENSION = ".json";

    public static final String TYPE_FEATURE_MODEL = "featuremodel";

    public static final String ATTR_MODEL = "feature";

    public static final String ATTR_ID = "featureId";

    public static final String ATTR_IS_FAR = "isFeatureArchive";

    /** Logger. */
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final BundleContext bundleContext;

    private final ArtifactManager artifactManager;

    private final List<Pattern> classifierPatterns = new ArrayList<>();

    private final File storageDirectory;

    @Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    private volatile List<ExtensionHandler> extensionHandlers;

    @Activate
    public FeatureModelInstallerPlugin(final BundleContext ctx, final Config config) throws IOException {
        this.bundleContext = ctx;
        this.storageDirectory = this.bundleContext.getDataFile("repository");
        final ArtifactManagerConfig amCfg = new ArtifactManagerConfig();
        amCfg.setUseMvn(config.useMvn());
        if (this.storageDirectory != null) {
            amCfg.setCacheDirectory(storageDirectory);
        }
        final List<String> repos = new ArrayList<>(Arrays.asList(amCfg.getRepositoryUrls()));
        if (config.repositories() != null && config.repositories().length > 0) {
            for (final String r : config.repositories()) {
                if (!r.trim().isEmpty()) {
                    repos.add(r);
                }
            }
        }
        amCfg.setRepositoryUrls(repos.toArray(new String[repos.size()]));

        this.artifactManager = ArtifactManager.getArtifactManager(amCfg);
        if (config.classifierPatterns() != null) {
            for (final String text : config.classifierPatterns()) {
                if (text != null && !text.trim().isEmpty()) {
                    if (":".equals(text.trim())) {
                        classifierPatterns.add(Pattern.compile("^$"));
                    } else {
                        classifierPatterns.add(Pattern.compile(toRegexPattern(text.trim())));
                    }
                }
            }
        }
    }

    /**
     * Check if the resource is a feature file or feature archive
     * @param resource The resource
     * @return The list of features
     */
    private List<Feature> getFeatures(final RegisteredResource resource) {
        final List<Feature> features = new ArrayList<>();
        if (resource.getType().equals(InstallableResource.TYPE_FILE) && resource.getURL().endsWith(FILE_EXTENSION)) {
            try (final Reader reader = new InputStreamReader(resource.getInputStream(), "UTF-8")) {
                features.add(FeatureJSONReader.read(reader, resource.getURL()));
            } catch (final IOException ioe) {
                logger.info("Unable to read feature model from " + resource.getURL(), ioe);
            }
        } else if (resource.getType().equals(InstallableResource.TYPE_FILE) && resource.getURL().endsWith(".far")) {
            try (final InputStream is = resource.getInputStream()) {
                features.addAll(ArchiveReader.read(is, null));
            } catch (final IOException ioe) {
                logger.info("Unable to read feature model from " + resource.getURL(), ioe);
            }
        }
        return features;
    }

    @Override
    public TransformationResult[] transform(final RegisteredResource resource) {
        final boolean isFeatureArchive = resource.getURL().endsWith(".far");
        final List<Feature> features = getFeatures(resource);
        if (!features.isEmpty()) {
            this.persistFeatures(features);

            boolean error = false;
            final List<TransformationResult> result = new ArrayList<>();
            for (Feature feature : features) {
                if (!classifierMatches(feature.getId().getClassifier())) {
                    continue;
                }

                final String featureJson = this.getFeatureJSON(feature);
                if (featureJson != null) {
                    final TransformationResult tr = new TransformationResult();
                    tr.setResourceType(TYPE_FEATURE_MODEL);
                    tr.setId(feature.getId().toMvnId());
                    tr.setVersion(feature.getId().getOSGiVersion());

                    final Map<String, Object> attributes = new HashMap<>();
                    attributes.put(ATTR_MODEL, featureJson);
                    attributes.put(ATTR_ID, feature.getId().toMvnId());
                    attributes.put(ATTR_IS_FAR, isFeatureArchive);

                    tr.setAttributes(attributes);

                    result.add(tr);
                } else {
                    error = true;
                    break;
                }
            }
            if (!error) {
                return result.toArray(new TransformationResult[result.size()]);
            }
        }
        return null;
    }

    @Override
    public InstallTask createTask(final TaskResourceGroup group) {
        final TaskResource rsrc = group.getActiveResource();
        if (!TYPE_FEATURE_MODEL.equals(rsrc.getType())) {
            return null;
        }
        if (rsrc.getState() == ResourceState.UNINSTALL ) {
            return new UninstallFeatureModelTask(group, bundleContext);
        }
        final InstallContext ctx = new InstallContext(this.artifactManager, this.storageDirectory);
        return new InstallFeatureModelTask(group,
                ctx, this.bundleContext, this.extensionHandlers);
    }

    boolean classifierMatches(String classifier) {
        boolean select = this.classifierPatterns.isEmpty();
        if (!select) {
            if (classifier == null) {
                classifier = "";
            }
            for (final Pattern pattern : this.classifierPatterns) {
                select = pattern.matcher(classifier).matches();

                if (select) {
                    break;
                }
            }
        }
        return select;
    }

    /**
     * Persist all features in the file system (if storage dir is specified)
     * @param features The list of features
     */
    private void persistFeatures(final List<Feature> features) {
        if (this.storageDirectory != null) {
            for (final Feature feature : features) {
                final File featureFile = new File(this.storageDirectory, feature.getId().toMvnPath().replace('/', File.separatorChar));
                if (!featureFile.exists()) {
                    featureFile.getParentFile().mkdirs();
                    try (final Writer writer = new FileWriter(featureFile)) {
                        FeatureJSONWriter.write(writer, feature);
                    } catch (final IOException ioe) {
                        logger.error("Unable to write feature to " + featureFile + ":" + ioe.getMessage(), ioe);
                    }
                }
            }
        }
    }

    /**
     * Get the feature json.
     * Assemble feature and resolve variables
     * @param feature The feature
     * @return The json string or {@code null}
     */
    private String getFeatureJSON(Feature feature) {
        // assemble feature now
        if (!feature.isAssembled()) {
            final BuilderContext ctx = new BuilderContext(this.artifactManager.toFeatureProvider());
            ctx.setArtifactProvider(this.artifactManager);

            // Set all merge extensions here from the service registry?
            feature = FeatureBuilder.assemble(feature, ctx);
        }

        FeatureBuilder.resolveVariables(feature, null);

        String featureJson = null;
        try (final StringWriter sw = new StringWriter()) {
            FeatureJSONWriter.write(sw, feature);
            featureJson = sw.toString();
        } catch (final IOException ioe) {
            logger.info("Unable to process feature model " + feature.getId().toMvnId(), ioe);
        }

        return featureJson;
    }

    private static String toRegexPattern(String pattern) {
        StringBuilder stringBuilder = new StringBuilder("^");
        int index = 0;
        while (index < pattern.length()) {
            char currentChar = pattern.charAt(index++);
            switch (currentChar) {
            case '*':
                stringBuilder.append("[^/]*");
                break;
            default:
                if (isRegexMeta(currentChar)) {
                    stringBuilder.append(Pattern.quote(Character.toString(currentChar)));
                } else {
                    stringBuilder.append(currentChar);
                }
            }
        }
        return stringBuilder.append('$').toString();
    }

    private static boolean isRegexMeta(char character) {
        return "<([{\\^-=$!|]})?*+.>".indexOf(character) != -1;
    }
}
