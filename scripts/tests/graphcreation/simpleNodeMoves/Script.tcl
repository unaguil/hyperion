source ../../../common/WCommon.tcl

set nNodes 6

set finishTime 30.0

set ns_		[new Simulator]

proc do_something {agents_ nodes_ god_} {
	global ns_
	upvar $agents_ agents
	upvar $nodes_ node_

	source ../../common/simple.tcl
	
	$ns_ at 8.0 "$node_(0) setdest 2.0 1.0 10.0"
}


wireless_simulation $nNodes $finishTime graphcreation.peer.Peer
