package org.janelia.workstation.application;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class NetBeansJPackageLauncher {

    private NetBeansJPackageLauncher() {
    }

    public static void main(String[] args) throws Exception {
        String brandingToken = System.getProperty("workstation.brandingToken", "janeliaws");
        String clusterName = System.getProperty("workstation.clusterName", "JaneliaWorkstation");

        Path payloadRoot = findPayloadRoot(brandingToken);
        Path platformRoot = payloadRoot.resolve("platform");
        Path bootJar = platformRoot.resolve("lib").resolve("boot.jar");

        System.setProperty("netbeans.home", platformRoot.toString());

        List<String> netbeansArgs = new ArrayList<>();
        netbeansArgs.add("--branding");
        netbeansArgs.add(brandingToken);
        netbeansArgs.add("--clusters");
        netbeansArgs.add(clusterPath(payloadRoot, brandingToken, clusterName));
        netbeansArgs.add("--userdir");
        netbeansArgs.add(defaultUserdir(clusterName));
        netbeansArgs.add("--laf");
        netbeansArgs.add("com.formdev.flatlaf.FlatDarkLaf");
        for (String arg : args) {
            netbeansArgs.add(arg);
        }

        try (URLClassLoader classLoader = new URLClassLoader(new URL[]{bootJar.toUri().toURL()},
                NetBeansJPackageLauncher.class.getClassLoader())) {
            Thread.currentThread().setContextClassLoader(classLoader);
            Class<?> mainClass = Class.forName("org.netbeans.Main", true, classLoader);
            Method main = mainClass.getMethod("main", String[].class);
            main.invoke(null, (Object) netbeansArgs.toArray(new String[0]));
        }
    }

    private static Path findPayloadRoot(String brandingToken) throws Exception {
        Path codeSource = Paths.get(NetBeansJPackageLauncher.class.getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .toURI());
        Path appDir = Files.isRegularFile(codeSource) ? codeSource.getParent() : codeSource;

        Path nestedPayload = appDir.resolve(brandingToken);
        if (Files.isDirectory(nestedPayload.resolve("platform"))) {
            return nestedPayload;
        }
        Path siblingPayload = appDir.getParent() == null ? null : appDir.getParent().resolve(brandingToken);
        if (siblingPayload != null && Files.isDirectory(siblingPayload.resolve("platform"))) {
            return siblingPayload;
        }
        if (Files.isDirectory(appDir.resolve("platform"))) {
            return appDir;
        }
        throw new IllegalStateException("Cannot locate NetBeans platform payload under " + appDir);
    }

    private static String clusterPath(Path payloadRoot, String brandingToken, String clusterName) throws Exception {
        Path clustersFile = payloadRoot.resolve("etc").resolve(brandingToken + ".clusters");
        List<String> clusterEntries = Files.exists(clustersFile)
                ? Files.readAllLines(clustersFile)
                : List.of("platform", "extra", clusterName);

        List<String> clusters = new ArrayList<>();
        for (String entry : clusterEntries) {
            String trimmed = entry.trim();
            if (trimmed.isEmpty() || trimmed.equals("bin") || trimmed.equals("etc")) {
                continue;
            }
            Path cluster = payloadRoot.resolve(trimmed);
            if (Files.isDirectory(cluster)) {
                clusters.add(cluster.toString());
            }
        }
        return String.join(System.getProperty("path.separator"), clusters);
    }

    private static String defaultUserdir(String clusterName) {
        String home = System.getProperty("user.home");
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("mac")) {
            return home + "/Library/Application Support/" + clusterName + "/0.4";
        }
        return home + "/." + clusterName + "/0.4";
    }
}
