package graphcreation.graph.andorgraph.node;

import java.util.Set;

public interface ANDNodeFactory<A extends ANDNode, S extends ANDNodeSet<A>> {

	public S create(Set<A> set);
}
