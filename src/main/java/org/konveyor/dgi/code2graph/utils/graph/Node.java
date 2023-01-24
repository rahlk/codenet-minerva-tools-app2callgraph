package org.konveyor.dgi.code2graph.utils.graph;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.shrike.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAInstruction;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.Serializable;

public class Node implements Serializable {

    public final Integer id;
    public final Integer instructionPosition;
    public final String instructionMethod;
    public final String instructionClass;

    public Node(Integer id, Statement statement) {
        this.id = id;
        this.instructionMethod = statement.getNode().getMethod().getName().toString();
        this.instructionClass = statement.getNode().getMethod().getDeclaringClass().getName().toString();
        this.instructionPosition = getStatementPosition(statement);
    }

    private Integer getStatementPosition(Statement statement) {
        CGNode statementNode = statement.getNode();
        IR statementIR = statementNode.getIR();
        int pos = -1;
        // TODO: check this assumption: the same source instruction maps to several SSAInstructions,
        //  therefore it is sufficient to return the position of the first statement.
        for (SSAInstruction inst : statementNode.getIR().getInstructions()) {
            try {
                pos = statementIR.getMethod().getSourcePosition(inst.iIndex()).getLastLine();
                return pos;
            } catch (InvalidClassFileException e) {
                throw new RuntimeException(e);
            } catch (NullPointerException npe) {
                return -1;
            }
        }
        return pos;
    }

    @Override
    public String toString() {
        return "{" +
                "position: " + instructionPosition +
                ", method: '" + instructionMethod + '\'' +
                ", class: '" + instructionClass + '\'' +
                '}';
    }

    public String getInstructionClass() {
        return instructionClass.substring(1).replace("/", ".");
    }

    public String getInstructionMethod() {
        return instructionMethod;
    }

    public Integer getInstructionPosition() {
        return instructionPosition;
    }

    public Integer getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (!(o instanceof Node)) return false;

        Node node = (Node) o;

        return new EqualsBuilder().append(id, node.id).append(getInstructionPosition(), node.getInstructionPosition()).append(getInstructionMethod(), node.getInstructionMethod()).append(getInstructionClass(), node.getInstructionClass()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(id).append(getInstructionPosition()).append(getInstructionMethod()).append(getInstructionClass()).toHashCode();
    }

}
