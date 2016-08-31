name := "gen-picklist"

version := "1.0.0"

organization := "com.abb"

scalaVersion := "2.11.7"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")

libraryDependencies ++= Seq(
  "org.scalikejdbc" %% "scalikejdbc"               % "2.4.0",
  "org.scalikejdbc" %% "scalikejdbc-interpolation" % "2.4.0",
  "org.scalikejdbc" %% "scalikejdbc-config"        % "2.4.0",
  "org.slf4s"       %% "slf4s-api"                 % "1.7.12",
  "org.scalatest"   %% "scalatest"                 % "3.0.0" % "test",
  "ch.qos.logback"  %  "logback-classic"           % "1.1.3",
  "org.xerial"      % "sqlite-jdbc"                % "3.7.2"
)

scalikejdbcSettings
