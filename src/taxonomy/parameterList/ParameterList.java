package taxonomy.parameterList;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import taxonomy.Taxonomy;
import taxonomy.parameter.InvalidParameterIDException;
import taxonomy.parameter.Parameter;
import taxonomy.parameter.ParameterFactory;

public class ParameterList implements Iterable<Parameter> {

	private static final String PARAMETER_TAG = "parameter";
	private static final String PARAMETER_ID_ATTRIB = "id";

	private final List<Parameter> parameters = new ArrayList<Parameter>();

	public ParameterList(final String xmlPath, final Taxonomy taxonomy) throws ParserConfigurationException, SAXException, IOException, InvalidParameterIDException {
		final File f = new File(xmlPath);
		loadData(f, taxonomy);
	}
	
	public ParameterList(final Set<Parameter> parameters) {
		this.parameters.addAll(parameters);
	}

	protected void loadData(final File f, final Taxonomy taxonomy) throws SAXException, IOException, ParserConfigurationException, InvalidParameterIDException {
		if (f.exists()) {
			final Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(f);

			final NodeList nodeList = doc.getElementsByTagName(PARAMETER_TAG);
			for (int i = 0; i < nodeList.getLength(); i++) {
				final Element e = (Element) nodeList.item(i);
				final String id = e.getAttribute(PARAMETER_ID_ATTRIB);
				// Parameters are always read as input parameters
				final Parameter p = ParameterFactory.createParameter(id, taxonomy);
				parameters.add(p);
			}
		}
	}

	@Override
	public Iterator<Parameter> iterator() {
		return parameters.iterator();
	}

	public int size() {
		return parameters.size();
	}

	public boolean isEmpty() {
		return parameters.isEmpty();
	}

	public Set<Parameter> getParameterSet() {
		return new HashSet<Parameter>(this.parameters);
	}
	
	public String pretty(final Taxonomy taxonomy) {
		StringBuffer strBuffer = new StringBuffer();
		strBuffer.append("[");
		int counter = 0;
		for (final Parameter p : parameters) {
			if (counter > 0)
				strBuffer.append(",");
			strBuffer.append(p.pretty(taxonomy));
			counter++;
		}
		strBuffer.append("]");
		return strBuffer.toString();
	}
	
	@Override
	public String toString() {
		return this.parameters.toString();
	}
}
