package com.cloudera.sparkwordcount

import scala.collection.mutable
import scala.reflect.ClassTag
import scala.collection.mutable.ArrayBuffer
import org.apache.spark._
import org.apache.spark.storage.StorageLevel
import org.apache.spark.graphx._
import org.apache.spark.graphx.lib._
import org.apache.spark.graphx.PartitionStrategy._
import scala.util.control.Breaks._

object DominatingSet extends Logging {

  /**
   * Run a dynamic version of DominatingSet returning a graph with vertex attributes containing the
   * DominatingSet and edge attributes containing the normalized edge weight.
   *
   * @tparam VD the original vertex attribute (not used)
   * @tparam ED the original edge attribute (not used)
   *
   * @param graph the graph on which to compute DominatingSet
   *
   */
  def runUntilConvergence[VD: ClassTag, ED: ClassTag](
      graph: Graph[VD, ED]): Graph[(Int,Int), Double] =
  {
    // val gStorageLevel = StorageLevel.MEMORY_AND_DISK_SER
    val DominatingSetGraph: Graph[(Int,Int,Int,ArrayBuffer[Int]), Double] = graph
      // Set the vertex attributes to (superstep,color,no of white neighbor,msg to pass) color: -5 for black, -4 for gray, -3 for white
      .mapTriplets( e => 1.0 )
      .mapVertices( (id, attr) => (0,-3,0,ArrayBuffer[Int]()) )
      .cache()

    // Define the three functions needed to implement DominatingSet in the GraphX
    // version of Pregel
    def vertexProgram(id: VertexId, attr: (Int,Int,Int,ArrayBuffer[Int]), msgSum: ArrayBuffer[Int]): (Int,Int,Int,ArrayBuffer[Int]) = {
    	val (superstep, oldColor, noNeigh, oldMsg) = attr
    	val nextSuperstep:Int = superstep+1
    	var newColor = oldColor

    	if(superstep < 0){
    		return attr
    	}
    	else if(superstep%4 == 0){
    		var i = 0
    		if(msgSum.length == 0) {
    			var msg=ArrayBuffer[Int]()
    			msg += id.toInt
    			msg += oldColor
    			return (nextSuperstep,oldColor,noNeigh,msg)
    		}
    		else {
    			if(oldColor != -5) {
	    			breakable {
			    		for( i <- 0 until msgSum.length/2) {
							if(msgSum(2*i+1) == -5) {
								newColor = -4
								break
							}
			    		}
		    		}
	    		}
	    		else {
	    			newColor = oldColor
	    		}
	    		var msg=ArrayBuffer[Int]()
	    		msg += id.toInt
	    		msg += newColor
	    		return (nextSuperstep,newColor,noNeigh,msg)
    		}
    	}
    	else if(superstep%4 == 1) {
    		var wt=0
    		var i=0
    		for( i <- 0 until msgSum.length/2) {
    			if(msgSum(2*i+1) == -3) {
    				wt=wt+1
    			}
    		}
    		var msg=ArrayBuffer[Int]()
    		msg += id.toInt
    		if(oldColor == -5||(oldColor == -4&&wt == 0)) {
    			msg += -1
    		}
    		else {
    			msg += wt
    		}
    		return (nextSuperstep,oldColor,wt,msg)
    	}
    	else if(superstep%4 == 2) {
    		var oneHopInfo=ArrayBuffer[Int]()
    		var count=0
    		var i=0
            var maxid = id.toInt
            var maxNeigh = noNeigh

    		// oneHopInfo += id.toInt
    		// oneHopInfo += noNeigh

    		for( i <- 0 until msgSum.length/2) {
				if(msgSum(2*i+1) != -1) {
                    if((maxNeigh < msgSum(2*i+1))||((maxNeigh == msgSum(2*i+1)) && (maxid > msgSum(2*i)))) {
                        maxNeigh = msgSum(2*i+1)
                        maxid = msgSum(2*i)
                    }
				    count=count+1
				    // oneHopInfo += msgSum(2*i)
				    // oneHopInfo += msgSum(2*i+1)
				}	
    		}
            oneHopInfo += maxid
            oneHopInfo += maxNeigh
    		if(count==0){
    			return (-1,oldColor,nextSuperstep,oneHopInfo)
    		}
    		else {
    			return (nextSuperstep,oldColor,noNeigh,oneHopInfo)
    		}
    	}
    	else {			//(superstep%4==3)
    		var flag=0
    		newColor=oldColor
    		var msg=ArrayBuffer[Int]()
    		if(oldColor != -5){
    			var i=0
    			breakable {
    				for( i <- 0 until msgSum.length/2) {
    					if(msgSum(2*i+1) > noNeigh||(msgSum(2*i+1) == noNeigh && msgSum(2*i) < id.toInt)) {
    						flag = -1
    						break
    					}
    				}
    			}
    			if(flag==0) {
    				newColor = -5
    			}
    		}
    		msg += id.toInt
    		msg += newColor
    		return (nextSuperstep,newColor,noNeigh,msg)
    	}
    }

    def sendMessage(edge: EdgeTriplet[(Int,Int,Int,ArrayBuffer[Int]), Double]) = {
    	if(edge.srcAttr._1 >= 0){
    		Iterator( (edge.dstId, edge.srcAttr._4) )
    	}
    	else {
    		Iterator.empty
    	}	
    	
    }

    def messageCombiner(a: ArrayBuffer[Int], b: ArrayBuffer[Int]): ArrayBuffer[Int] = {
    	var msg=ArrayBuffer[Int]()
    	var i=0
    	for( i <- 0 until a.length) {
    		msg+=a(i)
    	}
    	for( i <- 0 until b.length) {
    		msg+=b(i)
    	}
    	return msg
    }

    // The initial message received by all vertices in DominatingSet
    val initialMessage = ArrayBuffer[Int]()

    // Execute a dynamic version of Pregel.
    Pregel(DominatingSetGraph, initialMessage, activeDirection = EdgeDirection.Out)(
      vertexProgram, sendMessage, messageCombiner)
      .mapVertices( (vid, attr) => (attr._2,attr._3) )
  } // end of deltaDominatingSet
}