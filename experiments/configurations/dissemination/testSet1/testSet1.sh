#!/bin/bash
cd ../../../..
. ./environment.sh
cd experiments

mkdir $1/testSet1
mkdir $2/testSet1

array=($(seq 5 5 100))
size=${#array[@]}

n=4

counter=0
while [ $counter -lt $size ]
do
	let "remaining=$size - $counter"
	if [ $remaining -ge $n ]
	then
		run=$n
	else
		run=$remaining
	fi

	let "end=$counter + $run - 1"

	echo "Running experiments $counter -> $end"

	pos=0	
	for index in $(seq $counter $end)
	do
		i=${array[$index]}
		./Experiment.py -c ./configurations/dissemination/testSet1 -f RWPDisseminationMS1-$i.xml -d True -w $1/testSet1/experimentMS1-$i -o $2/testSet1/experimentMS1-$i.xml > $2/testSet1/experimentMS1-$i.out &
		pids[$pos]=$!
		let "pos=$pos + 1"
	done
	let "counter=$counter + $run"

	for pid in ${pids[*]}
	do
		echo "Waiting for process $pid"
		wait $pid
	done 
done 
