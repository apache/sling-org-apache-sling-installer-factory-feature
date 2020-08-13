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

import org.apache.sling.installer.api.tasks.InstallTask;
import org.apache.sling.installer.api.tasks.InstallTaskFactory;
import org.apache.sling.installer.api.tasks.RegisteredResource;
import org.apache.sling.installer.api.tasks.ResourceTransformer;
import org.apache.sling.installer.api.tasks.TaskResourceGroup;
import org.apache.sling.installer.api.tasks.TransformationResult;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@Component(service = { InstallTaskFactory.class, ResourceTransformer.class })
@Designate(ocd = BinaryPackageInstallerPlugin.Config.class)
public class BinaryPackageInstallerPlugin implements InstallTaskFactory, ResourceTransformer {

    @ObjectClassDefinition(name = "Binary Package Installer",
            description = "This component supports installing binary packages into the OSGi installer")
    public @interface Config {
        String directory();

        String overwrite() default "true";
    }

    @Override
    public TransformationResult[] transform(RegisteredResource resource) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public InstallTask createTask(TaskResourceGroup group) {
        // TODO Auto-generated method stub
        return null;
    }

}
