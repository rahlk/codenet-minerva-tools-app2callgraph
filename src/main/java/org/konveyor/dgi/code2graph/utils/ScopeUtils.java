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

package org.konveyor.dgi.code2graph.utils;

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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;

public class ScopeUtils {

  public static String[] stdLibs;
  private static final String EXCLUSIONS = "java\\/awt\\/.*\n"
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
      + "org\\/apache\\/.*\n"
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
