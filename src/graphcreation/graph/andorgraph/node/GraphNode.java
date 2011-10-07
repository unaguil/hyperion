package graphcreation.graph.andorgraph.node;

import java.io.Serializable;

public interface GraphNode extends Serializable {

	public String getNodeID();

	public GraphNode copy();
}
