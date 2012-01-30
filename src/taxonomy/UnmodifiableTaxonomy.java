package taxonomy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class UnmodifiableTaxonomy implements Taxonomy {

	private final Taxonomy taxonomy;

	public UnmodifiableTaxonomy(final Taxonomy taxonomy) {
		this.taxonomy = taxonomy;
	}

	@Override
	public void saveToXML(final OutputStream os) throws IOException {
		taxonomy.saveToXML(os);
	}

	@Override
	public void readFromXML(final InputStream is) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setRoot(final String rootID) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getRoot() {
		return taxonomy.getRoot();
	}

	@Override
	public void addChild(final String parentID, final String childID) throws TaxonomyException {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getParent(final String id) throws TaxonomyException{
		return taxonomy.getParent(id);
	}

	@Override
	public boolean subsumes(final String idA, final String idB) {
		return taxonomy.subsumes(idA, idB);
	}

	@Override
	public boolean areRelated(final String idA, final String idB) {
		return taxonomy.areRelated(idA, idB);
	}
}
