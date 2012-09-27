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

package graphcreation.collisionbased.message;

import graphcreation.collisionbased.collisiondetector.Collision;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import peer.peerid.PeerID;
import serialization.binary.BSerializable;

public class Inhibition implements BSerializable {
	
	private final Collision collision;
	private final PeerID detectedBy;
	
	public Inhibition() {
		this.collision = new Collision();
		this.detectedBy = new PeerID();
	}
	
	public Inhibition(final Collision collision, final PeerID detectedBy) {
		this.collision = collision;
		this.detectedBy = detectedBy;
	}
	
	public Collision getCollision() {
		return collision;
	}
	
	public PeerID getDetectedBy() {
		return detectedBy;
	}

	@Override
	public void read(ObjectInputStream in) throws IOException {
		collision.read(in);
		detectedBy.read(in);
	}

	@Override
	public void write(ObjectOutputStream out) throws IOException {
		collision.write(out);
		detectedBy.write(out);
	}
	
	@Override
	public String toString() {
		return "[" + collision + ", " + detectedBy + "]";
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Inhibition))
			return false;
		
		Inhibition inhibition = (Inhibition)o;
		return this.collision.equals(inhibition.collision);
	}
	
	@Override
	public int hashCode() {
		int result = 17;
		result = 37 * result + this.collision.hashCode();
		return result; 
	}
}
