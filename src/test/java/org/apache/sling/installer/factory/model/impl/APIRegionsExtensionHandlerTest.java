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

import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Extension;
import org.apache.sling.feature.ExtensionState;
import org.apache.sling.feature.ExtensionType;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.builder.ArtifactProvider;
import org.apache.sling.feature.spi.context.ExtensionHandlerContext;
import org.junit.Test;
import org.mockito.Mockito;

import java.net.URL;
import java.util.Arrays;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class APIRegionsExtensionHandlerTest {
    private static final String FEATURE_EXT_1 =
            "[" +
            "   {" +
            "     \"name\": \"my-region\"," +
            "     \"exports\": [\"org.foo.bar\", \"la.di.da\"]" +
            "   }" +
            "]";
    private static final String FEATURE_EXT_NOOP = "[]";
    private static final URL BUNDLE_WITH_ORIGIN_RESOURCE = APIRegionsExtensionHandlerTest.class
        .getResource("/APIRegionsExtensionHandlerTest/bundle-with-origin-1.jar");
    private static final URL BUNDLE_WITHOUT_ORIGIN_RESOURCE = APIRegionsExtensionHandlerTest.class
        .getResource("/APIRegionsExtensionHandlerTest/bundle-without-origin-1.jar");

    @Test
    public void testHandle() throws Exception {
        APIRegionsExtensionHandler areh = new APIRegionsExtensionHandler();

        ExtensionHandlerContext ctx = Mockito.mock(ExtensionHandlerContext.class);

        Extension ext = new Extension(ExtensionType.JSON, "api-regions", ExtensionState.REQUIRED);
        ext.setJSON(FEATURE_EXT_1);

        Feature feat = new Feature(ArtifactId.fromMvnId("x:y:8"));
        assertTrue(areh.handle(ctx, ext, feat));

        Mockito.verify(ctx).addConfiguration(Mockito.isNull(),
            Mockito.eq("org.apache.sling.feature.apiregions.factory~y_8.jar"),
            Mockito.argThat(p -> {
                String[] pkgs = (String[]) p.get("mapping.region.packages");
                return Arrays.deepEquals(new String [] {"my-region=org.foo.bar,la.di.da"}, pkgs);
            }));
    }

    @Test
    public void testDoesNotHandle() throws Exception {
        APIRegionsExtensionHandler areh = new APIRegionsExtensionHandler();

        Extension ext = new Extension(ExtensionType.TEXT, "api-regions", ExtensionState.REQUIRED);
        assertFalse(areh.handle(null, ext, null));
    }

    @Test
    public void testBundleToFeatureMappingWithAndWithoutFeatureOrigin() throws Exception {
        APIRegionsExtensionHandler areh = new APIRegionsExtensionHandler();
        // define a bundle artifact that has a feature-origin set
        ArtifactId bundleWithOriginId = ArtifactId.fromMvnId("bundle:with-origin:1");
        Artifact artifactWithOrigin = new Artifact(bundleWithOriginId);
        artifactWithOrigin.setFeatureOrigins(ArtifactId.fromMvnId("my:origin:1"));
        // define another bundle artifact that does not has a feature-origin set
        ArtifactId bundleWithoutOriginId = ArtifactId.fromMvnId("bundle:without-origin:1");
        Artifact artifactWithoutOrigin = new Artifact(bundleWithoutOriginId);

        ExtensionHandlerContext ctx = Mockito.mock(ExtensionHandlerContext.class);
        ArtifactProvider artifactProvider = Mockito.mock(ArtifactProvider.class);
        Mockito.when(ctx.getArtifactProvider()).thenReturn(artifactProvider);
        Mockito.when(artifactProvider.provide(bundleWithOriginId)).thenReturn(BUNDLE_WITH_ORIGIN_RESOURCE);
        Mockito.when(artifactProvider.provide(bundleWithoutOriginId)).thenReturn(BUNDLE_WITHOUT_ORIGIN_RESOURCE);

        Extension ext = new Extension(ExtensionType.JSON, "api-regions", ExtensionState.REQUIRED);
        ext.setJSON(FEATURE_EXT_NOOP);

        Feature feat = new Feature(ArtifactId.fromMvnId("x:y:8"));
        feat.getBundles().add(artifactWithOrigin);
        feat.getBundles().add(artifactWithoutOrigin);

        assertTrue(areh.handle(ctx, ext, feat));

        Mockito.verify(ctx).addConfiguration(Mockito.isNull(),
            Mockito.eq("org.apache.sling.feature.apiregions.factory~y_8.jar"),
            Mockito.argThat(p -> {
                String[] pkgs = (String[]) p.get("mapping.bundleid.features");
                return Arrays.deepEquals(new String [] {
                    "bundle:with-origin:1=my:origin:1",
                    "bundle:without-origin:1=x:y:8",
                }, pkgs);
            }));
    }
}
