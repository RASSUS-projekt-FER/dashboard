package hr.fer.rassus.dashboard

import akka.actor._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.ExecutionContext

class RestApi(system: ActorSystem, timeout: Timeout)
    extends RestRoutes {
  implicit val requestTimeout: Timeout = timeout
  implicit def executionContext: ExecutionContext = system.dispatcher

  override def createNetworkActor(): ActorRef = system.actorOf(NetworkActor.props, "root")
}

trait RestRoutes extends NetworkActorApi with EventMarshalling {
  
  import StatusCodes._

  def routes: Route = devicesRoute ~ deviceRoute ~ metricRoute ~
    aggregationRoute ~ aggregationChartRoute ~ indexRoute

  def devicesRoute: Route =
    pathPrefix("devices") {
      pathEndOrSingleSlash {
        get {
          onSuccess(getDevices()) {
            case devices: NetworkActor.Devices => complete(OK, devices)
            case _ => complete(InternalServerError)
          }
        }
      }
    }

  def deviceRoute: Route =
    pathPrefix("devices"/ Segment ) { deviceName =>
      pathEndOrSingleSlash {
        get {
          onSuccess(getDevice(deviceName)) {
            case device: DeviceActor.Device => complete(OK, device)
            case NetworkActor.NoSuchDevice =>
              complete(NotFound, ErrorMessage(s"No device with name: $deviceName"))
            case _ => complete(InternalServerError)
          }
        }
      }
    }

  def metricRoute: Route =
    pathPrefix("devices"/ Segment / "metrics" / Segment ) { (deviceName, metricName) =>
      pathEndOrSingleSlash {
        get {
          onSuccess(getMetric(deviceName, metricName)) {
            case metric: MetricActor.Metric => complete(OK, metric)
            case NetworkActor.NoSuchDevice =>
              complete(NotFound, ErrorMessage(s"No device named: $deviceName"))
            case DeviceActor.NoSuchMetric =>
              complete(NotFound, ErrorMessage(s"No metric named: $metricName, for device: $deviceName"))
            case _ => complete(InternalServerError)
          }
        }
      }
    }

  def aggregationRoute: Route =
    pathPrefix("devices"/ Segment / "metrics" / Segment / "aggregations" / Segment) {
      (deviceName, metricName, aggregationName) =>
        pathEndOrSingleSlash {
          get {
            onSuccess(getAggregation(deviceName, metricName, aggregationName)) {
              case aggregation: MetricActor.Aggregation => complete(OK, aggregation)
              case NetworkActor.NoSuchDevice =>
                complete(NotFound, ErrorMessage(s"No device named: $deviceName"))
              case DeviceActor.NoSuchMetric =>
                complete(NotFound, ErrorMessage(s"No metric named: $metricName, for device: $deviceName"))
              case MetricActor.NoSuchAggregation =>
                complete(NotFound, ErrorMessage(s"No aggregation named: $aggregationName, for device: $deviceName, " +
                  s"and metric: $metricName"))
              case _ => complete(InternalServerError)
            }
          }~
          post {
            entity(as[AggregationObservation]) { observation =>
              onSuccess(storeDeviceMetricAggregation(deviceName, metricName, aggregationName, observation)) {
                case MetricActor.StoreAggregationSuccess => complete(OK, observation)
                case _ => complete(InternalServerError)
              }
            }
          }
        }
    }

  def aggregationChartRoute: Route =
    pathPrefix("devices"/ Segment / "metrics" / Segment / "aggregations" / Segment / "chart") {
      (deviceName, metricName, aggregationName) =>
        pathEndOrSingleSlash {
          onSuccess(drawAggregation(deviceName, metricName, aggregationName)) {
            case MetricActor.DrawAggregationResult(fileLocation) =>
              getFromFile(fileLocation)
            case NetworkActor.NoSuchDevice =>
              complete(NotFound, ErrorMessage(s"No device named: $deviceName"))
            case DeviceActor.NoSuchMetric =>
              complete(NotFound, ErrorMessage(s"No metric named: $metricName, for device: $deviceName"))
            case MetricActor.NoSuchAggregation =>
              complete(NotFound, ErrorMessage(s"No aggregation named: $aggregationName, for device: $deviceName, " +
                s"and metric: $metricName"))
            case _ => complete(InternalServerError)
          }
        }
    }

  def indexRoute: Route = {
    pathPrefix("index") {
      pathEndOrSingleSlash {
        getFromFile("static/index.html")
      }
    }
  }
}

trait NetworkActorApi {
  import NetworkActor._

  def createNetworkActor(): ActorRef

  implicit def executionContext: ExecutionContext
  implicit def requestTimeout: Timeout

  lazy val networkActor = createNetworkActor()

  def storeDeviceMetricAggregation(deviceName: String, metricName: String,
                                   aggregationName: String, observation: AggregationObservation) =
    networkActor ? StoreDatagram(deviceName, metricName, aggregationName, observation.value)

  def getDevices() =
    networkActor ? GetDevices

  def getDevice(deviceName: String) =
    networkActor ? GetDevice(deviceName)

  def getMetric(deviceName: String, metricName: String) =
    networkActor ? GetDeviceMetric(deviceName, metricName)

  def getAggregation(deviceName: String, metricName: String, aggregationName: String) =
    networkActor ? GetDeviceMetricAggregation(deviceName, metricName, aggregationName)

  def drawAggregation(deviceName: String, metricName: String, aggregationName: String) =
    networkActor ? DrawDeviceMetricAggregation(deviceName, metricName, aggregationName)

}
