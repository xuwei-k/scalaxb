import sbt._
import Keys._
import sbtbuildinfo.BuildInfoKey
import sbtbuildinfo.BuildInfoKeys._

object Common {
  val Xsd = config("xsd") extend(Compile)
  val Wsdl = config("wsdl") extend(Compile)
  val Soap11 = config("soap11") extend(Compile)
  val Soap12 = config("soap12") extend(Compile)

  val scalaxbCodegenSettings: Seq[Def.Setting[_]] = {
    import sbtscalaxb.ScalaxbPlugin._
    import sbtscalaxb.ScalaxbPlugin.autoImport._
    def customScalaxbSettings(base: String): Seq[Def.Setting[_]] = Seq(
      sources := Seq(scalaxbXsdSource.value / (base + ".xsd")),
      sourceManaged := baseDirectory.value / "src_managed",
      scalaxbPackageName := base,
      scalaxbProtocolFileName := base + "_xmlprotocol.scala",
      scalaxbClassPrefix := Some("X")
    )

    def soapSettings(base: String): Seq[Def.Setting[_]] = Seq(
      sources := Seq(scalaxbXsdSource.value / (base + ".xsd")),
      sourceManaged := sourceDirectory.value / "main" / "resources",
      scalaxbPackageName := base,
      scalaxbProtocolFileName := base + "_xmlprotocol.scala",
      scalaxbPackageDir := false,
      scalaxbGenerate := {
        val files = scalaxbGenerate.value
        val renamed = files map { file => new File(file.getParentFile, file.getName + ".template") }
        IO.move(files zip renamed)
        renamed
      }
    )

    inConfig(Xsd)(baseScalaxbSettings ++ inTask(scalaxb)(customScalaxbSettings("xmlschema"))) ++
    inConfig(Wsdl)(baseScalaxbSettings ++ inTask(scalaxb)(customScalaxbSettings("wsdl11"))) ++
    inConfig(Soap11)(baseScalaxbSettings ++ inTask(scalaxb)(soapSettings("soapenvelope11"))) ++
    inConfig(Soap12)(baseScalaxbSettings ++ inTask(scalaxb)(soapSettings("soapenvelope12")))
  }

  val codegenSettings: Seq[Def.Setting[_]] = scalaxbCodegenSettings ++ Seq(
    Compile / unmanagedSourceDirectories += baseDirectory.value / "src_managed",
    buildInfoPackage := "scalaxb",
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion,
      "defaultDispatchVersion" -> Dependencies.defaultDispatchVersion,
      "defaultHttp4sVersion" -> Dependencies.defaultHttp4sVersion,
      "defaultGigahorseVersion" -> Dependencies.defaultGigahorseVersion,
      "defaultGigahorseBackend" -> Dependencies.defaultGigahorseBackend),
  )

  val sonatypeSettings: Seq[Def.Setting[_]] = Seq(
    Test / publishArtifact := false,
    resolvers ++= Seq(
      "sonatype-public" at "https://oss.sonatype.org/content/repositories/public"),
    publishTo := {
      val v = version.value
      val nexus = "https://oss.sonatype.org/"
      if (v.trim.endsWith("SNAPSHOT")) Some("snapshots" at nexus + "content/repositories/snapshots")
      else Some("releases"  at nexus + "service/local/staging/deploy/maven2")
    }
  )
}
