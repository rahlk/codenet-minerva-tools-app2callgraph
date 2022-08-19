/*
Copyright IBM Corporation 2021

Licensed under the Apache Public License 2.0, Version 2.0 (the "License");
you may not use this file except in compliance with the License.

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package io.tackle.dgi.code2graph;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

import com.ibm.wala.cast.java.loader.JavaSourceLoaderImpl;
import com.ibm.wala.cast.java.translator.jdt.ecj.ECJClassLoaderFactory;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.ipa.cha.ClassHierarchyException;

import io.tackle.dgi.code2graph.utils.Log;
import io.tackle.dgi.code2graph.utils.ScopeUtils;

import io.tackle.dgi.code2graph.CustomECJSourceLoaderImpl;

public class Main {
    /**
     * Convert source code (project folder) to a neo4j graph.
     * 
     * usage: ./code2graph [-b <arg>] [-h] [-i <arg>] [-q]
     * 
     * 
     * -h,--help Print this help message.
     * -i,--input <arg> Path to the source directory root.
     * -q,--quiet Don't print logs to console.
     */
    public static void main(String... args) {
        // Set Log Level
        Log.setLogLevel("INFO");
        Options options = new Options();
        options.addOption("s", "source-dir", true, "Path to the source directory root.");
        options.addOption("q", "quiet", false, "Don't print logs to console.");
        options.addOption("h", "help", false, "Print this help message.");
        CommandLineParser parser = new DefaultParser();

        CommandLine cmd = null;

        String header = "Convert source code (project folder) to a neo4j graph.\n\n";
        HelpFormatter hf = new HelpFormatter();

        try {
            cmd = parser.parse(options, args);
            if (cmd.hasOption("help")) {
                hf.printHelp("./code2graph", header, options, null, true);
                System.exit(0);
            }
            if (cmd.hasOption("quiet")) {
                Log.setVerbosity(false);
            }
            if (!cmd.hasOption("source-dir")) {
                throw new RuntimeException(
                        "[Runtime Exception] Need to provide the path to project source directory.\n\n");
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
            hf.printHelp("./code2graph", header, options, null, true);
            return;
        }
        try {
            run(cmd);
        } catch (Exception e) {
            System.err.println(e);
            System.exit(1);
        }
        System.exit(0);
    }

    /**
     * @param cmd
     * @throws Exception
     */
    private static void run(CommandLine cmd) throws Exception {

        String sourceDir = cmd.getOptionValue("source-dir");
        AnalysisScope scope = ScopeUtils.createScope(sourceDir);

        // Make class Heirarchy

        Log.toConsole("Make class hierarchy.");
        try {
            IClassHierarchy cha = ClassHierarchyFactory.make(scope,
                    new ECJClassLoaderFactory(scope.getExclusions()) {
                        @Override
                        protected JavaSourceLoaderImpl makeSourceLoader(ClassLoaderReference classLoaderReference,
                                IClassHierarchy cha, IClassLoader parent) {
                            // TODO: Why do these lines fix issue #1
                            return new CustomECJSourceLoaderImpl(classLoaderReference, parent, cha,
                                    ScopeUtils.getStdLibs());
                        }
                    });
            Log.toConsole("Done class hierarchy: " + cha.getNumberOfClasses() + " classes");
        } catch (ClassHierarchyException che) {
            che.printStackTrace();
            System.exit(-1);
        }

        System.exit(0);
    }

}