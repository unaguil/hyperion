source ../../../common/WCommon.tcl

set nNodes 2

set finishTime 15.0

set ns_		[new Simulator]

proc do_something {agents_ nodes_ god_} {
	global ns_
	upvar $agents_ agents
	upvar $nodes_ node_

	source ../../common/verysimple.tcl
	
	$ns_ at 3.0 "$agents(1) agentj composeService 0"	
}

wireless_simulation $nNodes $finishTime graphsearch.peer.ForwardPeer
