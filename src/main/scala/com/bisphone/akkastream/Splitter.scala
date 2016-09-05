package com.bisphone.akkastream

import akka.stream._
import akka.stream.stage._
import akka.stream.scaladsl._
import scala.collection.mutable

/**
  * @author Reza Samei <reza.samei.g@gmail.com>
  */


object Splitter {
  def apply[In,Left,Right](name: String)(f: In => Either[Left,Right]) =
    new Splitter2[In,Left,Right](name) { override val decider = f }

  type To2[In, Left, Right] = GraphStage[FanOutShape2[In, Left, Right]]

  class Exception(msg: String, cause: Throwable = null) extends Throwable(msg, cause)
}

/**
  * This GraphStage should return
  * @tparam I Input Stream
  * @tparam L Left/Out0
  * @tparam R Right/Out1
  */
abstract class Splitter2[I,L,R](name: String) extends Splitter.To2[I,L,R] {

  val in = Inlet[I](s"Splitter(${name}).in")
  val left = Outlet[L](s"Splitter(${name}).left")
  val right = Outlet[R](s"Splitter(${name}).right")

  def decider: I => Either[L,R]

  override def shape = new FanOutShape2(in, left, right)

  override def createLogic(inherited: Attributes): GraphStageLogic = {

    val stage = new GraphStageLogic(shape) { stage =>

      var isRightDemanded = false
      var isLeftDemanded = false

      setHandler(right, new OutHandler {
        override def onPull(): Unit = {
          if (isRightDemanded) failStage(new Splitter.Exception(s"Splitter(${name}).right demands again!"))
          else if (isLeftDemanded) pull(in)
          isRightDemanded = true
        }
      })

      setHandler(left, new OutHandler {
        override def onPull(): Unit = {
          if (isLeftDemanded) failStage(new Splitter.Exception(s"Splitter(${name}).left demands again!"))
          else if (isRightDemanded) pull(in)
          isLeftDemanded = true
        }
      })

      setHandler(in, new InHandler {
        override def onPush(): Unit = {
          decider(grab(in)) match {
            case Left(value) => push(left, value); isLeftDemanded = false
            case Right(value) => push(right, value); isRightDemanded = false
          }
        }
      })

    }

    stage
  }
}
