package io.tackle.dgi.code2graph.utils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Path;
import java.util.jar.JarFile;

import com.ibm.wala.cast.java.ipa.callgraph.JavaSourceAnalysisScope;
import com.ibm.wala.classLoader.SourceDirectoryTreeModule;
import com.ibm.wala.classLoader.SourceFileModule;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.properties.WalaProperties;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.config.FileOfClasses;

public class ScopeUtils {

    private static Path workDir = Path.of("/tmp/code2graph-tmp");

    /**
     * Create an analysis scope base on the input
     * 
     * @param inputs Directories to consider for scope creation.
     * @return scope The created analysis scope
     * @throws IOException
     */
    public static AnalysisScope createScope(String inputs) throws IOException {
        Log.toConsole("Create analysis scope.");
        AnalysisScope scope = new JavaSourceAnalysisScope();
        scope = addDefaultExclusions(scope);
        // add standard libraries to scope
        String[] stdlibs = WalaProperties.getJ2SEJarFiles();
        for (String stdlib : stdlibs) {
            scope.addToScope(ClassLoaderReference.Primordial, new JarFile(stdlib));
        }

        String[] sourceDirs = inputs.split(":");
        for (String sourceDir : sourceDirs) {
            // add the source directory
            File root = new File(sourceDir);
            if (root.isDirectory()) {
                scope.addToScope(JavaSourceAnalysisScope.SOURCE, new SourceDirectoryTreeModule(root));
            } else {
                String srcFileName = sourceDir.substring(sourceDir.lastIndexOf(File.separator) + 1);
                assert root.exists() : "couldn't find " + sourceDir;
                scope.addToScope(
                        JavaSourceAnalysisScope.SOURCE, new SourceFileModule(root, srcFileName, null));
            }
        }
        // build the class hierarchy
        return scope;
    }

    private static AnalysisScope addDefaultExclusions(AnalysisScope scope)
            throws UnsupportedEncodingException, IOException {
        Log.toConsole("Add exclusions to scope.");
        scope.setExclusions(new FileOfClasses(new ByteArrayInputStream(EXCLUSIONS.getBytes("UTF-8"))));
        return scope;
    }

    private static final String EXCLUSIONS = "java\\/awt\\/.*\n" + "javax\\/awt\\/.*\n" + "javax\\/swing\\/.*\n"
            + "sun\\/.*\n" + /* "com\\/.*\n" + */ "jdk\\/.*\n" + "oracle\\/.*\n" + "apple\\/.*\n" + "netscape\\/.*\n"
            + "javafx\\/.*\n" + "org\\/w3c\\/.*\n" + "org\\/xml\\/.*\n" + "org\\/jcp\\/.*\n" + "org\\/ietf\\/.*\n"
            + "org\\/omg\\/.*\n" + "java\\/security\\/.*\n" + "java\\/beans\\/.*\n" + "java\\/time\\/.*\n"
            + "java\\/text\\/.*\n" + "java\\/net\\/.*\n" + "java\\/nio\\/.*\n" /* + "java\\/io\\/.*\n" */
            + "java\\/math\\/.*\n" + "java\\/applet\\/.*\n" + "java\\/rmi\\/.*\n" + "";

}
