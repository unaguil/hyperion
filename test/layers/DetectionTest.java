package layers;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;

import peer.PeerIDSet;
import testing.BasicTest;
import testing.MultipleTests;

class NeighboursFileFilter implements FileFilter {

	@Override
	public boolean accept(final File file) {
		return file.getName().contains("Neighbours");
	}
}

public class DetectionTest extends MultipleTests {

	public DetectionTest() {
		super("files/Detection.xml", new NeighboursFileFilter());
	}

	@Override
	public Object readObject(final FileInputStream fileInputStream) throws Exception {
		final PeerIDSet peerIDSet = new PeerIDSet();
		peerIDSet.readFromXML(fileInputStream);
		return peerIDSet;
	}

	@Override
	public void check(final BasicTest test) throws Exception {
	}
}
