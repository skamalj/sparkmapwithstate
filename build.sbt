// Name of the package
name := "mapwithstate"
// Version of our package
version := "1.0"
// Version of Scala
scalaVersion := "2.12.10"
val sparkVersion = "3.1.2"
// Spark library dependencies
libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % sparkVersion,
  "org.apache.spark" %% "spark-sql"  % sparkVersion,
  "org.apache.spark" %% "spark-streaming"  % sparkVersion
)