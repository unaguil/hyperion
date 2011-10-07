#!/bin/sh

PSTREE=$(pstree -p $1)

PIDS=$(echo "$PSTREE" | grep -o '[0-9]\{2,5\}')

kill -9 $PIDS
