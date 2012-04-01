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