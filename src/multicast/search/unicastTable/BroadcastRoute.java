package multicast.search.unicastTable;

import multicast.search.Route;
import peer.message.MessageID;
import peer.peerid.PeerID;

class BroadcastRoute implements Route {

		// the destination of the route
		private final PeerID dest;

		// the next hop of the route
		private final PeerID neighbor;

		// the id of the route
		private final MessageID routeID;

		// distance to the destination peer
		private final int distance;
		
		//route creation timestamp
		private final long timestamp;

		public BroadcastRoute(final PeerID dest, final PeerID neighbor, final MessageID routeID, final int distance) {
			this.dest = dest;
			this.neighbor = neighbor;
			this.routeID = routeID;
			this.distance = distance;
			this.timestamp = System.currentTimeMillis();
		}

		@Override
		public boolean equals(final Object o) {
			if (!(o instanceof BroadcastRoute))
				return false;

			final BroadcastRoute route = (BroadcastRoute) o;
			return routeID.equals(route.routeID);
		}

		/* (non-Javadoc)
		 * @see multicast.search.Route#getDest()
		 */
		@Override
		public PeerID getDest() {
			return dest;
		}
		
		/* (non-Javadoc)
		 * @see multicast.search.Route#getDistance()
		 */
		@Override
		public int getDistance() {
			return distance;
		}
		
		/* (non-Javadoc)
		 * @see multicast.search.Route#getNeighbor()
		 */
		@Override
		public PeerID getThrough() {
			return neighbor;
		}

		public MessageID getRouteID() {
			return routeID;
		}

		/* (non-Javadoc)
		 * @see multicast.search.Route#getTimestamp()
		 */
		@Override
		public long getTimestamp() {
			return timestamp;
		}

		@Override
		public int hashCode() {
			return routeID.hashCode();
		}

		@Override
		public String toString() {
			return "(D:" + dest + " N:" + neighbor + ")";
		}
	}