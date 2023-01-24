package org.konveyor.dgi.code2graph.utils.graph;

import com.ibm.wala.core.util.strings.Atom;
import org.json.JSONObject;

import java.io.Serializable;

public class CallGraphEdge implements Serializable {
    public final Atom source;
    public final Atom destination;
    public static final long serialVersionUID = -8284030936836318929L;

    public CallGraphEdge(Atom source, Atom destination) {
        this.source = source;
        this.destination = destination;
    }

    @Override
    public String toString() {
        return new JSONObject()
                .put("source", getSource())
                .put("destination", getDestination())
                .toString();
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof ControlDepEdge) && (toString().equals(o.toString()));
    }

    public String getSource() {
        return source.toString();
    }

    public String getDestination() {
        return destination.toString();
    }

}