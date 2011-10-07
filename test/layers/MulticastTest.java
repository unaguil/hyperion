package layers;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;

import multicast.search.unicastTable.UnicastTable;
import peer.PeerID;
import testing.BasicTest;
import testing.MultipleTests;

class UTableFileFilter implements FileFilter {

	@Override
	public boolean accept(final File file) {
		return file.getName().contains("UTable");
	}
}

public class MulticastTest extends MultipleTests {

	public MulticastTest() throws Exception {
		super("files/Multicast.xml", new UTableFileFilter());
	}

	@Override
	public Object readObject(final FileInputStream fileInputStream) throws Exception {
		final UnicastTable uTable = new UnicastTable(PeerID.VOID_PEERID);
		uTable.readFromXML(fileInputStream);
		return uTable;
	}

	@Override
	public void check(final BasicTest test) throws Exception {
	}
}
