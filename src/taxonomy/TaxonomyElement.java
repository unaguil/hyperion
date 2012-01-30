package taxonomy;

import java.util.HashSet;
import java.util.Set;

class TaxonomyElement {

	private final String elementID;

	private final TaxonomyElement parent;

	private final Set<TaxonomyElement> childs = new HashSet<TaxonomyElement>();

	public TaxonomyElement(final String elementID, final TaxonomyElement parent) {
		this.elementID = elementID;
		this.parent = parent;
	}

	public String getID() {
		return elementID;
	}

	public Set<TaxonomyElement> childs() {
		return childs;
	}

	public TaxonomyElement addChild(final String childID) {
		final TaxonomyElement tElement = new TaxonomyElement(childID, this);
		childs.add(tElement);
		return tElement;
	}

	public boolean removeChild(final String childID) {
		return childs.remove(new TaxonomyElement(childID, this));
	}

	public TaxonomyElement getParent() {
		return parent;
	}

	@Override
	public boolean equals(final Object o) {
		if (!(o instanceof TaxonomyElement))
			return false;

		final TaxonomyElement tElement = (TaxonomyElement) o;
		return this.elementID.equals(tElement.elementID);
	}

	@Override
	public int hashCode() {
		return elementID.hashCode();
	}

	@Override
	public String toString() {
		return elementID;
	}
}