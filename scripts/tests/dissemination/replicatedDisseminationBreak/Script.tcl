source ../../../common/WCommon.tcl

set nNodes 8

set finishTime 15.0

set ns_		[new Simulator]

proc do_something {agents_ nodes_ god_} {
	global ns_
	upvar $agents_ agents
	upvar $nodes_ node_

	source ../../common/replicatedDissemination.tcl
	
	$ns_ at 5.0 "$node_(6) setdest 2.0 1.0 5.0"
	$ns_ at 5.0 "$node_(7) setdest 2.0 2.0 5.0"
	
	$ns_ at 10.0 "$node_(6) setdest 2.0 2.0 5.0"
	$ns_ at 10.0 "$node_(7) setdest 2.0 3.0 5.0"
}


wireless_simulation $nNodes $finishTime dissemination.peer.Peer
