package multicast;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import multicast.search.Route;

public class Util {
	
	static class DistanceComparator implements Comparator<Route> {

		@Override
		public int compare(Route routeA, Route routeB) {
			return routeA.getDistance() - routeB.getDistance();		
		}	
	}
	
	private static final DistanceComparator distanceComparator = new DistanceComparator();

	public static Route getShortestRoute(final List<Route> availableRoutes) {		
		//sort available routes using creation timestamp
		Collections.sort(availableRoutes, distanceComparator);
		if (!availableRoutes.isEmpty())
			return availableRoutes.get(0);
		return null;
	}
}
