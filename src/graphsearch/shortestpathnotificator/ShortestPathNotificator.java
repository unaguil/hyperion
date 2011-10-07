package graphsearch.shortestpathnotificator;

import graphcreation.GraphCreator;
import graphcreation.services.Service;
import graphsearch.bidirectionalsearch.message.ShortestPathNotificationMessage;

import java.util.Collections;

import peer.PeerID;
import util.logger.Logger;

public class ShortestPathNotificator {

	private final PeerID peerID;

	private final GraphCreator gCreator;

	private final ShortestPathListener listener;

	private final Logger logger = Logger.getLogger(ShortestPathNotificator.class);

	public ShortestPathNotificator(final PeerID peerID, final GraphCreator gCreator, final ShortestPathListener listener) {
		this.peerID = peerID;
		this.gCreator = gCreator;
		this.listener = listener;
	}

	public void processShortestPathNotificationMessage(final ShortestPathNotificationMessage shortestPathNotificationMessage) {
		if (Logger.TRACE)
			logger.trace("Peer " + peerID + " processing composition modification message " + shortestPathNotificationMessage);

		final Service destination = shortestPathNotificationMessage.getDestination();

		if (destination.getPeerID().equals(peerID)) {
			listener.acceptShortestPathNotificationMessage(shortestPathNotificationMessage);
			return;
		}

		final int pathDistance = shortestPathNotificationMessage.getPathDistance();

		// check if the current peer has a shortest route to the destination
		if (gCreator.getPSearch().knowsSearchRouteTo(destination.getPeerID())) {
			final int multicastDistance = gCreator.getPSearch().getDistanceTo(destination.getPeerID());
			if (multicastDistance <= pathDistance) {
				if (Logger.TRACE)
					logger.trace("Peer " + peerID + " has a shortest route to destination peer " + destination.getPeerID());
				gCreator.forwardMessage(shortestPathNotificationMessage, Collections.singleton(destination));
				return;
			}
		}

		// propagate through next service
		final Service nextService = shortestPathNotificationMessage.nextService();

		if (Logger.TRACE)
			logger.trace("Peer " + peerID + " forwarding composition through next service " + nextService);

		gCreator.forwardMessage(shortestPathNotificationMessage, Collections.singleton(nextService));
	}
}
