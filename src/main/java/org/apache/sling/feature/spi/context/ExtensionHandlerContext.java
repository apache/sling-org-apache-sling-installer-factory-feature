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

    public ArtifactManager getArtifactManager();
}
