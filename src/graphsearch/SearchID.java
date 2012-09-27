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

package graphsearch;

import peer.message.MessageID;
import peer.peerid.PeerID;

public class SearchID extends MessageID {
	
	private static short searchCounter = 0;
	
	public SearchID() {
	}
	
	public SearchID(final PeerID startPeer, final short id) {
		super(startPeer, id);
	}

	public SearchID(final PeerID startPeer) {
		super(startPeer, searchCounter++);
	}

	@Override
	public boolean equals(final Object o) {
		if (!(o instanceof SearchID))
			return false;

		return super.equals(o);
	}
}
