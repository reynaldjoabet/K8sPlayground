
name:= "K8sPlayground"

version:= "0.1"

scalaVersion:= "3.3.6"


val root = project.in(file("."))
.settings(
    libraryDependencies ++= Seq(
        "org.scalameta" %% "munit" % "0.7.29"%Test,
    )
)

