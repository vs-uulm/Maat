#!/bin/bash
function usage() {
  cmdname=`echo $0 | sed -e "s/^.*[^\/]\/\(.*\)/\1/"`
  echo "usage: $cmdname [params]"
  echo -e "\nParams:"
  echo "-t start: start at time <start> (default: 0)"
  echo "-c count: generate <count> amount of data (default: 10000)"
  echo "-f file : write (append) to output file (default: stdout)"
  echo -e "\nExamples:"
  echo "$cmdname -t 7 -c 21 -f start_at_7_count_21.csv"
  echo "count=10; $cmdname -c \$count -f \$count.csv"
  echo "for (( i = 1; i <= 1000; i++ )); do $cmdname -c \$i -f car_\$i.csv ; done"
  exit 0
}

start=0
count=10000
file=/dev/stdout

# read params (TODO: sanity checks):
while [ "$1" == "-t" ] || [ "$1" == "-c" ] || [ "$1" == "-f" ] ; do
  if [ "$1" == "-t" ]; then
    start=$2; shift 2
  elif [ "$1" == "-c" ]; then
    count=$2; shift 2
  elif [ "$1" == "-f" ]; then
    file=$2; shift 2
  fi
done

[ $# -gt 0 ] && usage

echo "\"time\";\"sensor value\"" >> $file
for (( i = 0; i < $count; i++ )); do
  echo "$start;$RANDOM" >> $file
  start=$(( $start + 1 ))
done
exit 0
