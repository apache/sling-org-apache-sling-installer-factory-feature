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

import org.apache.sling.feature.io.artifacts.ArtifactManager;

import java.net.URL;
import java.util.Dictionary;

public interface ExtensionHandlerContext {
    /**
     * Add a bundle to be installed by the launcher.
     * @param startLevel The start level for the bundle.
     * @param file The file with the bundle.
     */
    public void addBundle(final Integer startLevel, final URL file);

    /**
     * Add an artifact to be installed by the launcher
     * @param file The file
     */
    public void addInstallableArtifact(final URL file);

    /**
     * Add a configuration to be installed by the launcher
     * @param pid The pid
     * @param factoryPid The factory pid
     * @param properties The propertis
     */
    public void addConfiguration(final String pid, final String factoryPid, final Dictionary<String, Object> properties);

    /**
     * Obtain the artifact manager.
     * @return The artifact manager to use.
     */
    public ArtifactManager getArtifactManager();
}
