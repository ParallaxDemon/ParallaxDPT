package com.parallax.parallax.util;

import com.android.tools.smali.dexlib2.DexFileFactory;
import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.Method;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.stream.Stream;

/**
 * Optional, separate VM conversion pass. The VM converter is supplied by the
 * operator; the shell never claims to virtualize methods it only extracts.
 */
public final class VmpStage {
    private VmpStage() { }
    private static Path temporaryWorkspace;

    public static void cleanup() {
        Path workspace = temporaryWorkspace;
        temporaryWorkspace = null;
        if (workspace == null) {
            return;
        }
        try (Stream<Path> paths = Files.walk(workspace)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    LogUtils.warn("Could not remove VM temporary file %s: %s", path, e.getMessage());
                }
            });
        } catch (IOException e) {
            LogUtils.warn("Could not clean VM workspace %s: %s", workspace, e.getMessage());
        }
    }

    public static File convert(File input, File converter, File rules, int minimumCoverage) throws IOException {
        if (minimumCoverage < 0 || minimumCoverage > 100) {
            throw new IOException("VM coverage must be between 0 and 100 percent");
        }
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
        temporaryWorkspace = workspace;
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
        Set<String> candidates = methodKeys(staged.toFile(), false, workspace);
        Set<String> converted = methodKeys(result.toFile(), true, workspace);
        converted.retainAll(candidates);
        if (converted.isEmpty()) {
            throw new IOException("VM converter did not convert any original method to native; refusing shell-only output");
        }
        if (converted.size() * 100L < candidates.size() * (long) minimumCoverage) {
            throw new IOException("VM method coverage " + converted.size() + "/" + candidates.size()
                    + " is below the required " + minimumCoverage + "%");
        }
        LogUtils.info("VM conversion verified: %d of %d candidate methods (%.1f%%)",
                converted.size(), candidates.size(), 100.0 * converted.size() / candidates.size());
        LogUtils.info("VM conversion output: %s", result);
        return result.toFile();
    }

    private static Set<String> methodKeys(File apk, boolean nativeMethods, Path workspace) throws IOException {
        Set<String> keys = new HashSet<>();
        int dexCount = 0;
        try (ZipFile zip = new ZipFile(apk)) {
            for (java.util.Enumeration<? extends ZipEntry> entries = zip.entries(); entries.hasMoreElements();) {
                ZipEntry entry = entries.nextElement();
                if (!entry.getName().matches("classes(\\d*)\\.dex")) {
                    continue;
                }
                dexCount++;
                Path temporaryDex = Files.createTempFile(workspace, "verify-", ".dex");
                try {
                    try (java.io.InputStream data = zip.getInputStream(entry)) {
                        Files.copy(data, temporaryDex, StandardCopyOption.REPLACE_EXISTING);
                    }
                    DexBackedDexFile dex = DexFileFactory.loadDexFile(temporaryDex.toFile(), Opcodes.getDefault());
                    for (com.android.tools.smali.dexlib2.iface.ClassDef clazz : dex.getClasses()) {
                        for (Method method : clazz.getMethods()) {
                            if ("<init>".equals(method.getName()) || "<clinit>".equals(method.getName())) {
                                continue;
                            }
                            boolean selected = nativeMethods
                                    ? (method.getAccessFlags() & 0x100) != 0 && method.getImplementation() == null
                                    : method.getImplementation() != null;
                            if (selected) {
                                StringBuilder key = new StringBuilder(clazz.getType())
                                        .append("->").append(method.getName()).append('(');
                                for (CharSequence parameter : method.getParameterTypes()) {
                                    key.append(parameter);
                                }
                                keys.add(key.append(')').append(method.getReturnType()).toString());
                            }
                        }
                    }
                } finally {
                    Files.deleteIfExists(temporaryDex);
                }
            }
        } catch (RuntimeException e) {
            throw new IOException("Cannot verify converted DEX methods in " + apk, e);
        }
        if (dexCount == 0) {
            throw new IOException("No classes*.dex found for VM verification in " + apk);
        }
        return keys;
    }
}
