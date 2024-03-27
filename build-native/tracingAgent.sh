#!/usr/bin/env bash

if [[ ! -e ../target/scala-3.4.0/pct-compose ]]; then
	cd ..
	sbt assembly
	cd build-native
fi

java -agentlib:native-image-agent=config-merge-dir=./native-image-configs/ -jar ../target/scala-3.4.0/pct-compose $@
