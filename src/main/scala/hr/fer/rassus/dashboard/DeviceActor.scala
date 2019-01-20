package hr.fer.rassus.dashboard

import akka.actor.{Actor, Props}

object DeviceActor {
  def props = Props(new DeviceActor)

  // input messages
  case class StoreMetric(metricName: String, aggregationName: String, value: Double)
  case object GetDevice
  case class GetMetric(metricName: String)
  case class GetMetricAggregation(metricName: String, aggregationName: String)
  case class DrawMetricAggregation(metricName: String, aggregationName: String)

  // output message
  case class Device(deviceName: String, metrics: Vector[String])
  case object NoSuchMetric
}

class DeviceActor extends Actor {

  import DeviceActor._

  override def receive: Receive = {
    case StoreMetric(metricName, aggregationName, value) =>
      val storeAggregation = MetricActor.StoreAggregation(aggregationName, value)
      def createAndStore(): Unit = {
        val metric = context.actorOf(MetricActor.props, metricName)
        metric.forward(storeAggregation)
      }

      context.child(metricName).fold(createAndStore())(_.forward(storeAggregation))

    case GetDevice =>
      val metricsList = context.children.map{
        _.path.name
      }.toVector
      sender() ! Device(self.path.name, metricsList)

    case GetMetric(metricName) =>
      context.child(metricName)
        .fold(sender() ! NoSuchMetric)(_.forward(MetricActor.GetMetric))

    case GetMetricAggregation(metricName, aggregationName) =>
      context.child(metricName)
        .fold(sender() ! NoSuchMetric)(_.forward(MetricActor.GetAggregation(aggregationName)))

    case DrawMetricAggregation(metricName, aggregationName) =>
      context.child(metricName)
        .fold(sender() ! NoSuchMetric)(_.forward(MetricActor.DrawAggregation(aggregationName)))
  }
}
