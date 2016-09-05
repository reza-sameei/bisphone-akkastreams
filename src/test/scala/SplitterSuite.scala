
import com.bisphone.akkastream.Splitter
import com.bisphone.std._
import org.scalatest._
import akka.stream._
import akka.stream.scaladsl._
import akka.stream.testkit.scaladsl.TestSink
import com.bisphone.testkit._
import com.typesafe.config.ConfigFactory

/**
  * @author Reza Samei <reza.samei.g@gmail.com>
  */
class SplitterSuite extends AkkaTest("SplitterSuite", ConfigFactory.load) {

  implicit val materializer = ActorMaterializer()(system)

  def leftSource[In, Left, Right](name: String, input: List[In])(fn: In => StdEither[Left, Right]) = {

    val graph = GraphDSL.create() { implicit builder =>

      import GraphDSL.Implicits._

      val source = Source(input)

      val rightSink = Sink.seq[Right]

      val splitter = builder add Splitter[In, Left, Right](name)(fn)

      source ~> splitter.in

      splitter.out1 ~> rightSink

      SourceShape.of(splitter.out0 /* Left */)
    }

    Source fromGraph graph
  }

  def rightSource[In, Left, Right](name: String, input: List[In])(fn: In => StdEither[Left, Right]) = {

    val graph = GraphDSL.create() { implicit builder =>

      import GraphDSL.Implicits._

      val source = Source(input)

      val leftSink = Sink.seq[Left]

      val splitter = builder add Splitter[In, Left, Right](name)(fn)

      source ~> splitter.in

      splitter.out0 ~> leftSink

      SourceShape.of(splitter.out1 /* Right */)
    }

    Source fromGraph graph
  }

  "Splitter" must "split 'string' and 'int' values and return them correctly" in {

    val list = "Hello" :: "12" :: "34" :: "Again" :: "Hello" :: Nil


    val ints = rightSource("right-source", list) { str =>
      try(str.toInt.stdright) catch {
        case cause => /*ignore*/ str.stdleft
      }
    }
    val stream1 = ints runWith TestSink.probe[Int]
    stream1.request(3).expectNext(12, 34).expectComplete()


    val strings = leftSource("right-source", list) { str =>
      try(str.toInt.stdright) catch {
        case cause => /*ignore*/ str.stdleft
      }
    }
    val stream2 = strings runWith TestSink.probe[String]
    stream2.request(3).expectNext("Hello", "Again", "Hello").expectComplete()
  }

}