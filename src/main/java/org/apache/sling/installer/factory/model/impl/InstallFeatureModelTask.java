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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Configuration;
import org.apache.sling.feature.Extension;
import org.apache.sling.feature.ExtensionType;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.builder.ArtifactProvider;
import org.apache.sling.feature.io.archive.ArchiveReader;
import org.apache.sling.feature.io.artifacts.ArtifactHandler;
import org.apache.sling.feature.io.json.FeatureJSONReader;
import org.apache.sling.feature.spi.context.ExtensionHandler;
import org.apache.sling.feature.spi.context.ExtensionHandlerContext;
import org.apache.sling.installer.api.InstallableResource;
import org.apache.sling.installer.api.OsgiInstaller;
import org.apache.sling.installer.api.tasks.InstallationContext;
import org.apache.sling.installer.api.tasks.ResourceState;
import org.apache.sling.installer.api.tasks.TaskResource;
import org.apache.sling.installer.api.tasks.TaskResourceGroup;
import org.osgi.framework.BundleContext;

/**
 * This task installs a feature model resources.
 */
public class InstallFeatureModelTask extends AbstractFeatureModelTask {
    private final InstallContext installContext;
    private final List<ExtensionHandler> extensionHandlers;

    public InstallFeatureModelTask(final TaskResourceGroup group,
            final InstallContext installContext, final BundleContext bundleContext,
            final List<ExtensionHandler> extensionHandlers) {
        super(group, bundleContext);
        this.installContext = installContext;
        this.extensionHandlers = extensionHandlers;
    }

    @Override
    public void execute(final InstallationContext ctx) {
        try {
            final TaskResource resource = this.getResource();
            ctx.log("Installing {}", resource.getEntityId());
            final String featureJson = (String) resource.getAttribute(FeatureModelInstallerPlugin.ATTR_MODEL);
            if (featureJson == null) {
                ctx.log("Unable to install feature model resource {} : no model found", resource);
                this.getResourceGroup().setFinishState(ResourceState.IGNORED, null, "No model found");
            } else {
                boolean success = false;
                final List<InstallableResource> result = this.transform(featureJson, resource);
                if (result == null) {
                    ctx.log("Unable to install feature model resource {} : unable to create resources", resource);
                    this.getResourceGroup().setFinishState(ResourceState.IGNORED, null, "Unable to create resources");
                } else {
                    if (!result.isEmpty()) {
                        final OsgiInstaller installer = this.getService(OsgiInstaller.class);
                        if (installer != null) {
                            installer.registerResources(
                                    getScheme(resource),
                                    result.toArray(new InstallableResource[result.size()]));
                        } else {
                            ctx.log("Unable to install feature model resource {} : unable to get OSGi installer",
                                    resource);
                            this.getResourceGroup().setFinishState(ResourceState.IGNORED, null,  "Unable to get OSGi installer");
                            return;
                        }
                    }
                    this.getResourceGroup().setFinishState(ResourceState.INSTALLED);
                    success = true;
                }
                if ( success ) {
                    ctx.log("Installed {}", resource.getEntityId());
                }
            }
        } finally {
            this.cleanup();
        }
    }

    private File getArtifactFile(final File baseDir, final ArtifactId id) {
        return new File(baseDir, id.toMvnPath().replace('/', File.separatorChar));
    }

    private boolean isFeatureArchive(final TaskResource rsrc) {
        final Object value = rsrc.getAttribute(FeatureModelInstallerPlugin.ATTR_IS_FAR);
        if ( value == null ) {
            return true;
        }
        return Boolean.valueOf(value.toString());
    }

    private List<InstallableResource> transform(final String featureJson,
            final TaskResource rsrc) {
        Feature feature = null;
        try (final Reader reader = new StringReader(featureJson)) {
            feature = FeatureJSONReader.read(reader, null);
        } catch ( final IOException ioe) {
            logger.warn("Unable to read feature model file", ioe);
        }
        if (feature == null) {
            return null;
        }

        final List<InstallableResource> result = new ArrayList<>();
        // configurations
        for (final Configuration cfg : feature.getConfigurations()) {
            result.add(new InstallableResource("/".concat(cfg.getPid()).concat(".config"), null,
                    cfg.getConfigurationProperties(), null, InstallableResource.TYPE_CONFIG, null));
        }

        // extract artifacts
        if (this.installContext.storageDirectory != null && isFeatureArchive(rsrc ) ) {
            final byte[] buffer = new byte[1024*1024*256];

            try ( final InputStream is = rsrc.getInputStream() ) {
                ArchiveReader.read(is, new ArchiveReader.ArtifactConsumer() {

                    @Override
                    public void consume(final ArtifactId id, final InputStream is) throws IOException {
                        final File artifactFile = getArtifactFile(installContext.storageDirectory, id);
                        if (!artifactFile.exists()) {
                            artifactFile.getParentFile().mkdirs();
                            try (final OutputStream os = new FileOutputStream(artifactFile)) {
                                int l = 0;
                                while ((l = is.read(buffer)) > 0) {
                                    os.write(buffer, 0, l);
                                }
                            }
                        }
                    }
                });
            } catch ( final IOException ioe) {
                logger.warn("Unable to extract artifacts from feature model " + feature.getId().toMvnId(), ioe);
                return null;
            }
        }

        ExtensionHandlerContext context = new ContextImpl(result);

        for (Extension ext : feature.getExtensions()) {
            boolean handlerFound = false;
            for (ExtensionHandler eh : extensionHandlers) {
                try {
                    handlerFound |= eh.handle(context, ext, feature);
                } catch (Exception e) {
                    logger.error("Exception while processing extension {} with handler {}", ext, eh, e);
                }
            }
            if (!handlerFound) {
                if (ExtensionType.ARTIFACTS == ext.getType()) {
                    // Unhandled ARTIFACTS extensions get stored
                    for (final Artifact artifact : ext.getArtifacts()) {
                        addArtifact(artifact, result);
                    }
                } else {
                    // should this be an error?
                    logger.warn("No extension handler found for mandartory extension " + ext);
                }
            }
        }

        // bundles
        for (final Artifact bundle : feature.getBundles()) {
            if (!addArtifact(bundle, result)) {
                return null;
            }
        }

        return result;
    }

    private boolean addArtifact(final Artifact artifact,
            final List<InstallableResource> result) {
        File artifactFile = (this.installContext.storageDirectory == null ? null
                : getArtifactFile(this.installContext.storageDirectory, artifact.getId()));
        ArtifactHandler handler;
        if (artifactFile == null || !artifactFile.exists()) {
            try {
                handler = this.installContext.artifactManager.getArtifactHandler(artifact.getId().toMvnUrl());
            } catch (final IOException ignore) {
                return false;
            }
        } else {
            try {
                handler = new ArtifactHandler(artifactFile);
            } catch (final MalformedURLException e) {
                return false;
            }
        }
        if (handler == null) {
            return false;
        }
        try {
            final URLConnection connection = handler.getLocalURL().openConnection();
            connection.connect();
            final InputStream is = connection.getInputStream();
            final long lastModified = connection.getLastModified();
            final String digest = lastModified == 0 ? null : String.valueOf(lastModified);
            // handle start order
            final Dictionary<String, Object> dict = new Hashtable<String, Object>();
            if (artifact.getStartOrder() > 0) {
                dict.put(InstallableResource.BUNDLE_START_LEVEL, artifact.getStartOrder());
            }
            dict.put(InstallableResource.RESOURCE_URI_HINT, handler.getLocalURL().toString());

            result.add(new InstallableResource("/".concat(artifact.getId().toMvnName()), is, dict, digest,
                    InstallableResource.TYPE_FILE, null));
        } catch (final IOException ioe) {
            logger.warn("Unable to read artifact " + handler.getLocalURL(), ioe);
            return false;
        }
        return true;
    }

    @Override
    public String getSortKey() {
        return "30-" + getResource().getAttribute(FeatureModelInstallerPlugin.ATTR_ID);
    }

    private ArtifactProvider getLocalArtifactProvider() {
        // TODO share with addArtifact()
        return new ArtifactProvider() {
            @Override
            public URL provide(ArtifactId id) {
                File artifactFile = (installContext.storageDirectory == null ? null
                        : getArtifactFile(installContext.storageDirectory, id));
                ArtifactHandler handler;
                if (artifactFile == null || !artifactFile.exists()) {
                    try {
                        handler = installContext.artifactManager.getArtifactHandler(id.toMvnUrl());
                    } catch (final IOException ignore) {
                        return null;
                    }
                } else {
                    try {
                        handler = new ArtifactHandler(artifactFile);
                    } catch (final MalformedURLException e) {
                        return null;
                    }
                }
                if (handler == null) {
                    return null;
                }
                return handler.getLocalURL();
            }
        };
    }

    private class ContextImpl implements ExtensionHandlerContext {
        private final List<InstallableResource> results;

        public ContextImpl(List<InstallableResource> results) {
            this.results = results;
        }

        @Override
        public void addBundle(ArtifactId id, URL file, Integer startLevel) {
            // TODO Auto-generated method stub
        }

        @Override
        public void addInstallableArtifact(ArtifactId id, URL url, Map<String,Object> props) {
            try {
                Dictionary <String,Object> dict = new Hashtable<>();
                props.entrySet().stream()
                    .filter(e -> e.getValue() != null)
                    .forEach(e -> dict.put(e.getKey(), e.getValue()));

                InputStream is = url.openStream();
                results.add(new InstallableResource("/".concat(id.toMvnName()), is, dict, null /* TODO digest? */,
                        InstallableResource.TYPE_FILE, null));
            } catch (IOException e) {
                logger.warn("Unable to read artifact " + id + " from url " + url, e);
            }
        }

        @Override
        public void addConfiguration(String pid, String factoryPid, Dictionary<String, Object> properties) {
            // TODO handler factoryPid, is this ok?
            String cfgPid = pid;
            if (factoryPid != null) {
                cfgPid = factoryPid;
            }

            results.add(new InstallableResource("/".concat(cfgPid).concat(".config"), null,
                    properties, null, InstallableResource.TYPE_CONFIG, null));
        }

        @Override
        public ArtifactProvider getArtifactProvider() {
            return getLocalArtifactProvider();
        }
    }
}
