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

import org.apache.sling.feature.Extension;
import org.apache.sling.feature.ExtensionType;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.spi.context.ExtensionHandler;
import org.apache.sling.feature.spi.context.ExtensionHandlerContext;
import org.apache.sling.installer.api.tasks.InstallationContext;
import org.apache.sling.installer.api.tasks.ResourceState;
import org.apache.sling.installer.api.tasks.TaskResource;
import org.apache.sling.installer.api.tasks.TaskResourceGroup;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class InstallFeatureModelTaskTest {
    private static String FEATURE_JSON_1 = "{" +
            "  \"id\":\"com.adobe.someproj:feature:7.0.126\"," +
//            "  \"bundles\": [" +
//            "    \"com.adobe.osgi.wrapper:com.adobe.osgi.wrapper.javassist:3.15.0-GA-wrapper.1\"" +
//            "  ]," +
            "  \"system-fonts:ARTIFACTS|true\": [" +
            "    \"com.adobe.aemfd.core:adobe-aemfd-core-fonts:jar:0.7.1\"" +
            "  ]," +
            "  \"blahblahblah:ARTIFACTS|true\": [" +
            "  ]," +
            "  \"framework-properties\": {" +
            "    \"foo\": \"bar\"" +
            "  }" +
            "}";

//    private Path tempDir;
//
//
//    @Before
//    public void setUp() throws IOException {
//        tempDir = Files.createTempDirectory(getClass().getSimpleName());
//    }
//
//    @After
//    public void tearDown() throws IOException {
//        // Delete the temp dir again
//        Files.walk(tempDir)
//            .sorted(Comparator.reverseOrder())
//            .map(Path::toFile)
//            .forEach(File::delete);
//    }

    @Test
    public void testTransform() {
        TaskResource resource = Mockito.mock(TaskResource.class);
        Mockito.when(resource.getAttribute(FeatureModelInstallerPlugin.ATTR_MODEL))
            .thenReturn(FEATURE_JSON_1);

        TaskResourceGroup group = Mockito.mock(TaskResourceGroup.class);
        Mockito.when(group.getActiveResource()).thenReturn(resource);

        InstallContext installContext = new InstallContext(null, null);
        BundleContext bundleContext = Mockito.mock(BundleContext.class);

        TestExtensionHandler testEH = new TestExtensionHandler("system-fonts", ExtensionType.ARTIFACTS);
        List<ExtensionHandler> extensionHandlers = Arrays.asList(testEH);

        InstallFeatureModelTask ifmt = new InstallFeatureModelTask(group, installContext,
                bundleContext, extensionHandlers );

        InstallationContext ctx = Mockito.mock(InstallationContext.class);

        assertEquals("Precondition", 0, testEH.handled.size());
        assertEquals("Precondition", 0, testEH.notHandled.size());

        ifmt.execute(ctx);

        assertEquals(1, testEH.handled.size());
        assertEquals("system-fonts", testEH.handled.get(0).getName());

        assertEquals(1, testEH.notHandled.size());
        assertEquals("blahblahblah", testEH.notHandled.get(0).getName());

        Mockito.verify(group).setFinishState(ResourceState.INSTALLED);
    }

    private static class TestExtensionHandler implements ExtensionHandler {
        private final String extensionName;
        private final ExtensionType extensionType;
        private final List<Extension> handled = new ArrayList<>();
        private final List<Extension> notHandled = new ArrayList<>();

        private TestExtensionHandler(String name, ExtensionType type) {
            extensionName = name;
            extensionType = type;
        }

        @Override
        public boolean handle(ExtensionHandlerContext context, Extension extension, Feature feature) throws Exception {
            if (extensionName.equals(extension.getName()) &&
                    extensionType == extension.getType()) {

                handled.add(extension);
                return true;
            }

            notHandled.add(extension);
            return false;
        }
    }
}
