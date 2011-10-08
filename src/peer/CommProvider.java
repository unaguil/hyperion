package peer;

import java.io.IOException;

import peer.message.BroadcastMessage;

public interface CommProvider {

	public void initComm() throws IOException;
	
	public void broadcast(byte[] data) throws IOException;
	
	public byte[] receiveData() throws IOException;
	
	public void stopComm() throws IOException;

	public boolean isValid(BroadcastMessage message);
}
