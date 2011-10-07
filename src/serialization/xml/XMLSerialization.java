package serialization.xml;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface XMLSerialization {

	/**
	 * Saves the information to XML serialization
	 * 
	 * @param os
	 *            the output stream where the XML is written to
	 * @throws IOException
	 *             thrown if there is some problem writing the XML
	 */
	public void saveToXML(OutputStream os) throws IOException;

	/**
	 * Reads the information from an XML serialization.
	 * 
	 * @param is
	 *            the input stream the XML is read from
	 * @throws IOException
	 *             thrown if there is some problem reading from the XML
	 */
	public void readFromXML(InputStream is) throws IOException;
}
