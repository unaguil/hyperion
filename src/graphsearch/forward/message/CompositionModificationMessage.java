package graphsearch.forward.message;

import graphcreation.collisionbased.ServiceDistance;
import graphcreation.services.Service;
import graphsearch.SearchID;
import graphsearch.bidirectionalsearch.message.ShortestPathNotificationMessage;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import peer.message.PayloadMessage;
import peer.peerid.PeerID;

public class CompositionModificationMessage extends ShortestPathNotificationMessage {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final Set<Service> removedServices = new HashSet<Service>();
	
	public CompositionModificationMessage() {
		
	}

	public CompositionModificationMessage(final PeerID source, final SearchID searchID, final Set<Service> removedServices, final Map<Service, Set<ServiceDistance>> serviceDistances, final List<Service> notificationPath) {
		super(source, searchID, serviceDistances, notificationPath);
		this.removedServices.addAll(removedServices);
	}

	private CompositionModificationMessage(final PeerID source, final SearchID searchID, final Set<Service> removedServices, final Map<Service, Set<ServiceDistance>> serviceDistances, final List<Service> notificationPath, final Service destination) {
		super(source, searchID, serviceDistances, notificationPath, destination);
		this.removedServices.addAll(removedServices);
	}

	public Set<Service> getRemovedServices() {
		return removedServices;
	}

	@Override
	public PayloadMessage copy() {
		final CompositionModificationMessage compositionModificationMessage = new CompositionModificationMessage(getSource(), getSearchID(), removedServices, serviceDistances, notificationPath, destination);
		return compositionModificationMessage;
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		super.readExternal(in);
		
		removedServices.addAll(Arrays.asList((Service[])in.readObject()));
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);
		
		out.writeObject(removedServices.toArray(new Service[0]));
	}
}
