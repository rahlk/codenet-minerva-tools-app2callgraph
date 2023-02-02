package com.ibm.minerva.app2callgraph.entities;

import com.ibm.wala.core.util.strings.Atom;
import org.json.JSONObject;

import java.io.Serializable;

public class CallGraphEdge implements Serializable {
    public Double weight;
    public final Atom source;
    public final Atom destination;
    public static final long serialVersionUID = -8284030936836318929L;

    public CallGraphEdge(Atom source, Atom destination) {
        this.source = source;
        this.destination = destination;
        this.weight = 1.0;
    }

    public CallGraphEdge(Atom source, Atom destination, Double weight) {
        this.source = source;
        this.destination = destination;
        this.weight = weight;
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
        return (o instanceof CallGraphEdge) && (toString().equals(o.toString()));
    }

    public String getSource() {
        return source.toString();
    }

    public void incrementWeight() {
        this.weight += 1;
    }

    public double getWeight() {
        return this.weight;
    }

    public String getDestination() {
        return destination.toString();
    }

    public void setWeight(Double weight) {
        this.weight = weight;
    }

}