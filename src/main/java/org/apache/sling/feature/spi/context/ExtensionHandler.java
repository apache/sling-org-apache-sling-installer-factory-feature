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

import org.apache.sling.feature.Extension;
import org.apache.sling.feature.Feature;

/**
 * Service interface to handle feature model extensions in the feature installer
 */
public interface ExtensionHandler {
    /**
     * Called on registered services when an extension is encountered
     * @param context The extension context
     * @param extension The extension to be handled
     * @param feature The feature that contains the extension
     * @return Returns {@code true} if this extension handler can handle the presented extension.
     * @throws Exception If something goes wrong.
     */
    public boolean handle(ExtensionHandlerContext context, Extension extension, Feature feature) throws Exception;
}
