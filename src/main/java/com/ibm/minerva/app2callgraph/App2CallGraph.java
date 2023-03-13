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

package com.ibm.minerva.app2callgraph;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
// JGraptT to export call graph
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.nio.Attribute;
import org.jgrapht.nio.DefaultAttribute;
import org.jgrapht.nio.json.JSONExporter;

import com.ibm.minerva.app2callgraph.entities.CallGraphEdge;
import com.ibm.minerva.app2callgraph.entities.CallGraphNode;
import com.ibm.minerva.app2callgraph.utils.AnalysisUtils;
import com.ibm.minerva.app2callgraph.utils.Log;
import com.ibm.minerva.app2callgraph.utils.ScopeUtils;
import com.ibm.wala.cast.ir.ssa.AstIRFactory;
import com.ibm.wala.cast.java.client.impl.ZeroOneCFABuilderFactory;
import com.ibm.wala.cast.java.translator.jdt.ecj.ECJClassLoaderFactory;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.AnalysisCacheImpl;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisOptions.ReflectionOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.CallGraphBuilderCancelException;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.SymbolTable;

public class App2CallGraph {

  private static String input;
  private static String outDir;

  /**
   * Convert java binary (*.jar, *.ear, *.war) to a neo4j graph.
   *
   * <p>
   * usage: ./app2callgraph [-h|--help] [-q|--quite] [-i|--input <arg> input jar]
   * [-d|--outdir <arg>] [-o|--output]
   * output jar]`
   */
  public static void main(String... args) {
    Options options = new Options();
    options.addOption("i", "input", true,
        "Path to the input jar(s). For multiple JARs, separate them with ':'. E.g., file1.jar:file2.jar, etc.");
    options.addOption("o", "output", true, "Destination (directory) to save the output graphs.");
    options.addOption("q", "quiet", false, "Don't print logs to console.");
    options.addOption("h", "help", false, "Print this help message.");
    // Experimental options for the finding the root cause of issue #7
    options.addOption("m", "context-mode", false, "Select context mode (0-CFA, 0-1-CFA, etc.).");
    options.addOption("e", "experimental", false,
        "Experimental mode to save the CHA classes for comparison and verification.");
    CommandLineParser parser = new DefaultParser();

    CommandLine cmd = null;

    String header = "Convert java binary (*.jar, *.ear, *.war) to a neo4j graph.\n\n";
    HelpFormatter hf = new HelpFormatter();

    try {
      cmd = parser.parse(options, args);
      if (cmd.hasOption("help")) {
        hf.printHelp("./app2callgraph", header, options, null, true);
        System.exit(0);
      }
      if (cmd.hasOption("experimental")) {
        Log.warn("Using experimental mode. There will be dump of *.txt files.");
      }
      if (cmd.hasOption("quiet")) {
        Log.setVerbosity(false);
      }
      if (!cmd.hasOption("input")) {
        throw new RuntimeException(
            "[Runtime Exception] Need to provide an input JAR to process.\n\n");
      }
      if (!cmd.hasOption("output")) {
        throw new RuntimeException(
            "[Runtime Exception] Need to provide an output directory to save the generated files.\n\n");
      } else {
        outDir = String.valueOf(options.getOption("outdir"));
      }
    } catch (Exception e) {
      System.err.println(e.getMessage());
      hf.printHelp("./app2callgraph", header, options, null, true);
      return;
    }
    try {
      buildAndSaveCallGraph(cmd);
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
  private static void buildAndSaveCallGraph(CommandLine cmd)
      throws ClassHierarchyException, IllegalArgumentException, NullPointerException, IOException,
      CallGraphBuilderCancelException {

    input = cmd.getOptionValue("input");
    AnalysisScope scope = ScopeUtils.createScope(input);
    // Make class Heirarchy
    Log.info("Make class hierarchy.");
    try {
      // Create class hierarchy
      IClassHierarchy cha = ClassHierarchyFactory.make(scope, new ECJClassLoaderFactory(scope.getExclusions()));
      Log.done("Done class hierarchy: " + cha.getNumberOfClasses() + " classes");
      if (cmd.hasOption("experimental")) {
        try (FileWriter writer = new FileWriter("classes_in_class_heirarchy.txt")) {
          for (IClass c : cha) {
            if (AnalysisUtils.isApplicationClass(c))
              writer.write(c.getName() + "\n");
          }
        } catch (FileNotFoundException e) {
          throw e;
        } catch (IOException e) {
          Log.error("Something went wrong");
        }
        System.exit(0);
      }
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
      // TODO: Make the context sensitivity configurable
      CallGraphBuilder<?> builder = new ZeroOneCFABuilderFactory().make(options, cache, cha);
      CallGraph callGraph = builder.makeCallGraph(options, null);
      long end_time = System.currentTimeMillis();
      Log.done(
          "Finished construction of call graph. Took "
              + Long.toString(end_time - start_time)
              + " milliseconds.");

      // Save call graph
      outDir = cmd.getOptionValue("output");
      saveCallGraph(callGraph, outDir, "call_graph.json");
      Log.info("Saving graph to " + (new File(outDir, "call_graph.json")).getAbsolutePath().toString() + ".");
    } catch (ClassHierarchyException | IllegalArgumentException | NullPointerException
        | CallGraphBuilderCancelException che) {
      che.printStackTrace();
      System.exit(-1);
    }
  }

  /**
   * @param klass
   * @return CallGraphNode
   */
  private static CallGraphNode buildCallGraphNode(IClass klass) {
    String className = klass.getName().toString();
    Boolean isPrivateClass = klass.isPrivate();
    Integer num_fields = klass.getAllFields().size();
    Integer num_static_fields = klass.getAllStaticFields().size();
    Integer num_instance_fields = klass.getAllInstanceFields().size();
    Integer num_static_methods = 0;
    for (IMethod method : klass.getDeclaredMethods()) {
      num_static_methods += method.isStatic() ? 1 : 0;
    }

    return new CallGraphNode(className, isPrivateClass, num_fields, num_static_fields, num_instance_fields,
        num_static_methods, num_static_methods);

  }

  /**
   * @param callGraph
   * @return Graph<CallGraphNode, CallGraphEdge>
   */
  private static Graph<CallGraphNode, CallGraphEdge> getDefaultDirectedGraph(CallGraph callGraph) {
    Graph<CallGraphNode, CallGraphEdge> graph = new DefaultDirectedGraph<>(CallGraphEdge.class);
    for (CGNode entrypointNode : callGraph.getEntrypointNodes()) {
      IMethod entryMethod = entrypointNode.getMethod();
      // Get all callsites that may execute in a given method
      Iterator<CallSiteReference> outGoingCalls = entrypointNode.iterateCallSites();
      for (Iterator<CallSiteReference> it = outGoingCalls; it.hasNext();) {
        CallSiteReference callSiteReference = it.next();
        for (CGNode callTarget : callGraph.getPossibleTargets(entrypointNode, callSiteReference)) {
          if (AnalysisUtils.isApplicationClass(callTarget.getMethod().getDeclaringClass())) {
            // Create a node for the source class
            IClass sourceClass = entryMethod.getDeclaringClass();
            CallGraphNode source = buildCallGraphNode(sourceClass);

            // Create a node for the target class
            IClass targetClass = callTarget.getMethod().getDeclaringClass();
            CallGraphNode target = buildCallGraphNode(targetClass);

            // Ignore self references
            if (source.equals(target)) {
              continue;
            }

            // Add the vertices to the final graph
            graph.addVertex(source);
            graph.addVertex(target);
            // Get the edge between the source and the traget
            CallGraphEdge cgEdge = graph.getEdge(source, target);
            // If no edge exists, then create one...
            if (cgEdge == null) {
              CallGraphEdge edge = new CallGraphEdge(entryMethod.getName(), callTarget.getMethod().getName(), 1.0);
              graph.addEdge(source, target, edge);
            }
            // If edge exists, then increment the weight
            else {
              cgEdge.incrementWeight();
            }
          }
        }
      }
    }
    return graph;
  }

  /**
   * @param callGraph
   * @param outPath
   * @param outFile
   */
  public static void saveCallGraph(CallGraph callGraph, String outPath, String outFile) {
    Graph<CallGraphNode, CallGraphEdge> graph = getDefaultDirectedGraph(callGraph);
    JSONExporter<CallGraphNode, CallGraphEdge> exporter = new JSONExporter<>(v -> v.className);
    exporter.setVertexAttributeProvider((v) -> {
      Map<String, Attribute> map = new LinkedHashMap<>();
      map.put("is_class_private", DefaultAttribute.createAttribute(v.getIsPrivate()));
      map.put("num_total_fields", DefaultAttribute.createAttribute(v.getNum_fields()));
      map.put("num_static_fields", DefaultAttribute.createAttribute(v.getNum_static_fields()));
      map.put("num_instance_fields", DefaultAttribute.createAttribute(v.getNum_instance_fields()));
      map.put("num_total_methods", DefaultAttribute.createAttribute(v.getNum_declared_methods()));
      map.put("num_static_methods", DefaultAttribute.createAttribute(v.getNum_static_methods()));
      return map;
    });
    exporter.setEdgeAttributeProvider((e) -> {
      Map<String, Attribute> map = new LinkedHashMap<>();
      map.put("weight", DefaultAttribute.createAttribute(e.getWeight()));
      return map;
    });
    // Export the graph to JSON
    exporter.exportGraph(graph, new File(outPath, outFile));
  }

}
