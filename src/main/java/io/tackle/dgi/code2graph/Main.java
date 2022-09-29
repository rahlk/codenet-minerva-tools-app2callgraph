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

package io.tackle.dgi.code2graph;

import com.ibm.wala.cast.ir.ssa.AstIRFactory;
import com.ibm.wala.cast.java.client.impl.ZeroOneCFABuilderFactory;
import com.ibm.wala.cast.java.translator.jdt.ecj.ECJClassLoaderFactory;
import com.ibm.wala.ipa.callgraph.AnalysisCacheImpl;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisOptions.ReflectionOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.SymbolTable;
import io.tackle.dgi.code2graph.utils.AnalysisUtils;
import io.tackle.dgi.code2graph.utils.CallGraphUtil;
import io.tackle.dgi.code2graph.utils.Log;
import io.tackle.dgi.code2graph.utils.ScopeUtils;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.io.FilenameUtils;

public class Main {
  /**
   * Convert java binary (*.jar, *.ear, *.war) to a neo4j graph.
   *
   * <p>
   * usage: ./code2graph [-h|--help] [-q|--quite] [-i|--input <arg> input jar]
   * [-o|--output <arg>
   * output jar]
   */
  public static void main(String... args) {
    // Set Log Level
    Options options = new Options();
    options.addOption("i", "input", true, "Path to the input jar.");
    options.addOption("o", "output", true, "Destination to save the output graph (as graphml/dot/json).");
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
      // Create class hierarchy
      IClassHierarchy cha = ClassHierarchyFactory.make(scope, new ECJClassLoaderFactory(scope.getExclusions()));
      Log.info("Done class hierarchy: " + cha.getNumberOfClasses() + " classes");

      // Initialize analysis options
      AnalysisOptions options = new AnalysisOptions();
      Iterable<Entrypoint> entryPoints = AnalysisUtils.getEntryPoints(cha);
      options.setEntrypoints(entryPoints);
      options.getSSAOptions().setDefaultValues(SymbolTable::getDefaultValue);
      options.setReflectionOptions(ReflectionOptions.NONE);
      IAnalysisCacheView cache = new AnalysisCacheImpl(AstIRFactory.makeDefaultFactory(), options.getSSAOptions());

      // Build the call graph
      Log.info("Building call graph.");
      long start_time = System.currentTimeMillis();
      CallGraphBuilder<?> builder = new ZeroOneCFABuilderFactory().make(options, cache, cha, scope);
      CallGraph callGraph = builder.makeCallGraph(options, null);
      long end_time = System.currentTimeMillis();
      Log.info(
          "Finished construction of call graph. Took "
              + Long.toString(end_time - start_time)
              + " milliseconds.");

      // Save call graph as json
      String savePath = cmd.getOptionValue("output");
      String extenstion = FilenameUtils.getExtension(savePath);
      switch (extenstion) {
        case "graphml":
          CallGraphUtil.callgraph2GraphML(callGraph, savePath);
          break;
        case "json":
          CallGraphUtil.callgraph2JSON(callGraph, savePath);
          break;
        case "dot":
          CallGraphUtil.callgraph2DOT(callGraph, savePath);
          break;
        default:
          throw new IllegalArgumentException("Unknown extension type. Use `filename.[graphml/dot/json].`");
      }
    } catch (ClassHierarchyException che) {
      che.printStackTrace();
      System.exit(-1);
    } catch (IllegalArgumentException iae) {
      iae.printStackTrace();
      System.exit(-1);
    }
    System.exit(0);
  }
}
