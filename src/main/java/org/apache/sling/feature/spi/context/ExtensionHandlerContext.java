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
package org.apache.sling.feature.spi.context;

import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.builder.ArtifactProvider;

import java.net.URL;
import java.util.Dictionary;
import java.util.Map;

/**
 * This context is provided with calls to {@link ExtensionHandler} services.
 */
public interface ExtensionHandlerContext {
    /**
     * Add a bundle to be installed by the launcher.
     * @param id The bundle's artifact ID
     * @param startLevel The start level for the bundle.
     * @param file The file with the bundle.
     */
    public void addBundle(ArtifactId id, URL file, Integer startLevel);

    /**
     * Add an artifact to be installed by the launcher
     * @param id The artifact's ID
     * @param url The url to the Artifact resource
     * @param props Additional installation metadata
     */
    public void addInstallableArtifact(ArtifactId id, final URL url, final Map<String,Object> props);

    /**
     * Add a configuration to be installed by the launcher
     * @param pid The pid
     * @param factoryPid The factory pid
     * @param properties The propertis
     */
    public void addConfiguration(final String pid, final String factoryPid, final Dictionary<String, Object> properties);

    /**
     * Obtain the artifact provider.
     * @return The artifact provider.
     */
    public ArtifactProvider getArtifactProvider();
}
