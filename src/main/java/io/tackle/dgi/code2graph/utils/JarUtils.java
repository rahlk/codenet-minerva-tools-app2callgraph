package io.tackle.dgi.code2graph.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.List;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

public class JarUtils {
    private static final Pattern patternWar = Pattern.compile(".*\\.war");
    private static final Pattern patternJar = Pattern.compile(".*\\.jar");
    private static final Pattern patternClasses = Pattern.compile(".*/classes$");

    public static void unpackArchives(
            String jarFile, Path workDir, List<String> classRoots, List<String> jars) throws IOException {

        JarFile jar = new java.util.jar.JarFile(jarFile);
        Enumeration<JarEntry> enumEntries = jar.entries();

        while (enumEntries.hasMoreElements()) {
            JarEntry entry = enumEntries.nextElement();
            File file = ((java.nio.file.Path) workDir).resolve(entry.getName()).toFile();
            String fileName = file.getAbsolutePath();
            if (!file.exists()) {
                file.getParentFile().mkdirs();
            }

            boolean mj = patternJar.matcher(fileName).find();
            if (mj) {
                jars.add(fileName);
            }

            if (entry.isDirectory()) {
                boolean mcs = patternClasses.matcher(fileName).find();
                if (mcs) {
                    classRoots.add(fileName);
                }
                continue;
            }

            Files.copy(jar.getInputStream(entry), file.toPath(), StandardCopyOption.REPLACE_EXISTING);

            boolean mw = patternWar.matcher(fileName).find();
            if (mw) {
                String warname = entry.getName().substring(0, entry.getName().lastIndexOf("."));
                unpackArchives(fileName, ((java.nio.file.Path) workDir).resolve(warname), classRoots, jars);
            }
        }
        jar.close();
    }
}
