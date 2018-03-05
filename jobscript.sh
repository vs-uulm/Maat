#!/bin/bash
if [ -z "$1" ]; then
  echo "Usage: $0 <posverif sim job directory> (<attacker type>)"
  exit
fi

INPUT_DIR=$(cd "$1"; pwd)
ATTACKER_TYPE=""

if [ ! -z "$2" ]; then
  ATTACKER_TYPE="$2"
fi

# Queue variable. Currently no jobs are in the queue.
declare -i QUEUE
QUEUE=$(showq | grep "Total jobs" | sed -r "s/Total jobs:[ ]*([0-9]*)/\1/g")
UPPER_LIMIT_DAY=49
UPPER_LIMIT_NIGHT=49

# Job submission script
JOB_SUBMISSION_SCRIPT="`pwd`/job.moab"

# Get the simulation count
JOB_COUNT=$(find ${INPUT_DIR} -name "*.tgz" | wc -l)
if [ 0 -eq ${JOB_COUNT} ]; then
  echo "No sim jobs in '${INPUT_DIR}'. Nothing to do."
  exit
fi

echo "Jobs: ${JOB_COUNT}"

# cd into workspace
WS_DIR="${WORK}/`whoami`-maat-0"
mkdir -vp "$WS_DIR"
cd $WS_DIR

echo "Switched to ${WS_DIR}"

# Outer control loop
let "SLEEP_TIME=60"
for fileName in $(find ${INPUT_DIR} -name "*.tgz"); do
  echo "${fileName}"
  declare -i UPPER_LIMIT
  h=$(date +%H)
  if [ $h -lt 8 -o $h -gt 20 ]; then
    UPPER_LIMIT=$UPPER_LIMIT_NIGHT
  else
    UPPER_LIMIT=$UPPER_LIMIT_DAY
  fi

  # Determine how many jobs are in the queue at the moment
  QUEUE=$(showq -v | grep "`whoami`" | wc -l)

  while [ $QUEUE -ge $UPPER_LIMIT ]; do
    sleep $SLEEP_TIME; # Only check every minute if there is capacity available
    QUEUE=$(showq -v | grep "`whoami`" | wc -l)
  done

  # Add job
  echo "msub ${JOB_SUBMISSION_SCRIPT} ${fileName} ${ATTACKER_TYPE}"
  JOBID=$(msub ${JOB_SUBMISSION_SCRIPT} ${fileName} ${ATTACKER_TYPE})
  if [ 0 -eq $? ]; then
    let "QUEUE++"
    echo "Added JOB: ${JOBID} (${fileName})"
  fi
  sleep 1
done

echo "Finished running all jobs."
