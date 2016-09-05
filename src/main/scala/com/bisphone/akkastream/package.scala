package com.bisphone

import akka.stream.scaladsl.Sink

/**
  * @author Reza Samei <reza.samei.g@gmail.com>
  */
package object akkastream {

  implicit def toSinkOps(origin: Sink.type) = new syntax.SinkExt(origin)

}
