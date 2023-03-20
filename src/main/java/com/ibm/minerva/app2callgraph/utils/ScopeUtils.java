/*
Copyright IBM Corporation 2022

Licensed under the Apache Public License 2.0, Version 2.0 (the "License");
you may not use this file except in compliance with the License.

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package com.ibm.minerva.app2callgraph.utils;

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
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;

public class ScopeUtils {

  public static String[] stdLibs;
  // I am not including any exclusions for now.
  private static final String EXCLUSIONS = "";

  /**
   * Create an analysis scope base on the input
   *
   * @param inputs Directories to consider for scope creation.
   * @return scope The created analysis scope
   * @throws IOException
   * @throws URISyntaxException
   */

  public static AnalysisScope createScope(String inputs, String extraLibs) throws IOException, URISyntaxException {
    Log.info("Create analysis scope.");
    AnalysisScope scope = new JavaSourceAnalysisScope();
    scope = addDefaultExclusions(scope);

    Log.info("Loading Java SE standard libs.");
    String[] stdlibs = WalaProperties.getJ2SEJarFiles();
    for (String stdlib : stdlibs) {
      scope.addToScope(ClassLoaderReference.Primordial, new JarFile(stdlib));
    }
    setStdLibs(stdlibs);

    // ---------------------
    // Add JEE jars to scope
    // ---------------------
    Log.info("Loading JavaEE standard libs.");
    URL url = ScopeUtils.class.getClassLoader().getResource("libs");
    String jeeJarPath = url.getPath();
    File[] jeeJarFiles = new File(jeeJarPath).listFiles();
    for (File jarFile : jeeJarFiles) {
      Log.info("↪ Adding " + jarFile + " to scope.");
      scope.addToScope(ClassLoaderReference.Primordial, new JarFile(jarFile.getAbsolutePath()));
    }

    // -------------------------------------
    // Add extra user provided JARS to scope
    // -------------------------------------
    if (!(extraLibs == null)) {
      Log.info("Loading user specified extra libs.");
      File[] listOfExtraLibs = new File(extraLibs).listFiles();
      for (File extraLibJar : listOfExtraLibs) {
        Log.info("↪ Adding " + extraLibJar + " to scope.");
        scope.addToScope(ClassLoaderReference.Primordial, new JarFile(extraLibJar.getAbsolutePath()));
      }
    } else {
      Log.warn("No extra libraries to process.");
    }

    String tmpDirString = System.getProperty("java.io.tmpdir");
    Path workDir = Paths.get(tmpDirString);
    List<String> classRoots = new ArrayList<>();
    List<String> jars = new ArrayList<>();

    String[] binaryFiles = inputs.split(":");

    for (String s : binaryFiles) {
      if (new File(s).isDirectory()) {
        classRoots.add(s);
      } else if (s.endsWith(".ear") || s.endsWith(".war")) {
        JarUtils.unpackArchives(s, workDir.resolve("unpacked"), classRoots, jars);
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
}
