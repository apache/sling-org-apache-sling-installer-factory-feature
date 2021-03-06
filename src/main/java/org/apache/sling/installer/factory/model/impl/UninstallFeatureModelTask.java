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

import org.apache.sling.installer.api.OsgiInstaller;
import org.apache.sling.installer.api.tasks.InstallationContext;
import org.apache.sling.installer.api.tasks.ResourceState;
import org.apache.sling.installer.api.tasks.TaskResource;
import org.apache.sling.installer.api.tasks.TaskResourceGroup;
import org.osgi.framework.BundleContext;

/**
 * This task uninstalls feature model resources.
 */
public class UninstallFeatureModelTask extends AbstractFeatureModelTask {

    public UninstallFeatureModelTask(final TaskResourceGroup group,
            final BundleContext bundleContext) {
        super(group, bundleContext);
    }

    @Override
    public void execute(final InstallationContext ctx) {
        try {
            final OsgiInstaller installer = this.getService(OsgiInstaller.class);
            if ( installer == null ) {
                ctx.log("Unable to get OSGi Installer service!");
            } else {
                final TaskResource resource = this.getResource();
                ctx.log("Uninstalling {}", resource.getEntityId());
                installer.registerResources(getScheme(resource), null);
                this.getResourceGroup().setFinishState(ResourceState.UNINSTALLED);
                ctx.log("Uninstalled {}", resource.getEntityId());
            }
        } finally {
            this.cleanup();
        }
    }

    @Override
    public String getSortKey() {
        return "31-" + getResource().getAttribute(FeatureModelInstallerPlugin.ATTR_ID);
    }
}
