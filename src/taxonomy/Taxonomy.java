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

package taxonomy;

import serialization.xml.XMLSerializable;

public interface Taxonomy extends XMLSerializable {

	public void setRoot(String rootID);

	public String getRoot();

	public void addChild(String parentID, String childID) throws TaxonomyException;

	public String getParent(String id) throws TaxonomyException;

	public boolean subsumes(short idA, short idB);

	public boolean areRelated(short idA, short idB);
	
	public short encode(String id);
	
	public String decode(short value);
}