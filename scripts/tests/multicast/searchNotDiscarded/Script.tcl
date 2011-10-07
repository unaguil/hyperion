source ../../../common/WCommon.tcl

set nNodes 14

set finishTime 15.0

set ns_		[new Simulator]

proc do_something {agents_ nodes_ god_} {
	global ns_
	upvar $agents_ agents
	upvar $nodes_ node_

	source ../../common/searchNotDiscard.tcl
	
	$ns_ at 5.0 "$agents(4) agentj searchParameter I-A"
}


wireless_simulation $nNodes $finishTime multicast.peer.Peer
