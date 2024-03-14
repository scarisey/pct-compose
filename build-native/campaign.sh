#!/usr/bin/env bash
./tracingAgent.sh parse
./tracingAgent.sh status
./tracingAgent.sh restoreFw
./tracingAgent.sh project -s true
./tracingAgent.sh project -w true
./tracingAgent.sh update
