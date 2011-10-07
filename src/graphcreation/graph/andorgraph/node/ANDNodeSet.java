package graphcreation.graph.andorgraph.node;

import java.util.Set;

public interface ANDNodeSet<A extends ANDNode> extends ANDNode {

	public Set<A> getInnerSet();
}
