package com.parallax.parallax.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipFile;

/**
 * Optional, separate VM conversion pass. The VM converter is supplied by the
 * operator; the shell never claims to virtualize methods it only extracts.
 */
public final class VmpStage {
    private VmpStage() { }

    public static File convert(File input, File converter, File rules) throws IOException {
        if (!input.isFile() || !input.getName().endsWith(".apk")) {
            throw new IOException("VMP requires an existing APK input");
        }
        if (!converter.isFile()) {
            throw new IOException("VMP converter JAR not found: " + converter);
        }
        if (rules != null && !rules.isFile()) {
            throw new IOException("VMP rules not found: " + rules);
        }

        Path workspace = Files.createTempDirectory("parallax-vmp-");
        Path staged = workspace.resolve(input.getName());
        Files.copy(input.toPath(), staged, StandardCopyOption.REPLACE_EXISTING);
        String name = input.getName();
        String stem = name.substring(0, name.length() - ".apk".length());
        Path result = workspace.resolve("build").resolve(stem + "-protect.apk");

        List<String> command = new ArrayList<>();
        command.add(new File(System.getProperty("java.home"), "bin/java").getAbsolutePath());
        command.add("-jar");
        command.add(converter.getAbsolutePath());
        command.add("apk");
        command.add(staged.toAbsolutePath().toString());
        if (rules != null) {
            command.add(rules.getAbsolutePath());
        }

        LogUtils.info("Running VM conversion for %s", name);
        Process process = new ProcessBuilder(command).directory(workspace.toFile()).inheritIO().start();
        int status;
        try {
            status = process.waitFor();
        } catch (InterruptedException e) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            throw new IOException("VM conversion interrupted", e);
        }
        if (status != 0 || !Files.isRegularFile(result)) {
            throw new IOException("VM conversion failed (exit=" + status + "); expected " + result);
        }
        try (ZipFile zip = new ZipFile(result.toFile())) {
            if (zip.getEntry("AndroidManifest.xml") == null) {
                throw new IOException("VM converter returned an APK without AndroidManifest.xml");
            }
        }
        LogUtils.info("VM conversion output: %s", result);
        return result.toFile();
    }
}
