spark-submit --class com.cloudera.sparkwordcount.GraphReaderPageRank --master yarn-client --executor-memory 50G --num-executors 5 --executor-cores 8 DS_int.jar intprog /user/btech/arpitk/NewGraph/input/CA-GrQc.txt --numEPart=20 --delta=65 --k=9 --partStrat=0 --output=/user/btech/arpitk/NewGraph/outputIntegerProg/GrQc-noPart1 >out/GrQc-noPart1.txt 2>log/GrQc-noPart1.txt


