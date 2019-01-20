package hr.fer.rassus.dashboard

import akka.actor.{Actor, Props}

object NetworkActor {
  def props = Props(new NetworkActor)

  // input messages
  case class StoreDatagram(deviceName: String, metricName: String, aggregationName: String, value: Double)
  case object GetDevices
  case class GetDevice(deviceName: String)
  case class GetDeviceMetric(deviceName: String, metricName: String)
  case class GetDeviceMetricAggregation(deviceName: String, metricName: String, aggregationName: String)
  case class DrawDeviceMetricAggregation(deviceName: String, metricName: String, aggregationName: String)

  // output messages
  case class Devices(devices: Vector[String])
  case object NoSuchDevice
}

class NetworkActor extends Actor {

  import NetworkActor._

  def receive: Receive = {
    case StoreDatagram(deviceName, metricName, aggregationName, value) =>
      val storeMetric = DeviceActor.StoreMetric(metricName, aggregationName, value)

      def createAndStore(): Unit = {
        val device = context.actorOf(DeviceActor.props, deviceName)
        device.forward(storeMetric)
      }

      context.child(deviceName).fold(createAndStore())(_.forward(storeMetric))


    case GetDevices =>
      val devicesList = context.children.map {
        _.path.name
      }.toVector
      sender() ! Devices(devicesList)


    case GetDevice(deviceName) =>
      context.child(deviceName)
        .fold(sender() ! NoSuchDevice)(_.forward(DeviceActor.GetDevice))

    case GetDeviceMetric(deviceName, metricName) =>
      context.child(deviceName)
        .fold(sender() ! NoSuchDevice)(_.forward(DeviceActor.GetMetric(metricName)))

    case GetDeviceMetricAggregation(deviceName, metricName, aggregationName) =>
      context.child(deviceName)
        .fold(sender() ! NoSuchDevice)(_.forward(DeviceActor.GetMetricAggregation(metricName, aggregationName)))

    case DrawDeviceMetricAggregation(deviceName, metricName, aggregationName) =>
      context.child(deviceName)
        .fold(sender() ! NoSuchDevice)(_.forward(DeviceActor.DrawMetricAggregation(metricName, aggregationName)))
  }
}

