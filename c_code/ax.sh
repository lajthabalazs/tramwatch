#!/bin/sh

while true; do
 D=`date +'%y%m%d-%H%M%S'`
 mkdir $D
 cd $D
 touch vlc-log.txt

 ../a.out &
 APID=$!

 STOP=0
 until [ $STOP -eq 1 ]; do
#  inotifywait -e modify vlc-log.txt
#  if [ -n "`grep 'main error: ES' vlc-log.txt`" ]; then
  inotifywait -t 15 .
  if [ $? -ne 0 ]; then
   STOP=1
  fi
 done;
 
 kill -9 $APID
 cd ..
done

