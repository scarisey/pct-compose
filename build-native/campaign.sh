#!/usr/bin/env bash
./tracingAgent.sh parse
./tracingAgent.sh status
./tracingAgent.sh restoreFw
./tracingAgent.sh project -s true -r true
./tracingAgent.sh project
./tracingAgent.sh update
