package multicast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import multicast.search.Route;

public class Util {
	
	static class DistanceComparator implements Comparator<Route> {

		@Override
		public int compare(Route routeA, Route routeB) {
			return routeA.getDistance() - routeB.getDistance();		
		}	
	}
	
	public static final DistanceComparator distanceComparator = new DistanceComparator();

	public static Route getShortestRoute(final Set<Route> availableRoutes) {
		final List<Route> routeList = new ArrayList<Route>(availableRoutes);
		//sort available routes using creation timestamp
		Collections.sort(routeList, distanceComparator);
		if (!routeList.isEmpty())
			return routeList.get(0);
		return null;
	}
}
