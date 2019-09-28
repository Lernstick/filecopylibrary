#!/bin/sh
for BUNDLE in src/ch/fhnw/filecopier/Strings*
do
	sort ${BUNDLE} > tmp
	mv tmp ${BUNDLE}
done
