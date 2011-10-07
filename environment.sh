#/bin/sh

export AGENTJ_PEER_PATH=${PWD}/../AgentJPeer
export PATH=$PATH:$NS_DIR/bin:$NS_DIR/tcl8.4.18/unix:$NS_DIR/tk8.4.18/unix:$NS_DIR/ns-2.34/indep-utils/cmu-scen-gen/setdest:$NS_DIR/nam-1.14$
export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$NS_DIR/otcl-1.13:$NS_DIR/lib
export TCL_LIBRARY=$TCL_LIBRARY:$NS_DIR/tcl8.4.18/library

export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$AGENTJ/core/lib/:$JAVA_HOME/jre/lib/amd64/server

export AGENTJ_CLASSPATH=$AGENTJ_PEER_PATH/bin:$AGENTJ_PEER_PATH/lib/log4j-1.2.16.jar:${PWD}/bin:${PWD}/lib/jgrapht-jdk1.6.jar:

