package io.tackle.dgi.code2graph.utils;

import com.ibm.wala.cast.java.ipa.callgraph.JavaSourceAnalysisScope;
import com.ibm.wala.classLoader.BinaryDirectoryTreeModule;
import com.ibm.wala.classLoader.JarFileModule;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.properties.WalaProperties;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.config.FileOfClasses;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

public class ScopeUtils {

  public static String[] stdLibs;
  private static final Pattern patternWar = Pattern.compile(".*\\.war");
  private static final Pattern patternJar = Pattern.compile(".*\\.jar");
  private static final Pattern patternClasses = Pattern.compile(".*/classes$");
  private static final String EXCLUSIONS =
      "java\\/awt\\/.*\n"
          + "javax\\/awt\\/.*\n"
          + "javax\\/swing\\/.*\n"
          + "sun\\/.*\n"
          + /* "com\\/.*\n" + */ "jdk\\/.*\n"
          + "oracle\\/.*\n"
          + "apple\\/.*\n"
          + "netscape\\/.*\n"
          + "javafx\\/.*\n"
          + "org\\/w3c\\/.*\n"
          + "org\\/xml\\/.*\n"
          + "org\\/jcp\\/.*\n"
          + "org\\/ietf\\/.*\n"
          + "org\\/omg\\/.*\n"
          + "java\\/security\\/.*\n"
          + "java\\/beans\\/.*\n"
          + "java\\/time\\/.*\n"
          + "java\\/text\\/.*\n"
          + "java\\/net\\/.*\n"
          + "java\\/nio\\/.*\n" /* + "java\\/io\\/.*\n" */
          + "java\\/math\\/.*\n"
          + "java\\/applet\\/.*\n"
          + "java\\/rmi\\/.*\n"
          + "";

  /**
   * Create an analysis scope base on the input
   *
   * @param inputs Directories to consider for scope creation.
   * @return scope The created analysis scope
   * @throws IOException
   */
  public static AnalysisScope createScope(String inputs) throws IOException {
    Log.info("Create analysis scope.");
    AnalysisScope scope = new JavaSourceAnalysisScope();
    scope = addDefaultExclusions(scope);
    // add standard libraries to scope
    String[] stdlibs = WalaProperties.getJ2SEJarFiles();
    for (String stdlib : stdlibs) {
      scope.addToScope(ClassLoaderReference.Primordial, new JarFile(stdlib));
    }
    setStdLibs(stdlibs);

    String tmpDirString = System.getProperty("java.io.tmpdir");
    Path workDir = Paths.get(tmpDirString);
    List<String> classRoots = new ArrayList<>();
    List<String> jars = new ArrayList<>();

    String[] binaryFiles = inputs.split(":");

    for (String s : binaryFiles) {
      if (new File(s).isDirectory()) {
        classRoots.add(s);
      } else if (s.endsWith(".ear") || s.endsWith(".war")) {
        unpackArchives(s, workDir.resolve("unpacked"), classRoots, jars);
      } else {
        jars.add(s);
      }
    }

    for (String classRoot : classRoots) {
      scope.addToScope(
          ClassLoaderReference.Application, new BinaryDirectoryTreeModule(new File(classRoot)));
    }
    for (String jar : jars) {
      scope.addToScope(ClassLoaderReference.Application, new JarFileModule(new JarFile(jar)));
    }
    return scope;
  }

  private static AnalysisScope addDefaultExclusions(AnalysisScope scope)
      throws UnsupportedEncodingException, IOException {
    Log.info("Add exclusions to scope.");
    scope.setExclusions(new FileOfClasses(new ByteArrayInputStream(EXCLUSIONS.getBytes("UTF-8"))));
    return scope;
  }

  private static void setStdLibs(String[] stdlibs) {
    stdLibs = stdlibs;
  }

  public static String[] getStdLibs() {
    return stdLibs;
  }

  public static void unpackArchives(
      String jarFile, Path workDir, List<String> classRoots, List<String> jars) throws IOException {

    JarFile jar = new java.util.jar.JarFile(jarFile);
    Enumeration<JarEntry> enumEntries = jar.entries();

    while (enumEntries.hasMoreElements()) {
      JarEntry entry = enumEntries.nextElement();
      File file = workDir.resolve(entry.getName()).toFile();
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
        String newWorkDir = workDir + java.io.File.separator + warname;
        unpackArchives(fileName, workDir.resolve(warname), classRoots, jars);
      }
    }
    jar.close();
  }
}
