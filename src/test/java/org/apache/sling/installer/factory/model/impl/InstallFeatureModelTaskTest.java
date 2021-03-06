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
import org.apache.sling.feature.io.artifacts.ArtifactManager;
import org.apache.sling.feature.spi.context.ExtensionHandler;
import org.apache.sling.feature.spi.context.ExtensionHandlerContext;
import org.apache.sling.installer.api.tasks.InstallationContext;
import org.apache.sling.installer.api.tasks.ResourceState;
import org.apache.sling.installer.api.tasks.TaskResource;
import org.apache.sling.installer.api.tasks.TaskResourceGroup;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class InstallFeatureModelTaskTest {
    private static String FEATURE_JSON_1 = "{" +
            "  \"id\":\"org.apache.sling.someproj:feature:7.0.126\"," +
            "  \"system-fonts:ARTIFACTS|true\": [" +
            "    \"org.apache.sling.feature:my-fonts:jar:0.7.1\"" +
            "  ]," +
            "  \"blahblahblah:ARTIFACTS|true\": [" +
            "  ]," +
            "  \"framework-properties\": {" +
            "    \"foo\": \"bar\"" + "  }" +
            "}";


    private Path tempDir;

    @Before
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory(getClass().getSimpleName());
    }

    @After
    public void tearDown() throws IOException {
        // Delete the temp dir again
        Files.walk(tempDir).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
    }

    @Test
    public void testTransform() {
        TaskResource resource = Mockito.mock(TaskResource.class);
        Mockito.when(resource.getAttribute(FeatureModelInstallerPlugin.ATTR_MODEL)).thenReturn(FEATURE_JSON_1);

        TaskResourceGroup group = Mockito.mock(TaskResourceGroup.class);
        Mockito.when(group.getActiveResource()).thenReturn(resource);

        InstallContext installContext = new InstallContext(null, null);
        BundleContext bundleContext = Mockito.mock(BundleContext.class);

        TestExtensionHandler testEH = new TestExtensionHandler("system-fonts", ExtensionType.ARTIFACTS);
        List<ExtensionHandler> extensionHandlers = Arrays.asList(testEH);

        InstallFeatureModelTask ifmt = new InstallFeatureModelTask(group, installContext, bundleContext, extensionHandlers);

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

    @Test
    public void testTransform2() throws IOException {
        URL fmRes = getClass().getResource("/test2/test2.slingosgifeature");
        URL farRes = getClass().getResource("/test2/test2.far");

        String fm = new BufferedReader(new InputStreamReader(fmRes.openStream()))
                .lines().collect(Collectors.joining("\n"));

        TaskResource resource = Mockito.mock(TaskResource.class);
        Mockito.when(resource.getAttribute(FeatureModelInstallerPlugin.ATTR_MODEL))
            .thenReturn(fm);
        Mockito.when(resource.getInputStream())
            .thenReturn(farRes.openStream());

        TaskResourceGroup group = Mockito.mock(TaskResourceGroup.class);
        Mockito.when(group.getActiveResource()).thenReturn(resource);

        ArtifactManager am = Mockito.mock(ArtifactManager.class);
        InstallContext installContext = new InstallContext(am, tempDir.toFile());
        BundleContext bundleContext = Mockito.mock(BundleContext.class);

        List<ExtensionHandler> extensionHandlers = Arrays.asList();

        InstallFeatureModelTask ifmt = new InstallFeatureModelTask(group, installContext,
                bundleContext, extensionHandlers );

        InstallationContext ctx = Mockito.mock(InstallationContext.class);

        ifmt.execute(ctx);

        Path actualJar = tempDir.resolve(
                "org/apache/felix/org.apache.felix.converter/1.0.14/org.apache.felix.converter-1.0.14.jar");
        assertTrue(Files.exists(actualJar));
        assertEquals(116605, Files.size(actualJar));

        Path actualFeatureModel = tempDir.resolve(
                "org/apache/sling/someproj/feature/3.0.2/feature-3.0.2.slingosgifeature");
        assertTrue(Files.exists(actualFeatureModel));

        String expectedFM = new String(Files.readAllBytes(new File(fmRes.getFile()).toPath()));
        assertEquals(expectedFM, new String(Files.readAllBytes(actualFeatureModel)));
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
            if (extensionName.equals(extension.getName()) && extensionType == extension.getType()) {

                handled.add(extension);
                return true;
            }

            notHandled.add(extension);
            return false;
        }
    }
}
