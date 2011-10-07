source ../../../common/WCommon.tcl

set nNodes 6

set finishTime 15.0

set ns_		[new Simulator]

proc do_something {agents_ nodes_ god_} {
	global ns_
	upvar $agents_ agents
	upvar $nodes_ node_

	source ../../common/simpleBrokenNetwork.tcl
	
	$ns_ at 5.0 "$node_(2) setdest 2.0 0.1 5.0"
}


wireless_simulation $nNodes $finishTime detection.peer.Peer