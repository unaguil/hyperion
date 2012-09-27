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

package taxonomy.parameter;

import taxonomy.Taxonomy;

public class ParameterFactory {

	public static Parameter createParameter(final String fullID, Taxonomy taxonomy) throws InvalidParameterIDException {
		checkID(fullID);

		if (isInput(fullID))
			return new InputParameter(taxonomy.encode(getID(fullID)));
		else if (isOutput(fullID))
			return new OutputParameter(taxonomy.encode(getID(fullID)));
		else
			throw new InvalidParameterIDException("Parameter should have the following format: I-id or O-id. " + fullID + " received.");
	}

	private static void checkID(final String fullID) throws InvalidParameterIDException {
		if (fullID.length() < 3)
			throw new InvalidParameterIDException("Parameter should have the following format: I-id or O-id. " + fullID + " received.");
	}

	private static boolean isInput(final String fullID) {
		return fullID.substring(0, 2).equals("I-");
	}

	private static boolean isOutput(final String fullID) {
		return fullID.substring(0, 2).equals("O-");
	}

	private static String getID(final String fullID) {
		return fullID.substring(2, fullID.length());
	}
}
