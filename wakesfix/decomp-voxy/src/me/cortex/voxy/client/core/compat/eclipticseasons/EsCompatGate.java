/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.neoforged.fml.loading.LoadingModList
 *  net.neoforged.fml.loading.moddiscovery.ModFileInfo
 *  net.neoforged.neoforgespi.language.IModInfo
 *  org.apache.maven.artifact.versioning.ArtifactVersion
 *  org.apache.maven.artifact.versioning.DefaultArtifactVersion
 */
package me.cortex.voxy.client.core.compat.eclipticseasons;

import java.util.List;
import me.cortex.voxy.common.Logger;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.fml.loading.moddiscovery.ModFileInfo;
import net.neoforged.neoforgespi.language.IModInfo;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

public final class EsCompatGate {
    private static final ArtifactVersion MIN_VERSION = new DefaultArtifactVersion("0.15.0-rc");
    private static Boolean cached;

    private EsCompatGate() {
    }

    public static boolean shouldArm() {
        Boolean v = cached;
        if (v == null) {
            cached = v = Boolean.valueOf(EsCompatGate.compute());
        }
        return v;
    }

    private static boolean compute() {
        ArtifactVersion version;
        ModFileInfo file;
        LoadingModList list = LoadingModList.get();
        ModFileInfo modFileInfo = file = list == null ? null : list.getModFileById("eclipticseasons");
        if (file == null) {
            return false;
        }
        List mods = file.getMods();
        ArtifactVersion artifactVersion = version = mods.isEmpty() ? null : ((IModInfo)mods.get(0)).getVersion();
        if (version != null && version.compareTo((Object)MIN_VERSION) >= 0) {
            return true;
        }
        Logger.warn("EclipticSeasons " + String.valueOf(version) + " is older than " + String.valueOf(MIN_VERSION) + " and lacks symbols the seasonal LOD bridge links against; seasonal LOD stays disabled. Update EclipticSeasons to see snow/ice/seasonal models at LOD range.");
        return false;
    }
}

