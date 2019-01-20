package hr.fer.rassus.dashboard

import spray.json._

case class AggregationObservation(value: Double)
case class ErrorMessage(message: String)

trait EventMarshalling extends DefaultJsonProtocol {
  implicit val storeDatagramFormat = jsonFormat1(AggregationObservation)
  implicit val devicesFormat = jsonFormat1(NetworkActor.Devices)
  implicit val metricsFormat = jsonFormat2(DeviceActor.Device)
  implicit val aggregationsFormat = jsonFormat2(MetricActor.Metric)
  implicit val aggregationFormat = jsonFormat2(MetricActor.Aggregation)
  implicit val errorMessageFormat = jsonFormat1(ErrorMessage.apply)
}
