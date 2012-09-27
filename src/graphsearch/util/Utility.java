/*
*   Copyright (c) 2012 Unai Aguilera
*
*   Licensed under the Apache License, Version 2.0 (the "License");
*   you may not use this file except in compliance with the License.
*   You may obtain a copy of the License at
*
*       http://www.apache.org/licenses/LICENSE-2.0
*
*   Unless required by applicable law or agreed to in writing, software
*   distributed under the License is distributed on an "AS IS" BASIS,
*   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*   See the License for the specific language governing permissions and
*   limitations under the License.
*
*  
*   Author: Unai Aguilera <unai.aguilera@deusto.es>
*/

package graphsearch.util;

import graphcreation.collisionbased.ServiceDistance;
import graphcreation.services.Service;
import graphsearch.SearchID;

import java.util.HashSet;
import java.util.Set;

import taxonomy.parameter.InputParameter;
import taxonomy.parameter.OutputParameter;

public class Utility {

	private static final String SEPARATOR = "-";
	public static final String INIT = "INIT";
	public static final String GOAL = "GOAL";

	public static Service createInitService(final Service s, final SearchID searchID) {
		final Service init = new Service(s.getName() + SEPARATOR + INIT + SEPARATOR + searchID.getID(), searchID.getPeer());
		for (final InputParameter input : s.getInputParams())
			init.addParameter(new OutputParameter(input.getID()));
		return init;
	}

	public static Service createGoalService(final Service s, final SearchID searchID) {
		final Service goal = new Service(s.getName() + SEPARATOR + GOAL + SEPARATOR + searchID.getID(), searchID.getPeer());
		for (final OutputParameter output : s.getOutputParams())
			goal.addParameter(new InputParameter(output.getID()));
		return goal;
	}

	private static String getOriginalName(final Service service) {
		return service.getName().substring(0, service.getName().indexOf(SEPARATOR));
	}
	
	public static SearchID getSearchID(final Service service) {
		final int firstSeparator = service.getName().indexOf(SEPARATOR);
		final String substr = service.getName().substring(firstSeparator + 1, service.getName().length());
		final int secondSeparator = substr.indexOf(SEPARATOR);
		return new SearchID(service.getPeerID(), Short.parseShort(substr.substring(secondSeparator + 1, substr.length())));
	}

	public static boolean connected(final Service initService, final Service goalService) {
		if (!isINITService(initService))
			return false;
		if (!isGoalService(goalService))
			return false;
		return getOriginalName(initService).equals(getOriginalName(goalService)) 
				&& initService.getPeerID().equals(goalService.getPeerID())
				&& getSearchID(initService).equals(getSearchID(goalService));
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
