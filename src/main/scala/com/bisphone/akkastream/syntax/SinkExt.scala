package com.bisphone.akkastream.syntax

import akka.stream.scaladsl.Sink

/**
  * @author Reza Samei <reza.samei.g@gmail.com>
  */
class SinkExt(val sink: Sink.type) extends AnyVal {
  def count[T] = Sink.fold[Int,T](0) { (sum,_) => sum + 1 }
  def complete[T] = Sink.head[T]
  def close[T] = Sink.cancelled[T]
}