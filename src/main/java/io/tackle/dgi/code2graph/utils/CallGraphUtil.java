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

package io.tackle.dgi.code2graph.utils;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.util.strings.Atom;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.Iterator;
import java.util.LinkedHashMap;

import org.json.JSONObject;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.nio.DefaultAttribute;
import org.jgrapht.nio.Attribute;
import org.jgrapht.nio.dot.DOTExporter;
import org.jgrapht.nio.json.JSONExporter;
import org.jgrapht.nio.graphml.GraphMLExporter;

public class CallGraphUtil {
    static class ClassNode implements Serializable {
        public final String className;
        public final String classShortName;

        public ClassNode(String name) {
            this.className = name.substring(1).replace("/", ".");
            String classShortName = this.className.substring(this.className.lastIndexOf('.') + 1);
            this.classShortName = classShortName.replace("$", "_");
        }

        @Override
        public String toString() {
            return className;
        }

        @Override
        public int hashCode() {
            return toString().hashCode();
        }

        @Override
        public boolean equals(Object o) {
            return (o instanceof CallGraphEdge) && (toString().equals(o.toString()));
        }
    }

    static class CallGraphEdge implements Serializable {
        public final Atom from;
        public final Atom to;
        public static final long serialVersionUID = -8284030936836318929L;

        public CallGraphEdge(Atom from, Atom to) {
            this.from = from;
            this.to = to;
        }

        @Override
        public String toString() {
            return new JSONObject()
                    .put("from", this.from.toString())
                    .put("to", this.to.toString())
                    .toString();
        }

        @Override
        public int hashCode() {
            return toString().hashCode();
        }

        @Override
        public boolean equals(Object o) {
            return (o instanceof CallGraphEdge) && (toString().equals(o.toString()));
        }
    }

    private static Graph<ClassNode, CallGraphEdge> getDefaultDirectedGraph(CallGraph callGraph) {
        Graph<ClassNode, CallGraphEdge> graph = new DefaultDirectedGraph<>(CallGraphEdge.class);
        for (CGNode entrypointNode : callGraph.getEntrypointNodes()) {
            IMethod entryMethod = entrypointNode.getMethod();
            // Get call statements that may execute in a given method
            Iterator<CallSiteReference> outGoingCalls = entrypointNode.iterateCallSites();
            for (Iterator<CallSiteReference> it = outGoingCalls; it.hasNext();) {
                CallSiteReference callSiteReference = it.next();
                for (CGNode callTarget : callGraph.getPossibleTargets(entrypointNode, callSiteReference)) {
                    if (AnalysisUtils.isApplicationClass(callTarget.getMethod().getDeclaringClass())) {
                        ClassNode source = new ClassNode(entryMethod.getDeclaringClass().getName().toString());
                        ClassNode target = new ClassNode(
                                callTarget.getMethod().getDeclaringClass().getName().toString());
                        if (source.equals(target)) {
                            continue;
                        }
                        graph.addVertex(source);
                        graph.addVertex(target);
                        graph.addEdge(
                                source,
                                target,
                                new CallGraphEdge(entryMethod.getName(), callTarget.getMethod().getName()));
                    }
                }
            }
        }
        return graph;
    }

    public static void callgraph2GraphML(CallGraph callGraph, String savePath) {
        Graph<ClassNode, CallGraphEdge> graph = getDefaultDirectedGraph(callGraph);
        GraphMLExporter<ClassNode, CallGraphEdge> exporter = new GraphMLExporter<>(v -> v.className);
        exporter.setVertexAttributeProvider((v) -> {
            Map<String, Attribute> map = new LinkedHashMap<>();
            map.put("label", DefaultAttribute.createAttribute(v.toString()));
            return map;
        });
        // Export the graph to GraphML
        exporter.exportGraph(graph, new File(savePath));
    }

    public static void callgraph2JSON(CallGraph callGraph, String savePath) {
        Graph<ClassNode, CallGraphEdge> graph = getDefaultDirectedGraph(callGraph);
        JSONExporter<ClassNode, CallGraphEdge> exporter = new JSONExporter<>(v -> v.className);
        exporter.setVertexAttributeProvider((v) -> {
            Map<String, Attribute> map = new LinkedHashMap<>();
            map.put("label", DefaultAttribute.createAttribute(v.toString()));
            return map;
        });
        // Export the graph to JSON
        exporter.exportGraph(graph, new File(savePath));
    }

    public static void callgraph2DOT(CallGraph callGraph, String savePath) {
        Graph<ClassNode, CallGraphEdge> graph = getDefaultDirectedGraph(callGraph);
        DOTExporter<ClassNode, CallGraphEdge> exporter = new DOTExporter<>(v -> v.classShortName);
        exporter.setVertexAttributeProvider((v) -> {
            Map<String, Attribute> map = new LinkedHashMap<>();
            map.put("label", DefaultAttribute.createAttribute(v.toString()));
            return map;
        });
        // Export the graph to DOT
        exporter.exportGraph(graph, new File(savePath));
    }
}
