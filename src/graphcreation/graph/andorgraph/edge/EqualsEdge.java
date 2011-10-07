package graphcreation.graph.andorgraph.edge;

import org.jgrapht.graph.DefaultWeightedEdge;

public class EqualsEdge extends DefaultWeightedEdge {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public boolean equals(final Object o) {
		if (!(o instanceof EqualsEdge))
			return false;

		final EqualsEdge equalsEdge = (EqualsEdge) o;
		return this.getSource().equals(equalsEdge.getSource()) && this.getTarget().equals(equalsEdge.getTarget());
	}

	@Override
	public Object getTarget() {
		return super.getTarget();
	}

	@Override
	public Object getSource() {
		return super.getSource();
	}

	@Override
	public int hashCode() {
		int result = 17;

		if (this.getSource() != null && this.getTarget() != null) {
			result = 31 * result + this.getSource().hashCode();
			result = 31 * result + this.getTarget().hashCode();
		}

		return result;
	}
}
