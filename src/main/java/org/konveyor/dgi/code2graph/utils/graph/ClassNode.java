package org.konveyor.dgi.code2graph.utils.graph;

import java.io.Serializable;

public class ClassNode implements Serializable {
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
        return (o instanceof ClassNode) && (toString().equals(o.toString()));
    }

    public String getClassName() {
        return className;
    }

    public String getClassShortName() {
        return classShortName;
    }
}

