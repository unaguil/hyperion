package peer;

import java.io.IOException;
import java.util.Set;

import peer.message.BroadcastMessage;
import peer.message.MessageReceivedListener;
import peer.message.MessageSentListener;
import peer.peerid.PeerID;

public interface Peer {

	public void addCommunicationLayer(CommunicationLayer layer, Set<Class<? extends BroadcastMessage>> messageClasses) throws RegisterCommunicationLayerException;

	public void addReceivingListener(Class<? extends BroadcastMessage> messageClass, MessageReceivedListener receivedListener) throws AlreadyRegisteredListenerException;

	public void setHearListener(MessageReceivedListener hearListener);

	public void addSentListener(MessageSentListener sentListener);

	public void directBroadcast(BroadcastMessage message);

	public PeerID getPeerID();

	public void initPeer(PeerID peerID) throws IOException;

	public void stopPeer();

	public void printStatistics();
	
	public boolean isInitialized();
}
