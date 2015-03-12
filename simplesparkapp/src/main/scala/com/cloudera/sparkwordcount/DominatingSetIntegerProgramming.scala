package com.cloudera.sparkwordcount

import scala.collection.mutable
import scala.reflect.ClassTag
import scala.math.pow
import scala.math.log1p
import scala.collection.mutable.ArrayBuffer
import org.apache.spark._
import org.apache.spark.storage.StorageLevel
import org.apache.spark.graphx._
import org.apache.spark.graphx.lib._
import org.apache.spark.graphx.PartitionStrategy._
import scala.util.control.Breaks._

object DominatingSetIntegerProgramming extends Logging {

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

   def solveLinearProgram[VD: ClassTag, ED: ClassTag](
      graph: Graph[VD, ED], k: Int, delta: Int): Graph[(Int,Double), Double] =
   {

      val LPGraph: Graph[(Int,Int,Int,Int,Int,Int,Double,ArrayBuffer[Double]),Double] = graph
         // Set the vertices to (delta,step,l,m,k,color,x,msg)
         .mapTriplets( e => 1.0 )
         .mapVertices( (id, attr) => (delta,0,k-1,k-1,k,-1,0.0,ArrayBuffer[Double]()) )
         .cache()


      // -1 is white, -2 is grey

      // Vertex Program
      def vertexProgram(id: VertexId, attr: (Int,Int,Int,Int,Int,Int,Double,ArrayBuffer[Double]), msgCom: ArrayBuffer[Double]): (Int,Int,Int,Int,Int,Int,Double,ArrayBuffer[Double]) = {
         val (delta, step, l, m, k, oldColor, x, oldMsg) = attr
         val nextStep:Int = step+1
         //println(id+"\t"+nextStep+"\n")
         if(step < 0) {
            return attr
         }
         else if(step < 2*k*k+1){
            if(step%2 == 0) {
               var msg = ArrayBuffer[Double]()
               var newColor = oldColor
               if(step == 0) {
                  msg += oldColor
                  newColor = oldColor
               }
               else {
                  var i = 0
                  var sumX:Double = 0
                  while(i < msgCom.length) {
                     sumX = sumX + msgCom(i)
                     i = i+1
                  }
                  if(sumX >= 1){
                     newColor = -2
                  }
                  else {
                     newColor = oldColor
                  }
                  msg += newColor
               }
               return (delta,nextStep,l,m,k,newColor,x,msg)
            }
            else {
               var newX = x
               var msg = ArrayBuffer[Double]()
               var noWN:Double = 0
               var i = 0
               while(i < msgCom.length) {
                  if(msgCom(i) == -1) {
                     noWN = noWN + 1
                  }
                  i = i + 1
               }
               val compFac:Double = pow(delta+1,l.toDouble/k.toDouble);
               if(noWN >= compFac) {
                  val partCol:Double = 1.0/pow(delta+1,m.toDouble/k.toDouble);
                  newX = if(x > partCol) x else partCol
               }
               msg += newX

               var newM = if(m == 0) (k-1) else m-1
               var newL = if(m == 0) (l-1) else l

               return (delta,nextStep,newL,newM,k,oldColor,newX,msg)
            }
         }
         else {
            // in this case delta=delta, step=step, L=degree, M=M, K=K, color=color, x=x, msg=msg
            // Color scheme 0 for white 1 for gray 2 for black
            if(step == 2*k*k+1) {
               val c:Int = 0
               var msg = ArrayBuffer[Double]()
               msg += 1
               return (delta,nextStep,c,m,k,c,x,msg)
            }
            else if(step == 2*k*k+2) {
               var msg = ArrayBuffer[Double]()
               var newDeg:Int = msgCom.length
               msg += newDeg.toDouble
               return (delta,nextStep,newDeg,m,k,oldColor,x,msg)
               /*var msg = ArrayBuffer[Double]()
               msg+=1
               return (delta, nextStep,l,m,k,oldColor,x,msg)*/
            }
            else if(step == 2*k*k+3) {
               var msg = ArrayBuffer[Double]()
               var i = 0
               var maxDeg = 0
               while(i < msgCom.length) {
                  if(msgCom(i) > maxDeg){
                     maxDeg = msgCom(i).toInt
                  }
                  i = i+1
               }
               msg += maxDeg
               return (delta,nextStep,maxDeg,m,k,oldColor,x,msg)
               /*var msg = ArrayBuffer[Double]()
               msg+=1
               return (delta, nextStep, l,m,k,oldColor,x,msg)*/
            }
            else if(step == 2*k*k+4) {
               var msg = ArrayBuffer[Double]()
               var i = 0
               var maxDeg = 0
               while(i < msgCom.length) {
                  if(msgCom(i) > maxDeg){
                     maxDeg = msgCom(i).toInt
                  }
                  i = i+1
               }
               if(l > maxDeg){
                  maxDeg = l
               }
               var p:Double = x*log1p(maxDeg.toDouble)
               if(p > 1){
                  p = 1
               }
               val prob = scala.util.Random.nextDouble
               var newColor: Int = oldColor
               if(prob <= p) {
                  newColor = 2
               }
               else {
                  newColor = 0
               }
               msg += newColor.toDouble
               return (delta,nextStep,maxDeg,m,k,newColor,x,msg)
               /*var msg = ArrayBuffer[Double]()
               msg+=1
               return (delta, nextStep, l,m,k,oldColor,x,msg)*/
            }
            else if(step == 2*k*k+5) {
               var i = 0
               var flag = 0
               while(i < msgCom.length) {
                  if(msgCom(i) == 2){
                     flag = 1
                  }
                  i = i + 1
               }
               var newColor:Int = oldColor
               if(flag == 1){
                  newColor = oldColor
               }
               else {
                  newColor = 2
               }
               return (delta,nextStep,l,m,k,newColor,x,ArrayBuffer[Double]())
               /*var msg = ArrayBuffer[Double]()
               msg+=1
               return (delta, nextStep, l,m,k,oldColor,x,msg)*/
            }
            else {
               return (delta,nextStep,l,m,k, oldColor, x, ArrayBuffer[Double]())
            }
         }
      }

      //Send message
      def sendMessage(edge: EdgeTriplet[(Int,Int,Int,Int,Int,Int,Double,ArrayBuffer[Double]), Double]) = {
         Iterator( (edge.dstId, edge.srcAttr._8) )   
      }

      //Message combiner
      def messageCombiner(a: ArrayBuffer[Double], b: ArrayBuffer[Double]): ArrayBuffer[Double] = {
         var msg=ArrayBuffer[Double]()
         var i=0
         for( i <- 0 until a.length) {
            msg+=a(i)
         }
         for( i <- 0 until b.length) {
            msg += b(i)
         }
         return msg
      }

      val initialMessage = ArrayBuffer[Double]()

      Pregel(LPGraph, initialMessage, maxIterations = 2*k*k+6, activeDirection = EdgeDirection.Out)(
         vertexProgram, sendMessage, messageCombiner)
         .mapVertices( (vid, attr) => (attr._6,attr._7) )
   }

   /*def solveDS[VD: ClassTag, ED: ClassTag](
      graph: Graph[Double, ED]): Graph[Int, Double] =
   {
      val IPGraph: Graph[(Int,Int,Int,Double,ArrayBuffer[Int]),Double] = graph
         .mapTriplets( e => 1.0 )
         .mapVertices( (id, x) => (0,0,0,x,ArrayBuffer[Int]()) )
         .cache()


      def vertexProgram(id: VertexId, attr: (Int,Int,Int,Double,ArrayBuffer[Int]), msgCom: ArrayBuffer[Int]): (Int,Int,Int,Double,ArrayBuffer[Int]) = {
         val (step, deg, color, x, oldMsg) = attr
         val nextStep = step +1
         if(step == 0) {
            var msg = ArrayBuffer[Int]()
            msg += 1
            return (nextStep,deg,color,x,msg)
         }
         else if(step == 1) {
            var msg = ArrayBuffer[Int]()
            var newDeg = msgCom.length
            msg += newDeg
            return (nextStep,newDeg,color,x,msg)
         }
         else if(step ==2) {
            var msg = ArrayBuffer[Int]()
            var i = 0
            var maxDeg = 0
            while(i < msgCom.length) {
               if(msgCom(i) > maxDeg){
                  maxDeg = msgCom(i)
               }
               i = i+1
            }
            msg += maxDeg
            return (nextStep,maxDeg,color,x,msg)
         }
         else if(step ==3) {
            var msg = ArrayBuffer[Int]()
            var i = 0
            var maxDeg = 0
            while(i < msgCom.length) {
               if(msgCom(i) > maxDeg){
                  maxDeg = msgCom(i)
               }
               i = i+1
            }
            if(deg > maxDeg){
               maxDeg = deg
            }
            var p:Double = x*log1p(maxDeg.toDouble)
            if(p > 1){
               p = 1
            }
            val prob = scala.util.Random.nextDouble
            var newColor: Int = color
            if(prob <= p) {
               newColor = 1
            }
            else {
               newColor = 0
            }
            msg += newColor
            return (nextStep,maxDeg,newColor,x,msg)
         }
         else if(step == 4) {
            var i = 0
            var flag = 0
            while(i < msgCom.length) {
               if(msgCom(i) == 1){
                  flag = 1
               }
            }
            var newColor:Int = color
            if(flag == 1){
               newColor = color
            }
            else {
               newColor = 1
            }
            return (nextStep,deg,newColor,x,ArrayBuffer[Int]())
         }
         else {
            return (nextStep, deg, color, x, ArrayBuffer[Int]())
         }
      }

      def sendMessage(edge: EdgeTriplet[(Int,Int,Int,Double,ArrayBuffer[Int]), Double]) = {
         Iterator( (edge.dstId, edge.srcAttr._5) )   
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
 
      Pregel(IPGraph, initialMessage, maxIterations = 5, activeDirection = EdgeDirection.Out)(
         vertexProgram, sendMessage, messageCombiner)
         .mapVertices( (vid, attr) => attr._3 )
   }*/

}