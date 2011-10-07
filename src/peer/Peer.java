package peer;

import java.util.Set;

import message.BroadcastMessage;
import message.MessageReceivedListener;
import message.MessageSentListener;
import detection.NeighborDetector;

public interface Peer {

	public static final boolean USE_RELIABLE_BROADCAST = true;

	public void addCommunicationLayer(CommunicationLayer layer, Set<Class<? extends BroadcastMessage>> messageClasses) throws RegisterCommunicationLayerException;

	public void addReceivingListener(Class<? extends BroadcastMessage> messageClass, MessageReceivedListener receivedListener) throws AlreadyRegisteredListenerException;

	public void setHearListener(MessageReceivedListener hearListener);

	public void addSentListener(MessageSentListener sentListener);

	public void broadcast(BroadcastMessage message);

	public void enqueueBroadcast(BroadcastMessage message);

	public PeerID getPeerID();

	public int getPort();

	public NeighborDetector getDetector();

	public void processMessage(BroadcastMessage message);
}
