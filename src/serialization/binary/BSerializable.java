package serialization.binary;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public interface BSerializable {

	public void write(ObjectOutputStream out) throws IOException;
	
	public void read(ObjectInputStream in) throws IOException;
}
