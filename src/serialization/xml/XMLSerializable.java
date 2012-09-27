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
*   Author: Unai Aguilera <gkalgan@gmail.com>
*/

package serialization.xml;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface XMLSerializable {

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
