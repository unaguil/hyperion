package graphsearch.util;

import graphcreation.collisionbased.ServiceDistance;
import graphcreation.services.Service;

import java.util.HashSet;
import java.util.Set;

import peer.peerid.PeerID;
import taxonomy.parameter.InputParameter;
import taxonomy.parameter.OutputParameter;

public class Utility {

	private static final String SEPARATOR = "-";
	public static final String INIT = "INIT";
	public static final String GOAL = "GOAL";

	public static Service createInitService(final Service s, final PeerID peerID) {
		final Service init = new Service(s.getName() + SEPARATOR + INIT, peerID);
		for (final InputParameter input : s.getInputParams())
			init.addParameter(new OutputParameter(input.getID()));
		return init;
	}

	public static Service createGoalService(final Service s, final PeerID peerID) {
		final Service goal = new Service(s.getName() + SEPARATOR + GOAL, peerID);
		for (final OutputParameter output : s.getOutputParams())
			goal.addParameter(new InputParameter(output.getID()));
		return goal;
	}

	private static String getOriginalName(final Service service) {
		return service.getName().substring(0, service.getName().indexOf(SEPARATOR));
	}

	public static boolean connected(final Service initService, final Service goalService) {
		if (!isINITService(initService))
			return false;
		if (!isGoalService(goalService))
			return false;

		return getOriginalName(initService).equals(getOriginalName(goalService)) && initService.getPeerID().equals(goalService.getPeerID());
	}

	public static boolean isINITService(final Service service) {
		return service.getID().contains(INIT);
	}

	public static boolean isGoalService(final Service service) {
		return service.getID().contains(GOAL);
	}

	public static boolean sameComposition(final Service serviceA, final Service serviceB) {
		if (Utility.isINITService(serviceA) && Utility.isGoalService(serviceB))
			return connected(serviceA, serviceB);
		if (Utility.isINITService(serviceB) && Utility.isGoalService(serviceA))
			return connected(serviceB, serviceA);
		return false;
	}

	public static Set<Service> getServices(final Set<ServiceDistance> successors) {
		final Set<Service> services = new HashSet<Service>();
		for (final ServiceDistance successor : successors)
			services.add(successor.getService());
		return services;
	}
}
