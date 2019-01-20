A REST application using Scala, SBT and akka.

### Create fat jar
sbt clean assembly

### Start fat jar
java -jar .\target\scala-2.11\dashboard-assembly-1.0.jar


### Manual test using 
> store new aggregation observation for some device and its metric

http POST localhost:5000/devices/device-0/metrics/cpuUsage/aggregations/AVERAGE value:=0.86

> list devices (only names)

http GET localhost:5000/devices/

> get device resource

http GET localhost:5000/devices/device-0

> get metric resource

http GET localhost:5000/devices/device-0/metrics/cpuUsage

> get aggregation resource

http GET localhost:5000/devices/device-0/metrics/cpuUsage/aggregations/AVERAGE

> chart aggregation as PNG

http GET localhost:5000/devices/device-0/metrics/cpuUsage/aggregations/AVERAGE/chart

