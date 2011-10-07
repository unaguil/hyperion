package layers;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;

import peer.PeerID;
import taxonomy.BasicTaxonomy;
import taxonomy.Taxonomy;
import testing.BasicTest;
import testing.MultipleTests;
import dissemination.newProtocol.ptable.ParameterTable;

class PTableFileFilter implements FileFilter {

	@Override
	public boolean accept(final File file) {
		return file.getName().contains("PTable");
	}
}

public class DisseminationTest extends MultipleTests {

	private final Taxonomy emptyTaxonomy = new BasicTaxonomy();

	public DisseminationTest() throws Exception {
		super("files/Dissemination.xml", new PTableFileFilter());
	}

	@Override
	public Object readObject(final FileInputStream fileInputStream) throws Exception {
		final ParameterTable pTable = new ParameterTable(0, PeerID.VOID_PEERID, emptyTaxonomy);
		pTable.readFromXML(fileInputStream);
		return pTable;
	}

	@Override
	public void check(final BasicTest test) throws Exception {
	}
}
