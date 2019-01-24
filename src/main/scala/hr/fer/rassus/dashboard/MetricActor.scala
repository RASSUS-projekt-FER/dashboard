package hr.fer.rassus.dashboard


import java.time.Instant

import akka.actor.{Actor, Props}

import scala.collection.mutable


object MetricActor {
  def props = Props(new MetricActor)

  // input messages
  case class StoreAggregation(aggregationName: String, value: Double)
  case object GetMetric
  case class GetAggregation(aggregationName: String)
  case class DrawAggregation(aggregationName: String)

  // output messages
  case object StoreAggregationSuccess
  case class Metric(metricName: String, aggregations: Vector[String])
  case class Aggregation(aggregationName: String, values: Vector[Double])
  case object NoSuchAggregation
  case class DrawAggregationResult(imageLocation: String)

  // internal classes
  case class AggregationValue(value: Double, timestamp: Instant)
}

class MetricActor extends Actor {

  import MetricActor._

  val maxQueueSize: Int = 50
  // map[aggregationName, queue[values]
  val aggregationValuesMap: mutable.Map[String, mutable.Queue[AggregationValue]] =
    mutable.Map.empty[String, mutable.Queue[AggregationValue]]

  override def receive: Receive = {
    case StoreAggregation(aggregationName, value) =>
      if (!aggregationValuesMap.contains(aggregationName)) {
        aggregationValuesMap.put(aggregationName, mutable.Queue.empty[AggregationValue])
      }

      val queue = aggregationValuesMap(aggregationName)
      if (queue.size == maxQueueSize) {
        queue.dequeue()
      }

      queue.enqueue(AggregationValue(value, Instant.now()))
      sender() ! StoreAggregationSuccess

    case GetMetric =>
      sender() ! Metric(self.path.name, aggregationValuesMap.keys.toVector)

    case GetAggregation(aggregationName) =>
      if(aggregationValuesMap.contains(aggregationName)) {
        val aggregationValues = aggregationValuesMap(aggregationName).map(_.value).toVector
        sender() ! Aggregation(aggregationName, aggregationValues)
      } else {
        sender() ! NoSuchAggregation
      }

    case DrawAggregation(aggregationName) =>
      import scalax.chart.api._
      import java.nio.file.Files
      import java.nio.file.Paths

      val chartDirName = "charts"
      val charFileName = context.parent.path.name + aggregationName

      val currentDir = Paths.get(System.getProperty("user.dir"))
      val chartDir = Paths.get(currentDir.toString, chartDirName)
      if (!Files.exists(chartDir))
        Files.createDirectories(chartDir)

      val charFile = Paths.get(chartDir.toString, charFileName).toString

      if(aggregationValuesMap.contains(aggregationName)) {
        val aggregationValues = aggregationValuesMap(aggregationName)

        val data = aggregationValues.map(aggregationValue =>
          (aggregationValue.timestamp.toEpochMilli, aggregationValue.value)
        )
        val chart = XYLineChart(data)
        chart.saveAsPNG(charFile)
        sender() ! DrawAggregationResult(charFile)

      } else {
        sender() ! NoSuchAggregation
      }

  }
}
