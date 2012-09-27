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

package dissemination;

public class DistanceChange {

	private final int previousValue;
	private final int newValue;

	public DistanceChange(final int previousValue, final int newValue) {
		this.previousValue = previousValue;
		this.newValue = newValue;
	}

	public int getPreviousValue() {
		return previousValue;
	}

	public int getNewValue() {
		return newValue;
	}
	
	@Override
	public String toString() {
		return "Change:" + previousValue + "->" + newValue;
	}
}
