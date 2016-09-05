import akka.NotUsed
import akka.stream.ActorMaterializer
import akka.util.ByteString
import com.bisphone.testkit.AkkaTest
import com.bisphone.util.ByteOrder
import com.typesafe.config.ConfigFactory
import akka.stream.scaladsl._
import akka.stream.testkit.scaladsl.TestSink
import com.bisphone.akkastream.{ByteStreamSlicer, _}

/**
  * @author Reza Samei <reza.samei.g@gmail.com>
  */

class SlicerSuite extends AkkaTest("SplitterSuite", ConfigFactory.load) {

  implicit val materializer = ActorMaterializer()(system)

  val lenOfLenField = 4
  val order = ByteOrder.BigEndian

  val firstString = "Salam"
  val secondString = "My name is Reza"

  def string2ByteString(str: String, byteOrder: ByteOrder) = {
    implicit val _order = byteOrder.javaValue
    val bytes = ByteString(str)
    ByteString.newBuilder
      .putInt(bytes.length)
      .append(bytes)
      .result()
  }

  def source(byteOrder: ByteOrder): Source[ByteString, NotUsed] = {
    val rsl = ByteString.newBuilder
      .append(string2ByteString(firstString, byteOrder))
      .append(string2ByteString(secondString, byteOrder))
      .result()
    val ls = List(rsl.slice(0, 10), rsl.slice(10, 20), rsl.slice(20, rsl.length))
    Source(ls)
  }

  def invalidSource(byteOrder: ByteOrder): Source[ByteString, NotUsed] = {
    implicit val order = byteOrder.javaValue
    val rsl = ByteString.newBuilder
        .putInt("Before".size)
        .append(ByteString("Before"))
        .putInt(-12)
        .append(ByteString("Hello"))
        .putInt("After".size)
        .append(ByteString("After"))
        .result
    val ls = List(rsl.slice(0,4), rsl.slice(4, 10), rsl.slice(10, rsl.length))
    Source(ls)
  }

  "Slicer" must "extract strings from source" in {

    val stream = source(order)
        .via(ByteStreamSlicer("slicer", lenOfLenField, 1000, order))
        .log("Here")
        .map{ bytes => new String(bytes.drop(lenOfLenField).toArray) }
        .runWith(TestSink.probe[String])

    stream.request(3).expectNext(firstString, secondString).expectComplete()
  }

  it must "fail in the case of invalid lenOfLenFiled (<= 0)" in {
    an [IllegalArgumentException] should be thrownBy {
      val stream = source(order)
        .via(ByteStreamSlicer("slicer", 0, 1000, order))
        .log("Here")
        .map{ bytes => new String(bytes.drop(lenOfLenField).toArray) }
        .runWith(TestSink.probe[String])
    }
  }

  it must "fail in the case of invalid len value (<= 0)" in {
    val stream = invalidSource(order)
      .via(ByteStreamSlicer("slicer", lenOfLenField, 1000, order))
      .log("Here")
      .map{ bytes => new String(bytes.drop(lenOfLenField).toArray) }
      .runWith(TestSink.probe[String])

    stream.request(Int.MaxValue).expectNext("Before").expectError()
  }

}
