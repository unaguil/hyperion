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
	public boolean subsumes(final short valueA, final short valueB) {
		return taxonomy.subsumes(valueA, valueB);
	}

	@Override
	public boolean areRelated(final short valueA, final short valueB) {
		return taxonomy.areRelated(valueA, valueB);
	}

	@Override
	public short encode(String id) {
		return taxonomy.encode(id);
	}

	@Override
	public String decode(short value) {
		return taxonomy.decode(value);
	}
}
