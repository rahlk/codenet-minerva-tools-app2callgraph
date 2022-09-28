package io.tackle.dgi.code2graph.utils;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;

import com.ibm.wala.ipa.callgraph.CallGraph;
import org.jgrapht.graph.DefaultEdge;

import java.util.Iterator;
import java.util.Set;

public class CallGraphUtil {
    public static void callgraph2Json(CallGraph callGraph) {
        Graph<String, DefaultEdge> g = new DefaultDirectedGraph<>(DefaultEdge.class);
        for (CGNode entrypointNode : callGraph.getEntrypointNodes()) {
            IMethod entryMethod = entrypointNode.getMethod();
            // Get call statements that may execute in a given method
            Iterator<CallSiteReference> outGoingCalls = entrypointNode.iterateCallSites();
            for (Iterator<CallSiteReference> it = outGoingCalls; it.hasNext();) {
                CallSiteReference callSiteReference = it.next();
                for (CGNode callTarget : callGraph.getPossibleTargets(entrypointNode, callSiteReference)) {
                    Log.info(entryMethod.getSignature() + "\t-->\t" + callTarget.getMethod().getSignature());
                }
            }

        }

    }

    private static void addVerticesToGraph(Graph g) {
        return;
    }
}
