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

import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Extension;
import org.apache.sling.feature.ExtensionState;
import org.apache.sling.feature.ExtensionType;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.spi.context.ExtensionHandlerContext;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.Constants;

import java.util.Dictionary;
import java.util.Hashtable;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RepoinitExtensionHandlerTest {
    @Test
    public void testExtensionHandler() throws Exception {
        RepoinitExtensionHandler rieh = new RepoinitExtensionHandler();

        ExtensionHandlerContext ctx = Mockito.mock(ExtensionHandlerContext.class);
        Extension ex = new Extension(ExtensionType.TEXT,
                Extension.EXTENSION_NAME_REPOINIT, ExtensionState.REQUIRED);
        ex.setText("hello 123");

        Feature feat = new Feature(ArtifactId.fromMvnId("a:b:1"));

        assertTrue(rieh.handle(ctx, ex, feat));

        Dictionary<String,Object> props = new Hashtable<>();
        props.put("scripts", "hello 123");
        props.put(Constants.SERVICE_RANKING, 200);

        Mockito.verify(ctx).addConfiguration(null, "org.apache.sling.jcr.repoinit.RepositoryInitializer~b_1.jar", props);
    }

    @Test
    public void testSkipUnrelatedExtensions() throws Exception {
        RepoinitExtensionHandler rieh = new RepoinitExtensionHandler();

        ExtensionHandlerContext ctx = Mockito.mock(ExtensionHandlerContext.class);
        assertFalse(rieh.handle(ctx, new Extension(ExtensionType.TEXT,
                "someother-extension", ExtensionState.REQUIRED), null));
        assertFalse(rieh.handle(ctx, new Extension(ExtensionType.JSON,
                Extension.EXTENSION_NAME_REPOINIT, ExtensionState.REQUIRED), null));
    }
}
