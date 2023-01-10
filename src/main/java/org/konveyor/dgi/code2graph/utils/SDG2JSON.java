package org.konveyor.dgi.code2graph.utils;

import com.ibm.wala.cast.loader.AstMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.ContextKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.slicer.*;
import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.ssa.*;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.GraphSlicer;
import com.ibm.wala.util.graph.traverse.DFS;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;


/**
 * The type Sdg 2 json.
 */
public class SDG2JSON {

    private static JSONObject sdgJSON = new JSONObject();
    private static JSONArray nodesArray = new JSONArray();
    private static JSONArray edgesArray = new JSONArray();

    private static class GraphAugmentor {
        static Position getPosition(Statement srcNode) {
            Position srcPos;
            CGNode srcCG = srcNode.getNode();
            try {
                AstMethod.DebuggingInformation debugInfo = ((AstMethod)srcCG.getMethod()).debugInfo();
                if (srcNode.getKind() == Statement.Kind.NORMAL) {
                    SSAInstruction srcInst = ((NormalStatement)srcNode).getInstruction();
                    srcPos = debugInfo.getInstructionPosition(srcInst.iIndex());
                } else if (srcNode.getKind() == Statement.Kind.PARAM_CALLER) findParamCaller: {
                    SSAInstruction call = ((ParamCaller)srcNode).getInstruction();
                    int vn = ((ParamCaller)srcNode).getValueNumber();
                    for(int i = 0; i < call.getNumberOfUses(); i++) {
                        if (call.getUse(i) == vn) {
                            srcPos = debugInfo.getOperandPosition(call.iIndex(), i);
                            break findParamCaller;
                        }
                    }
                    assert false;
                    return null;
                } else if (srcNode.getKind() == Statement.Kind.PARAM_CALLEE) {
                    int arg = ((ParamCallee)srcNode).getValueNumber() - 1;
                    srcPos = debugInfo.getParameterPosition(arg);
                } else {
                    return null;
                }
                return srcPos;
                }
            catch (ClassCastException classCastException) {
                return null;
            }
        }
    }
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

    private static void write(Supplier<Iterator<Statement>> entryPoints,
                              Graph<Statement> sdg,
                              Function<Statement, String> features,
                              BiFunction<Statement, Statement, String> edgeLabels) {

        // Initialize the SDG JSON object
        sdgJSON.put("version", "1");
        sdgJSON.put("directed", "true");

        // TODO: Document why this is needed.
        int dfsNumber = 0;
        Map<Statement,Integer> dfsFinish = HashMapFactory.make();
        Iterator<Statement> search = DFS.iterateFinishTime(sdg, entryPoints.get());
        while (search.hasNext()) {
            dfsFinish.put(search.next(), dfsNumber++);
        }

        // TODO: Document why this is needed.
        int reverseDfsNumber = 0;
        Map<Statement,Integer> dfsStart = HashMapFactory.make();
        Iterator<Statement> reverseSearch = DFS.iterateDiscoverTime(sdg, entryPoints.get());
        while (reverseSearch.hasNext()) {
            dfsStart.put(reverseSearch.next(), reverseDfsNumber++);
        }

        // Populate nodes array
        sdg.stream()
                .filter(dfsFinish::containsKey)
                .sorted((l, r) -> dfsFinish.get(l) - dfsFinish.get(r))
                .forEach(statement -> {
                    /* Get node information:
                     * id, method, class, source file and position, node feature
                     */
                    Map<String, String> theNode = new HashMap<>();
                    // ID as per the DFS search order.
                    theNode.put("id", Integer.toString(dfsStart.get(statement)));
                    // Source Position
                    if (GraphAugmentor.getPosition(statement) == null)
                        theNode.put("pos", Integer.toString(-1));
                    else
                        theNode.put("pos", GraphAugmentor.getPosition(statement).prettyPrint());

                    // Method Information
                    theNode.put("method", statement.getNode().getMethod().toString());
                    // Class Information
                    theNode.put("class", statement.getNode().getMethod().getDeclaringClass().toString());
//                    if (!(features.apply(statement).isEmpty()))
//                        theNode.put("feature", features.apply(statement));
//                    else
//                        theNode.put("feature", "");
                    nodesArray.put(JSONElementFactory.makeNodeJSONObject(theNode));
                });

        // edge files
        sdg.stream()
                .filter(currentStatement -> dfsFinish.containsKey(currentStatement))
                .sorted((l, r) -> dfsFinish.get(l) - dfsFinish.get(r))
                .forEach(currentStatement -> sdg.getSuccNodes(currentStatement).forEachRemaining(nextStatement -> {
                    Map<String, Object> edgeInfoMap = new HashMap<>();
                    if (dfsFinish.containsKey(nextStatement) &&
                            !( (dfsStart.get(currentStatement) >= dfsStart.get(nextStatement)) && (dfsFinish.get(currentStatement) <= dfsFinish.get(nextStatement)) )) {
                        // Create an edge
                        edgeInfoMap.put("between", new ArrayList<>(Arrays.asList(dfsFinish.get(currentStatement), dfsFinish.get(nextStatement))));
                        // Populate edge feature
                        if (edgeLabels != null)
                            edgeInfoMap.put("feature", edgeLabels.apply(currentStatement, nextStatement));
                        else
                            edgeInfoMap.put("feature", null);
                        // Populate prev context information
                        // TODO:
//                        String prevContext = currentStatement.getNode().getContext().get(ContextKey.CALLSITE).toString();
//                        edgeInfoMap.put("prev_context", prevContext);
//
//                        // Populate next context information
//                        String nextContext = nextStatement.getNode().getContext().get(ContextKey.CALLSITE).toString();
//                        edgeInfoMap.put("next_context", nextContext);
                    }
                    edgesArray.put(JSONElementFactory.makeEdgeJSONObject(edgeInfoMap));
                }));

        sdgJSON.put("nodes", nodesArray);
        sdgJSON.put("edges", edgesArray);

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


        write(entryPointsSupplier,
                prunedGraph,
                SDG2JSON::sdgFeatures,
                (p, s) -> String.valueOf(sdg.getEdgeLabels(p, s).iterator().next()));


        try (PrintWriter f = new PrintWriter(new FileWriter(output))) {
            f.println(sdgJSON);
        } catch (IOException e) {
            assert false;
        }

    }
}
