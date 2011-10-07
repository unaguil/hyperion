package taxonomy;

import serialization.xml.XMLSerialization;

public interface Taxonomy extends XMLSerialization {

	public void setRoot(String rootID);

	public String getRoot();

	public boolean addChild(String parentID, String childID) throws TaxonomyException;

	public String getParent(String id);

	public boolean subsumes(String idA, String idB);

	public boolean areRelated(String idA, String idB);
}