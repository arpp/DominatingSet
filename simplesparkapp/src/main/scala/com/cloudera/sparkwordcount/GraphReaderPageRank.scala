package com.cloudera.sparkwordcount

import scala.collection.mutable
import org.apache.spark._
import org.apache.spark.storage.StorageLevel
import org.apache.spark.graphx._
import org.apache.spark.graphx.lib._
import org.apache.spark.graphx.PartitionStrategy._
import org.apache.spark.HashPartitioner
import org.apache.spark.rdd.RDD
import org.apache.spark.SparkContext._

/**
 * Driver program for running graph algorithms.
 */
object GraphReaderPageRank extends Logging {

  def main(args: Array[String]): Unit = {
    if (args.length < 2) {
      System.err.println(
        "Usage: Analytics <taskType> <file>  [other options]")
      System.exit(1)
    }

    val taskType = args(0)
    val fname = args(1)
    val optionsList = args.drop(2).map { arg =>
      arg.dropWhile(_ == '-').split('=') match {
        case Array(opt, v) => (opt -> v)
        case _ => throw new IllegalArgumentException("Invalid argument: " + arg)
      }
    }
    val options = mutable.Map(optionsList: _*)

    val numEPart = options.remove("numEPart").map(_.toInt).getOrElse {
      println("Set the number of edge partitions using --numEPart.")
      sys.exit(1)
    }

    val conf = new SparkConf()
      .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
      .set("spark.kryo.registrator", "org.apache.spark.graphx.GraphKryoRegistrator")
      .set("spark.locality.wait", "100000")
      .set("spark.scheduler.minRegisteredResourcesRatio", "0.5")
      .set("spark.scheduler.maxRegisteredResourcesWaitingTime", "100000")
      // .set("spark.default.parallelism","20")


    def partitionBy[ED](edges: RDD[Edge[ED]], partitionStrategy: PartitionStrategy): RDD[Edge[ED]] = {
      val numPartitions = edges.partitions.size
      edges.map(e => (partitionStrategy.getPartition(e.srcId, e.dstId, numPartitions), e))
        .partitionBy(new HashPartitioner(numPartitions))
        .mapPartitions(_.map(_._2), preservesPartitioning = true)
    }

    // val partitionStrategy: Option[PartitionStrategy] = options.remove("partStrategy")
    //   .map(PartitionStrategy.fromString(_))
    // val edgeStorageLevel = options.remove("edgeStorageLevel")
    //   .map(StorageLevel.fromString(_)).getOrElse(StorageLevel.MEMORY_ONLY)
    // val vertexStorageLevel = options.remove("vertexStorageLevel")
    //   .map(StorageLevel.fromString(_)).getOrElse(StorageLevel.MEMORY_ONLY)
    // val gStorageLevel = StorageLevel.MEMORY_AND_DISK_SER


    taskType match {
      case "dominatingset" =>
        val outFname = options.remove("output").getOrElse("")

        options.foreach {
          case (opt, _) => throw new IllegalArgumentException("Invalid option: " + opt)
        }

        println("======================================")
        println("|          DominatingSet             |")
        println("======================================")

        val sc = new SparkContext(conf.setAppName("DominatingSet(" + fname + ")"))

        val unpartitionedGraph = GraphLoader.edgeListFile(sc, fname,
          minEdgePartitions = numEPart).cache()
        val graph = unpartitionedGraph//partitionStrategy.foldLeft(unpartitionedGraph)(_.partitionBy(_))
        // val graph = Graph(unpartitionedGraph.vertices, partitionBy(unpartitionedGraph.edges, PartitionStrategy.EdgePartition2D))

        println("GRAPHX: Number of vertices " + graph.vertices.count)
        println("GRAPHX: Number of edges " + graph.edges.count)
        println("Begin time = " + System.currentTimeMillis);
        val pr = DominatingSet.runUntilConvergence(graph).vertices.cache()
        println("End time = " + System.currentTimeMillis);
        // println("GRAPHX: Total rank: " + pr.map(_._2).reduce(_ + _))

        if (!outFname.isEmpty) {
          logWarning("Saving dominating set info to " + outFname)
          pr.map { case (id, r) => id + "\t" + r }.saveAsTextFile(outFname)
        }

        sc.stop()

      case _ =>
        println("Invalid task type.")
    }
  }
}