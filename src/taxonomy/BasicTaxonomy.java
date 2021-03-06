/*
*   Copyright (c) 2012 Unai Aguilera
*
*   Licensed under the Apache License, Version 2.0 (the "License");
*   you may not use this file except in compliance with the License.
*   You may obtain a copy of the License at
*
*       http://www.apache.org/licenses/LICENSE-2.0
*
*   Unless required by applicable law or agreed to in writing, software
*   distributed under the License is distributed on an "AS IS" BASIS,
*   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*   See the License for the specific language governing permissions and
*   limitations under the License.
*
*  
*   Author: Unai Aguilera <unai.aguilera@deusto.es>
*/

package taxonomy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class BasicTaxonomy implements Taxonomy {

	private static final String TAXONOMY = "taxonomy";
	private static final String ROOT = "root";
	private static final String ELEMENT = "element";
	private static final String CHILD = "child";
	private static final String ID = "id";

	private TaxonomyElement root = null;

	private final Map<String, TaxonomyElement> allTElements = new TreeMap<String, TaxonomyElement>();
	
	public static final String NONE = "NONE";
	private static final TaxonomyElement NONE_ELEMENT = new TaxonomyElement(NONE, null);

	public BasicTaxonomy() {
	}

	@Override
	public void setRoot(final String rootID) {
		this.root = new TaxonomyElement(rootID, NONE_ELEMENT);
		allTElements.put(root.getID(),root);
	}

	@Override
	public String getRoot() {
		return root.getID();
	}

	@Override
	public void addChild(final String parentID, final String childID) throws TaxonomyException {
		final TaxonomyElement parent = getTElement(parentID);
		final TaxonomyElement child = parent.addChild(childID);
		allTElements.put(child.getID(), child);
	}

	@Override
	public String getParent(final String id) throws TaxonomyException {
		final TaxonomyElement tElement = getTElement(id);
		return tElement.getParent().getID();
	}

	@Override
	public boolean subsumes(final short valueA, final short valueB) {
		final String idA = decode(valueA);
		final String idB = decode(valueB);
		
		if (idA.equals("") || idB.equals(""))
			return false;
		
		if (idA.equals(idB))
			return true;

		String parent = idB;
		try {
			do
				parent = getParent(parent);
			while (parent != null && !parent.equals(idA));
	
			return parent != null;
		} catch (TaxonomyException e) {
			return false;
		}
	}

	@Override
	public boolean areRelated(final short valueA, final short valueB) {
		return subsumes(valueA, valueB) || subsumes(valueB, valueA);
	}

	@Override
	public String toString() {
		final StringBuilder strBuilder = new StringBuilder();
		if (root != null) {
			strBuilder.append("Root: " + root.toString());
			printRecursive(root, strBuilder);
		} else
			strBuilder.append("Root: " + NONE);

		return strBuilder.toString();
	}

	@Override
	public void readFromXML(final InputStream is) throws IOException {
		DocumentBuilder docBuilder = null;
		try {
			docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		} catch (final ParserConfigurationException pce) {
			throw new IOException(pce);
		}

		Document document = null;
		try {
			document = docBuilder.parse(is);
		} catch (final SAXException e) {
			throw new IOException(e);
		}

		final Element docRoot = document.getDocumentElement();

		setRoot(docRoot.getAttribute(ROOT));

		final NodeList elementList = document.getElementsByTagName(ELEMENT);
		for (int i = 0; i < elementList.getLength(); i++) {
			final Element element = (Element) elementList.item(i);
			final NodeList childList = element.getElementsByTagName(CHILD);
			for (int j = 0; j < childList.getLength(); j++) {
				final Element child = (Element) childList.item(j);
				try {
					addChild(element.getAttribute(ID), child.getAttribute(ID));
				} catch (final TaxonomyException te) {
					throw new IOException(te);
				}
			}
		}
	}

	@Override
	public void saveToXML(final OutputStream os) throws IOException {
		Document doc = null;
		try {
			doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		} catch (final ParserConfigurationException pce) {
			throw new IOException(pce);
		}

		final Element docRoot = doc.createElement(TAXONOMY);
		docRoot.setAttribute(ROOT, getRoot());
		doc.appendChild(docRoot);

		printRecursiveToXML(this.root, docRoot, doc);

		Transformer transformer = null;
		try {
			transformer = TransformerFactory.newInstance().newTransformer();
		} catch (final TransformerConfigurationException tce) {
			throw new IOException(tce);
		}

		final DOMSource source = new DOMSource(doc);
		final StreamResult result = new StreamResult(os);
		try {
			transformer.transform(source, result);
		} catch (final TransformerException te) {
			throw new IOException(te);
		}
	}

	private void printRecursiveToXML(final TaxonomyElement current, final Element docRoot, final Document doc) {
		final Element xmlElement = doc.createElement(ELEMENT);
		xmlElement.setAttribute(ID, current.getID());
		docRoot.appendChild(xmlElement);

		for (final TaxonomyElement child : current.childs()) {
			final Element xmlChild = doc.createElement(CHILD);
			xmlChild.setAttribute(ID, child.getID());
			xmlElement.appendChild(xmlChild);
		}

		for (final TaxonomyElement child : current.childs())
			printRecursiveToXML(child, docRoot, doc);
	}

	private void printRecursive(final TaxonomyElement tElement, final StringBuilder strBuilder) {
		strBuilder.append(" [" + tElement.getID() + "]: ");
		boolean first = true;
		for (final TaxonomyElement child : tElement.childs())
			if (first) {
				strBuilder.append(child);
				first = false;
			} else
				strBuilder.append(", " + child);

		for (final TaxonomyElement child : tElement.childs())
			printRecursive(child, strBuilder);
	}

	private TaxonomyElement getTElement(final String id) throws TaxonomyException {
		final TaxonomyElement tElement = allTElements.get(id);
		if (tElement == null)
			throw new TaxonomyException("Element with id " + id + " not found. It should be previously added");
		return tElement;
	}

	@Override
	public boolean equals(final Object o) {
		if (!(o instanceof BasicTaxonomy))
			return false;

		final BasicTaxonomy taxonomy = (BasicTaxonomy) o;
		if (this.allTElements.size() != taxonomy.allTElements.size())
			return false;

		// Compare all elements
		final Iterator<TaxonomyElement> otherIt = taxonomy.allTElements.values().iterator();
		for (final TaxonomyElement aElement : this.allTElements.values()) {
			final TaxonomyElement otherElement = otherIt.next();
			if (!aElement.equals(otherElement))
				return false;
			else if (!aElement.getParent().equals(otherElement.getParent()))
				return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		return allTElements.hashCode();
	}

	@Override
	public short encode(final String id) {
		 short counter = 1;
		 for (final String key : allTElements.keySet()) {
			 if (key.equals(id))
				 return counter;
			 counter++;
		 }
		 
		 return (short)(-1 * Short.parseShort(id));
	}

	@Override
	public String decode(final short value) {
		if (value > 0) {
			final Vector<String> keys = new Vector<String>(allTElements.keySet());
			if (keys.isEmpty())
				return "";
			return keys.elementAt(value - 1);
		}
		
		return String.valueOf(-1 * value);
	}
}
