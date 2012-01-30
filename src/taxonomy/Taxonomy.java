package taxonomy;

import serialization.xml.XMLSerializable;

public interface Taxonomy extends XMLSerializable {

	public void setRoot(String rootID);

	public String getRoot();

	public void addChild(String parentID, String childID) throws TaxonomyException;

	public String getParent(String id) throws TaxonomyException;

	public boolean subsumes(String idA, String idB);

	public boolean areRelated(String idA, String idB);
}