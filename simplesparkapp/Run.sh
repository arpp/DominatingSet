#!/bin/bash

# spark-submit --class com.cloudera.sparkwordcount.GraphReaderPageRank --master yarn-client --executor-memory 50G --num-executors 20 --executor-cores 4 sparkwordcount-0.0.1-SNAPSHOT.jar dominatingset /user/btech/arpitk/GraphData/CollNet/CA-HepPh.txtD.txt --output=/user/btech/arpitk/GraphData/output/CollNet/HepPh >HepPhOut.txt 2>HepPhLog.txt
# spark-submit --class com.cloudera.sparkwordcount.GraphReaderPageRank --master yarn-client --executor-memory 50G --num-executors 20 --executor-cores 4 sparkwordcount-0.0.1-SNAPSHOT.jar dominatingset /user/btech/arpitk/GraphData/CollNet/CA-GrQc.txtD.txt --output=/user/btech/arpitk/GraphData/output/CollNet/GrQc >GrQcOut.txt 2>GrQcLog.txt
spark-submit --class com.cloudera.sparkwordcount.GraphReaderPageRank --master yarn-client --executor-memory 50G --num-executors 20 --executor-cores 4 sparkwordcount-0.0.1-SNAPSHOT.jar dominatingset /user/btech/arpitk/GraphData/CollNet/CA-CondMat.txtD.txt --output=/user/btech/arpitk/GraphData/output/CollNet/CondMat
# spark-submit --class com.cloudera.sparkwordcount.GraphReaderPageRank --master yarn-client --executor-memory 50G --num-executors 20 --executor-cores 4 sparkwordcount-0.0.1-SNAPSHOT.jar dominatingset /user/btech/arpitk/GraphData/CollNet/CA-HepTh.txtD.txt --output=/user/btech/arpitk/GraphData/output/CollNet/HepTh >HepThOut.txt 2>HepThLog.txt

# spark-submit --class com.cloudera.sparkwordcount.GraphReaderPageRank --master yarn-client --executor-memory 50G --num-executors 20 --executor-cores 4 sparkwordcount-0.0.1-SNAPSHOT.jar dominatingset /user/btech/arpitk/GraphData/RoadNet/roadNet-CA.txtD.txt --output=/user/btech/arpitk/GraphData/output/RoadNet/roadNet-CA >roadnetCAOut.txt 2>roadnetCALog.txt
# spark-submit --class com.cloudera.sparkwordcount.GraphReaderPageRank --master yarn-client --executor-memory 50G --num-executors 20 --executor-cores 4 sparkwordcount-0.0.1-SNAPSHOT.jar dominatingset /user/btech/arpitk/GraphData/RoadNet/roadNet-PA.txtD.txt --output=/user/btech/arpitk/GraphData/output/RoadNet/roadNet-PA >roadnetPAOut.txt 2>roadnetPALog.txt
# spark-submit --class com.cloudera.sparkwordcount.GraphReaderPageRank --master yarn-client --executor-memory 50G --num-executors 20 --executor-cores 4 sparkwordcount-0.0.1-SNAPSHOT.jar dominatingset /user/btech/arpitk/GraphData/RoadNet/roadNet-TX.txtD.txt --output=/user/btech/arpitk/GraphData/output/RoadNet/roadNet-TX >roadnetTXOut.txt 2>roadnetTXLog.txt