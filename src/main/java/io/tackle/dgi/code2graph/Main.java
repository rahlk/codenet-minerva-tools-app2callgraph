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

import com.ibm.wala.cast.java.loader.JavaSourceLoaderImpl;
import com.ibm.wala.cast.java.translator.jdt.ecj.ECJClassLoaderFactory;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;
import io.tackle.dgi.code2graph.utils.Log;
import io.tackle.dgi.code2graph.utils.ScopeUtils;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

public class Main {
    /**
     * Convert java binary (*.jar, *.ear, *.war) to a neo4j graph.
     *
     * usage: ./code2graph [-h] [-q] [-i <arg>] [-o <arg>]
     * 
     * -h,--help Print this help message.
     * -q,--quiet Don't print logs to console.
     * -i,--input <arg> Path to the input jar.
     * -o,--output <arg> Destination to save the output graph (as json).
     */
    public static void main(String... args) {
        // Set Log Level
        Options options = new Options();
        options.addOption("i", "input", true, "Path to the input jar.");
        options.addOption("o", "output", true, "Destination to save the output graph (as json).");
        options.addOption("q", "quiet", false, "Don't print logs to console.");
        options.addOption("h", "help", false, "Print this help message.");
        CommandLineParser parser = new DefaultParser();

        CommandLine cmd = null;

        String header = "Convert java binary (*.jar, *.ear, *.war) to a neo4j graph.\n\n";
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
            if (!cmd.hasOption("input")) {
                throw new RuntimeException(
                        "[Runtime Exception] Need to provide an input JAR to process.\n\n");
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

        String jars = cmd.getOptionValue("input");
        AnalysisScope scope = ScopeUtils.createScope(jars);

        // Make class Heirarchy

        Log.info("Make class hierarchy.");
        try {
            IClassHierarchy cha = ClassHierarchyFactory.make(scope, new ECJClassLoaderFactory(scope.getExclusions()));
            Log.info("Done class hierarchy: " + cha.getNumberOfClasses() + " classes");
        } catch (ClassHierarchyException che) {
            che.printStackTrace();
            System.exit(-1);
        }

        System.exit(0);
    }
}
