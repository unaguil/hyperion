package peer;

import java.io.IOException;

public interface PeerBehavior {

	public void initCommunication() throws IOException;
	
	public void broadcast(byte[] data) throws IOException;
	
	public byte[] receiveData() throws IOException;
	
	public void loadData();
}
