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
*   Author: Unai Aguilera <gkalgan@gmail.com>
*/

package taxonomy.parameter;

import taxonomy.Taxonomy;

public class OutputParameter extends Parameter {
	
	public OutputParameter() {
		super(OUTPUT_PARAMETER);
	}

	public OutputParameter(final short value) {
		super(OUTPUT_PARAMETER, value);
	}

	@Override
	public boolean equals(final Object o) {
		if (!(o instanceof OutputParameter))
			return false;

		return super.equals(o);
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}

	@Override
	public String pretty(final Taxonomy taxonomy) {
		return "O-" + super.pretty(taxonomy);
	}
}
