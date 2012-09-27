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

package graphsearch.backward.message;

class IncompatibleParts extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public IncompatibleParts() {
	}

	public IncompatibleParts(final String arg0) {
		super(arg0);
	}

	public IncompatibleParts(final Throwable arg0) {
		super(arg0);
	}

	public IncompatibleParts(final String arg0, final Throwable arg1) {
		super(arg0, arg1);
	}
}
