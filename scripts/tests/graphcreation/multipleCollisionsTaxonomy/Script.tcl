source ../../../common/WCommon.tcl

set nNodes 8

set finishTime 15.0

set ns_		[new Simulator]

proc do_something {agents_ nodes_ god_} {
	global ns_
	upvar $agents_ agents
	upvar $nodes_ node_

	source ../../common/multipleCollisions.tcl
}


wireless_simulation $nNodes $finishTime graphcreation.peer.Peer
