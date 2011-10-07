package peer;

import java.io.IOException;

public interface PeerBehavior {

	public void init() throws IOException;
	
	public void broadcast(byte[] data) throws IOException;
	
	public byte[] receiveData();
	
	public void loadData();
}
