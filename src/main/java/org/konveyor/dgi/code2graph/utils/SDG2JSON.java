package org.konveyor.dgi.code2graph.utils;

import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.slicer.*;
import com.ibm.wala.ssa.*;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.GraphSlicer;
import com.ibm.wala.util.graph.traverse.DFS;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.nio.Attribute;
import org.jgrapht.nio.DefaultAttribute;
import org.jgrapht.nio.json.JSONExporter;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;


import org.konveyor.dgi.code2graph.utils.graph.CallGraphEdge;
import org.konveyor.dgi.code2graph.utils.graph.ClassNode;
import org.konveyor.dgi.code2graph.utils.graph.Node;
import org.konveyor.dgi.code2graph.utils.graph.Edge;

/**
 * The type Sdg 2 json.
 */
public class SDG2JSON {

    private static class JSONElementFactory {
        static JSONObject makeNodeJSONObject(Map<String, String> nodeAttr) {
            JSONObject newNode = new JSONObject();
            nodeAttr.forEach(newNode::put);
            return newNode;
        }
        static JSONObject makeEdgeJSONObject(Map<String, Object> edges) {
            JSONObject edgeObject = new JSONObject();
            edges.forEach((key, obj) -> {
                if (obj instanceof ArrayList) {
                    ArrayList<String> val = (ArrayList<String>) obj;
                    edgeObject.put("between", new JSONArray(val));
                } else if(obj instanceof String){
                    edges.forEach(edgeObject::put);
                }
            });
            return edgeObject;
        }
    }

    private static String sdgFeatures(Statement n) {
        if (n instanceof MethodEntryStatement) {
            return "entry " + n.getNode().getMethod().getName();
        } else if (n instanceof MethodExitStatement) {
            return "exit " + n.getNode().getMethod().getName();
        } else if (n instanceof PhiStatement ||
                n instanceof ParamCaller ||
                n instanceof ParamCallee ||
                n instanceof NormalReturnCallee ||
                n instanceof NormalReturnCaller) {
            return "flow";
        } else if (n instanceof NormalStatement) {
            SSAInstruction inst = ((NormalStatement)n).getInstruction();
            if (inst instanceof SSABinaryOpInstruction) {
                return ((SSABinaryOpInstruction)inst).getOperator().toString();
            } else if (inst instanceof SSAUnaryOpInstruction) {
                return ((SSAUnaryOpInstruction)inst).getOpcode().toString();
            } else if (inst instanceof SSAConditionalBranchInstruction) {
                return ((SSAConditionalBranchInstruction)inst).getOperator().toString();
            } else if (inst instanceof SSAAbstractInvokeInstruction) {
                return ((SSAAbstractInvokeInstruction)inst).getDeclaredTarget().getName().toString();
            } else if (inst instanceof SSANewInstruction) {
                return ((SSANewInstruction)inst).getConcreteType().getName().toString();
            } else {
                return null;
            }

        } else {
            return null;
        }
    }

    private static org.jgrapht.Graph<Node, Edge> build(Supplier<Iterator<Statement>> entryPoints,
                              Graph<Statement> sdg,
                              Function<Statement, String> features,
                              BiFunction<Statement, Statement, String> edgeLabels) {

        org.jgrapht.Graph<Node, Edge> graph = new DefaultDirectedGraph<>(Edge.class);
        // We'll use forward and backward search on the DFS to identify which CFG nodes are dominant
        // This is a forward DFS search (or exit time first search)
        int dfsNumber = 0;
        Map<Statement,Integer> dfsFinish = HashMapFactory.make();
        Iterator<Statement> search = DFS.iterateFinishTime(sdg, entryPoints.get());
        while (search.hasNext()) {
            dfsFinish.put(search.next(), dfsNumber++);
        }

        // THis is a reverse DFS search (or entry time first search)
        int reverseDfsNumber = 0;
        Map<Statement,Integer> dfsStart = HashMapFactory.make();
        Iterator<Statement> reverseSearch = DFS.iterateDiscoverTime(sdg, entryPoints.get());
        while (reverseSearch.hasNext()) {
            dfsStart.put(reverseSearch.next(), reverseDfsNumber++);
        }

        // Populate graph
        sdg.stream()
                .filter(dfsFinish::containsKey)
                .sorted(Comparator.comparingInt(dfsFinish::get))
                .forEach(currentStatement -> {
                    sdg.getSuccNodes(currentStatement).forEachRemaining(nextStatement -> {
                        if (dfsFinish.containsKey(nextStatement) &&
                                !((dfsStart.get(currentStatement) >= dfsStart.get(nextStatement)) &&
                                        (dfsFinish.get(currentStatement) <= dfsFinish.get(nextStatement)) &&
                                        !Objects.equals(currentStatement.getNode().getMethod().toString(), nextStatement.getNode().getMethod().toString()))) {


                            Node sourceNode = new Node(dfsStart.get(currentStatement), currentStatement);
                            Node destinationNode = new Node(dfsStart.get(nextStatement), nextStatement);

                            graph.addVertex(sourceNode);
                            graph.addVertex(destinationNode);
                            graph.addEdge(
                                    sourceNode,
                                    destinationNode,
                                    new Edge(dfsStart.get(currentStatement), dfsStart.get(nextStatement), edgeLabels.apply(currentStatement, nextStatement)));
                        }
                    });
                });
        return graph;
    }
    public static void convert2JSON(SDG<? extends InstanceKey> sdg, File output) {
        // Prune the Graph to keep only application classes.
        Log.info("Pruning SDG to keep only Application classes.");
        Graph<Statement> prunedGraph = GraphSlicer.prune(sdg,
                statement -> {
                    return (
                            statement.getNode()
                                    .getMethod()
                                    .getDeclaringClass()
                                    .getClassLoader()
                                    .getReference()
                                    .equals(ClassLoaderReference.Application)
                    );
                }
        );
        Log.done("SDG built and pruned. It has " + prunedGraph.getNumberOfNodes() + " nodes.");

        CallGraph callGraph = sdg.getCallGraph();

        // A supplier to get entries
        Supplier<Iterator<Statement>> entryPointsSupplier =
                () -> callGraph.getEntrypointNodes().stream().map(n -> (Statement)new MethodEntryStatement(n)).iterator();


        org.jgrapht.Graph<Node, Edge> graph = build(entryPointsSupplier,
                                            prunedGraph,
                                            SDG2JSON::sdgFeatures,
                                            (p, s) -> String.valueOf(sdg.getEdgeLabels(p, s).iterator().next()));


        JSONExporter<Node, Edge> exporter = new JSONExporter<>(v -> String.valueOf(v.getId()));
        exporter.setVertexAttributeProvider((v) -> {
            Map<String, Attribute> map = new LinkedHashMap<>();
            map.put("class", DefaultAttribute.createAttribute(v.getInstructionClass()));
            map.put("method", DefaultAttribute.createAttribute(v.getInstructionMethod()));
            map.put("position", DefaultAttribute.createAttribute(v.getInstructionPosition()));
            return map;
        });
        exporter.setEdgeAttributeProvider((e) -> {
            Map<String, Attribute> map = new LinkedHashMap<>();
            map.put("type", DefaultAttribute.createAttribute(e.getType()));
            map.put("between", DefaultAttribute.createAttribute(String.join(", ", e.getBetween())));
            map.put("context", DefaultAttribute.createAttribute(e.getContext()));
            return map;
        });
        // Export the graph to JSON
        exporter.exportGraph(graph, output);


    }
}
