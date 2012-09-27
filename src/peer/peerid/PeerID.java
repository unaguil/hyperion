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

package peer.peerid;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import serialization.binary.BSerializable;
import serialization.binary.SerializationUtils;

/**
 * This class is used for peer identification. It is implemented through the
 * usage of a string identifier.
 * 
 * @author Unai Aguilera (unai.aguilera@gmail.com)
 * 
 */
public final class PeerID implements Comparable<PeerID>, BSerializable {
	
	private final int id;

	public static final PeerID VOID_PEERID = new PeerID(Integer.MIN_VALUE);
	
	public PeerID() {
		id = Integer.MIN_VALUE;
	}

	public PeerID(final String id) {
		this.id = Integer.parseInt(id);
	}
	
	public PeerID(final int id) {
		this.id = id;
	}

	@Override
	public String toString() {
		return "" + id;
	}

	@Override
	public boolean equals(final Object o) {
		if (!(o instanceof PeerID))
			return false;
		final PeerID peerID = (PeerID) o;
		return peerID.id == this.id;
	}

	@Override
	public int hashCode() {
		return id;
	}

	@Override
	public int compareTo(final PeerID peerID) {
		return this.id - peerID.id;
	}

	@Override
	public void read(final ObjectInputStream in) throws IOException {
		SerializationUtils.setFinalField(PeerID.class, this, "id", in.readInt());
	}

	@Override
	public void write(final ObjectOutputStream out) throws IOException {
		out.writeInt(id);
	}
}
