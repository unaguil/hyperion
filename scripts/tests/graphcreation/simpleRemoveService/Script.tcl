source ../../../common/WCommon.tcl

set nNodes 6

set finishTime 15.0

set ns_		[new Simulator]

proc do_something {agents_ nodes_ god_} {
	global ns_
	upvar $agents_ agents
	upvar $nodes_ node_

	source ../../common/simple.tcl
	
	$ns_ at 6.0 "$agents(0) agentj removeService S1"
}

wireless_simulation $nNodes $finishTime graphcreation.peer.Peer
