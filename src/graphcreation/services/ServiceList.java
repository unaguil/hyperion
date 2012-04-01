package graphcreation.services;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
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

import peer.peerid.PeerID;
import taxonomy.Taxonomy;
import taxonomy.parameter.InvalidParameterIDException;
import taxonomy.parameter.Parameter;
import taxonomy.parameter.ParameterFactory;

public class ServiceList implements Iterable<Service> {

	private static final String SERVICE_TAG = "service";
	private static final String PARAMETER_TAG = "parameter";
	private static final String ID_ATTRIB = "id";

	private final List<Service> services = new ArrayList<Service>();

	public ServiceList() {
	}

	public ServiceList(final ServiceList serviceList) {
		for (final Service s : serviceList)
			this.services.add(s);
	}

	public ServiceList(final String xmlPath, final PeerID peer, final Taxonomy taxonomy) throws ParserConfigurationException, SAXException, IOException, InvalidParameterIDException {
		final File f = new File(xmlPath);
		loadData(f, peer, taxonomy);
	}

	protected void loadData(final File f, final PeerID peer, final Taxonomy taxonomy) throws SAXException, IOException, ParserConfigurationException, InvalidParameterIDException {
		if (f.exists()) {
			final Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(f);

			final NodeList serviceNodeList = doc.getElementsByTagName(SERVICE_TAG);
			for (int i = 0; i < serviceNodeList.getLength(); i++) {
				final Element serviceElement = (Element) serviceNodeList.item(i);
				final String serviceID = serviceElement.getAttribute(ID_ATTRIB);
				final Service s = new Service(serviceID, peer);
				services.add(s);

				final NodeList parameterNodeList = serviceElement.getElementsByTagName(PARAMETER_TAG);
				for (int j = 0; j < parameterNodeList.getLength(); j++) {
					final Element parameterElement = (Element) parameterNodeList.item(j);
					final String parameterID = parameterElement.getAttribute(ID_ATTRIB);
					s.addParameter(ParameterFactory.createParameter(parameterID, taxonomy));
				}
			}
		}
	}

	@Override
	public Iterator<Service> iterator() {
		return services.iterator();
	}

	public int size() {
		return services.size();
	}

	public List<Parameter> getParameterList() {
		final Set<Parameter> params = new HashSet<Parameter>();
		for (final Service s : services) {
			params.addAll(s.getInputParams());
			params.addAll(s.getOutputParams());
		}

		return new ArrayList<Parameter>(params);
	}

	public Service getService(final int index) {
		return services.get(index);
	}

	public void addService(final Service service) {
		services.add(service);
	}
	
	public List<Service> getServiceList() {
		return Collections.unmodifiableList(services);
	}

	@Override
	public String toString() {
		return services.toString();
	}
}
