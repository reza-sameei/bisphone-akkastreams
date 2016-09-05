package com.bisphone.akkastream

import akka.stream._
import akka.stream.stage._
import akka.stream.scaladsl._
import akka.util.ByteString
import com.bisphone.util.ByteOrder

/**
  * @author Reza Samei <reza.samei.g@gmail.com>
  * This is a similar implementation of [[akka.stream.scaladsl.Framing.lengthField]] with some little diffs:
  * - The 'len-field len' in included in the len of slice/frame/packet
  * - There's not 'offset' filed for slice/frame/packet body
  * - the out-stream is a byte-string that includes the len-filed
  * because of the above reason and avoiding name confusion, this class named 'ByteStreamSlicer'.
  */
class ByteStreamSlicer(
  name: String,
  lenOfLenField: Int,
  maxLen: Int,
  byteOrder: ByteOrder
) extends GraphStage[FlowShape[ByteString, ByteString]]{

  require(lenOfLenField>0, "Invalid value for 'lenOfLenFiled'")

  private val minChunkSize = lenOfLenField

  private val in = Inlet[ByteString](s"ByteStreamSlicer(${name}).in")
  private val out = Outlet[ByteString](s"ByteSreamSlicer(${name}).out")

  override val shape: FlowShape[ByteString, ByteString] = FlowShape(in, out)

  override def createLogic(inheritedAttrs: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) with InHandler with OutHandler {

      private var buf = ByteString.empty
      private var sliceSize = Int.MaxValue

      private def tryPull(): Unit =
        if (isClosed(in))
          failStage(new RuntimeException("Stream finished but there was a truncated final frame in the buffer"))
        else pull(in)

      private def tryPush(): Unit = {
        if (buf.size >= sliceSize) pushSlice()
        else if (buf.size >= minChunkSize) {
          val parsedLen = byteOrder.decodeInt(buf.iterator, lenOfLenField) // - lenOfLenField
          if (parsedLen <= 0) failStage(new RuntimeException(s"Invalid value for slice's len: ${parsedLen}"))
          else {
            sliceSize = parsedLen + minChunkSize
            if (sliceSize > maxLen)
              failStage(new RuntimeException(s"Maximum allowed frame size is $maxLen but decoded frame header reported size $sliceSize"))
            else if (buf.size >= sliceSize) pushSlice()
            else tryPull()
          }
        } else tryPull()
      }

      private def pushSlice() = {
        val emit = (buf take sliceSize).compact
        buf = buf drop sliceSize
        sliceSize = Int.MaxValue
        push(out, emit)
        if (buf.isEmpty && isClosed(in)) completeStage()
      }

      override def onPush(): Unit = {
        buf ++= grab(in)
        tryPush
      }

      override def onPull(): Unit = tryPush

      override def onUpstreamFinish(): Unit = {
        if (buf.isEmpty) completeStage()
        else if (isAvailable(out)) tryPush()
        // ?! - else swallow the termination and wait for pull
      }

      setHandlers(in, out, this)

    }

}

object ByteStreamSlicer {
  def apply(name: String, lenOfLenField: Int, maxLen: Int, byteOrder: ByteOrder) =
    new ByteStreamSlicer(name, lenOfLenField, maxLen, byteOrder)
}
