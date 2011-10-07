package layers;

import java.io.File;
import java.io.FileFilter;

class GraphFileFilter implements FileFilter {

	@Override
	public boolean accept(final File file) {
		return file.getName().contains("Graph");
	}
}