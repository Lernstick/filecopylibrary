#!/bin/sh
for BUNDLE in src/main/resources/ch/fhnw/filecopier/Strings*
do
	echo "processing bundle ${BUNDLE}"
	while read LINE || [ -n "$LINE" ]
	do
		KEY=$(echo ${LINE} | awk -F= '{ print $1 }')
		find -name "*.java" | xargs grep -q "\"${KEY}\""
		if [ $? != 0 ]
		then
			echo "KEY \"${KEY}\" not found"
		fi
	done <${BUNDLE}
done