package org.konveyor.dgi.code2graph.utils.graph;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Edge implements Serializable {

    public final List<String> between;
    public final String context;
    public final String type;

    public Edge(Integer source, Integer destination, String type) {
        this(source, destination, type, null);
    }

    public Edge(Integer source, Integer destination, String type, String context) {
        this.between = new ArrayList<>(Arrays.asList(source.toString(), destination.toString()));
        this.type = type;
        this.context = context;
    }

    public String getType() {
        return type;
    }

    public List<String> getBetween() {
        return between;
    }

    public String getContext() {
        return context;
    }

    @Override
    public String toString() {
        return "{" +
                    ", between: " + between +
                    ", Type: '" + type + '\'' +
                    ", context: '" + context + '\'' +
                '}';
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (!(o instanceof Edge)) return false;

        Edge edge = (Edge) o;

        return new EqualsBuilder().append(between, edge.between).append(getContext(), edge.getContext()).append(getType(), edge.getType()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(between).append(getContext()).append(getType()).toHashCode();
    }
}
