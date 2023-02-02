package com.ibm.minerva.app2callgraph.entities;

import java.io.Serializable;

public class CallGraphNode implements Serializable {
    public final String className;
    public final String classShortName;
    public final Boolean isPrivate;
    // Fields
    public final Integer num_fields;
    public final Integer num_static_fields;
    public final Integer num_instance_fields;
    // Methods
    public final Integer num_static_methods;
    public final Integer num_declared_methods;

    public CallGraphNode(String name, Boolean isPrivate, Integer num_fields, Integer num_static_fields,
            Integer num_instance_fields, Integer num_static_methods, Integer num_declared_methods) {

        this.className = name.substring(1).replace("/", ".");
        String classShortName = this.className.substring(this.className.lastIndexOf('.') + 1);
        this.classShortName = classShortName.replace("$", "_");
        this.isPrivate = isPrivate;
        this.num_fields = num_fields;
        this.num_static_fields = num_static_fields;
        this.num_instance_fields = num_instance_fields;
        this.num_static_methods = num_static_methods;
        this.num_declared_methods = num_declared_methods;
    }

    public Boolean getIsPrivate() {
        return isPrivate;
    }

    public Integer getNum_fields() {
        return num_fields;
    }

    public Integer getNum_static_fields() {
        return num_static_fields;
    }

    public Integer getNum_instance_fields() {
        return num_instance_fields;
    }

    public Integer getNum_static_methods() {
        return num_static_methods;
    }

    public Integer getNum_declared_methods() {
        return num_declared_methods;
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
        return (o instanceof CallGraphNode) && (toString().equals(o.toString()));
    }

    public String getClassName() {
        return className;
    }

    public String getClassShortName() {
        return classShortName;
    }
}
