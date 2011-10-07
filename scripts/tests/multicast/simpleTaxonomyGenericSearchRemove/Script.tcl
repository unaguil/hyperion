source ../../../common/WCommon.tcl

set nNodes 6

set finishTime 15.0

set ns_		[new Simulator]

proc do_something {agents_ nodes_ god_} {
	global ns_
	upvar $agents_ agents
	upvar $nodes_ node_

	source ../../common/simple.tcl
	
	$ns_ at 3.0 "$agents(4) agentj searchParameterGeneric I-A"
	
	$ns_ at 8.0 "$agents(0) agentj removeParameter I-B"
}


wireless_simulation $nNodes $finishTime multicast.peer.Peer
