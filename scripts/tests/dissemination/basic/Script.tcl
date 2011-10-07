source ../../../common/WCommon.tcl

set nNodes 12

set finishTime 15.0

set ns_		[new Simulator]

proc do_something {agents_ nodes_ god_} {
	global ns_
	upvar $agents_ agents
	upvar $nodes_ node_

 	source ../../common/basic.tcl
}

wireless_simulation $nNodes $finishTime dissemination.peer.Peer