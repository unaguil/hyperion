package graphsearch.shortestpathnotificator;

import graphcreation.GraphCreator;
import graphcreation.services.Service;
import graphsearch.bidirectionalsearch.message.ShortestPathNotificationMessage;

import java.util.Collections;

import peer.peerid.PeerID;
import util.logger.Logger;

public class ShortestPathNotificator {

	private final PeerID peerID;

	private final GraphCreator gCreator;

	private final ShortestPathListener listener;
	
	private final boolean directBroadcast;
	
	private final boolean multiplePaths;

	private final Logger logger = Logger.getLogger(ShortestPathNotificator.class);

	public ShortestPathNotificator(final PeerID peerID, final GraphCreator gCreator,
								    final ShortestPathListener listener, final boolean directBroadcast,
								    final boolean multiplePaths) {
		this.peerID = peerID;
		this.gCreator = gCreator;
		this.listener = listener;
		this.directBroadcast = directBroadcast;
		this.multiplePaths = multiplePaths;
	}

	public void processShortestPathNotificationMessage(final ShortestPathNotificationMessage shortestPathNotificationMessage) {

		logger.trace("Peer " + peerID + " processing composition modification message " + shortestPathNotificationMessage);

		final Service destination = shortestPathNotificationMessage.getDestination();

		if (destination.getPeerID().equals(peerID)) {
			listener.acceptShortestPathNotificationMessage(shortestPathNotificationMessage);
			return;
		}

		final int pathDistance = shortestPathNotificationMessage.getPathDistance();

		// check if the current peer has a shortest route to the destination
		if (gCreator.getPSearch().knowsRouteTo(destination.getPeerID())) {
			final int multicastDistance = gCreator.getPSearch().getRoute(destination.getPeerID()).getDistance();
			if (multicastDistance <= pathDistance) {

				logger.trace("Peer " + peerID + " has a shortest route to destination peer " + destination.getPeerID());
				gCreator.forwardMessage(shortestPathNotificationMessage, Collections.singleton(destination), directBroadcast, multiplePaths);
				return;
			}
		}

		// propagate through next service
		final Service nextService = shortestPathNotificationMessage.nextService();

		logger.trace("Peer " + peerID + " forwarding composition through next service " + nextService);

		gCreator.forwardMessage(shortestPathNotificationMessage, Collections.singleton(nextService), directBroadcast, multiplePaths);
	}
}
