package sbtscalaxb

import sbt.{given, _}
import Keys._
import scalaxb.{compiler => sc}
import scalaxb.compiler.{Config => ScConfig}
import sc.ConfigEntry
import sc.ConfigEntry.{HttpClientStyle => _, _}

object ScalaxbPlugin extends sbt.AutoPlugin {
  override def requires = plugins.JvmPlugin

  object autoImport extends ScalaxbKeys
  import autoImport._
  override lazy val globalSettings: Seq[Def.Setting[_]] = Seq[Setting[_]](
    scalaxbPackageName             := "generated",
    scalaxbPackageNames            := Map.empty,
    scalaxbClassPrefix             := None,
    scalaxbParamPrefix             := None,
    scalaxbAttributePrefix         := None,
    scalaxbOpOutputWrapperPostfix  := sc.Defaults.opOutputWrapperPostfix,
    scalaxbPrependFamily           := false,
    scalaxbWrapContents            := Nil,
    scalaxbContentsSizeLimit       := Int.MaxValue,
    scalaxbChunkSize               := 10,
    scalaxbNamedAttributes         := false,
    scalaxbPackageDir              := true,
    scalaxbGenerateRuntime         := true,
    scalaxbGenerateDispatchClient  := true,
    scalaxbGenerateDispatchAs      := false,
    scalaxbGenerateHttp4sClient    := false,
    scalaxbGenerateGigahorseClient := false,
    scalaxbGenerateSingleClient    := HttpClientType.None,
    scalaxbProtocolFileName        := sc.Defaults.protocolFileName,
    scalaxbProtocolPackageName     := None,
    scalaxbLaxAny                  := false,
    scalaxbDispatchVersion         := ScConfig.defaultDispatchVersion.value,
    scalaxbGigahorseVersion        := ScConfig.defaultGigahorseVersion.value,
    scalaxbGigahorseBackend        := GigahorseHttpBackend.OkHttp,
    scalaxbHttp4sVersion           := ScConfig.defaultHttp4sVersion.value,
    scalaxbMapK                    := false,
    scalaxbIgnoreUnknown           := false,
    scalaxbVararg                  := false,
    scalaxbGenerateMutable         := false,
    scalaxbGenerateVisitor         := false,
    scalaxbGenerateLens            := false,
    scalaxbAutoPackages            := false,
    scalaxbCapitalizeWords         := false,
    scalaxbSymbolEncodingStrategy  := SymbolEncodingStrategy.Legacy151,
    scalaxbEnumNameMaxLength       := 50,
    scalaxbUseLists                := false,
    scalaxbAsync                   := true,
    scalaxbJaxbPackage             := JaxbPackage.Javax,
  )

  override lazy val projectSettings: Seq[Def.Setting[_]] =
    inConfig(Compile)(baseScalaxbSettings) ++
    Set(
      Compile / sourceGenerators += (Compile / scalaxb).taskValue
    )
  lazy val baseScalaxbSettings: Seq[Def.Setting[_]] = Seq[Setting[_]](
    scalaxb := (scalaxb / scalaxbGenerate).value,
    scalaxb / sourceManaged := {
      sourceManaged.value / "sbt-scalaxb"
    },
    scalaxb / scalaxbXsdSource := {
      val src = sourceDirectory.value
      if (Seq(Compile, Test) contains configuration.value) src / "xsd"
      else src / "main" / "xsd"
    },
    scalaxb / scalaxbWsdlSource := {
      val src = sourceDirectory.value
      if (Seq(Compile, Test) contains configuration.value) src / "wsdl"
      else src / "main" / "wsdl"
    },
    scalaxb / logLevel := (logLevel?? Level.Info).value
  ) ++ Project.inTask(scalaxb)(Seq[Setting[_]](
    scalaxbGenerate := {
      val s = streams.value
      val ll = logLevel.value
      ScalaxbCompile(sources.value, scalaxbConfig.value, sourceManaged.value, s.cacheDirectory, ll == Level.Debug)
    },
    sources := {
      val xsd = scalaxbXsdSource.value
      val wsdl = scalaxbWsdlSource.value
      (wsdl ** "*.wsdl").get().sorted ++ (xsd ** "*.xsd").get().sorted
    },
    clean := {
      val outdir = sourceManaged.value
      IO.delete((outdir ** "*").get())
      IO.createDirectory(outdir)
    },
    scalaxbCombinedPackageNames := {
      val x = scalaxbPackageName.value
      val xs = scalaxbPackageNames.value
      (xs map { case (k, v) => ((Some(k.toString): Option[String]), Some(v)) }) updated (None, Some(x))
    },
    scalaxbHttpClientStyle := {
      (scalaxbHttpClientStyle.?.value) match {
        case Some(x) => x
        case _ =>
          if (scalaxbGenerateHttp4sClient.value) HttpClientStyle.Tagless
          else if (scalaxbAsync.value) HttpClientStyle.Future
          else HttpClientStyle.Sync
      }
    },
    scalaxbConfig := _root_.scalaxb.compiler.Config.apply {
        val xxx = (
          Vector[ConfigEntry](PackageNames(scalaxbCombinedPackageNames.value)) ++
            (if (scalaxbPackageDir.value) Vector[ConfigEntry](GeneratePackageDir) else Vector[ConfigEntry]()) ++
            (scalaxbClassPrefix.value match {
              case Some(x) => Vector[ConfigEntry](ClassPrefix(x))
              case None => Vector[ConfigEntry]()
            }) ++
            (scalaxbParamPrefix.value match {
              case Some(x) => Vector[ConfigEntry](ParamPrefix(x))
              case None => Vector[ConfigEntry]()
            }) ++
            (scalaxbAttributePrefix.value match {
              case Some(x) => Vector[ConfigEntry](AttributePrefix(x))
              case None => Vector[ConfigEntry]()
            }) ++
            Vector[ConfigEntry](OpOutputWrapperPostfix(scalaxbOpOutputWrapperPostfix.value)) ++
            Vector[ConfigEntry](ScConfig.defaultOutdir) ++
            (if (scalaxbPrependFamily.value) Vector[ConfigEntry](PrependFamilyName) else Vector[ConfigEntry]()) ++
            Vector[ConfigEntry](WrappedComplexTypes(scalaxbWrapContents.value.toList)) ++
            Vector[ConfigEntry](SeperateProtocol) ++
            Vector[ConfigEntry](ProtocolFileName(scalaxbProtocolFileName.value)) ++
            Vector[ConfigEntry](ProtocolPackageName(scalaxbProtocolPackageName.value)) ++
            Vector[ConfigEntry](ScConfig.defaultDefaultNamespace) ++
            (if (scalaxbGenerateRuntime.value) Vector[ConfigEntry](GenerateRuntime) else Vector[ConfigEntry]()) ++
            (if (scalaxbGenerateDispatchClient.value && scalaxbGenerateSingleClient.value == HttpClientType.None ||
              scalaxbGenerateSingleClient.value == HttpClientType.Dispatch) Vector[ConfigEntry](GenerateDispatchClient) else Vector[ConfigEntry]()) ++
            (if (scalaxbGenerateDispatchAs.value) Vector[ConfigEntry](GenerateDispatchAs) else Vector[ConfigEntry]()) ++
            (if (scalaxbGenerateGigahorseClient.value && scalaxbGenerateSingleClient.value == HttpClientType.None ||
              scalaxbGenerateSingleClient.value == HttpClientType.Gigahorse) Vector[ConfigEntry](GenerateGigahorseClient) else Vector[ConfigEntry]()) ++
            (if (scalaxbGenerateHttp4sClient.value && scalaxbGenerateSingleClient.value == HttpClientType.None ||
              scalaxbGenerateSingleClient.value == HttpClientType.Http4s) Vector[ConfigEntry](GenerateHttp4sClient, ConfigEntry.HttpClientStyle.Tagless) else Vector[ConfigEntry]()) ++
            Vector[ConfigEntry](ContentsSizeLimit(scalaxbContentsSizeLimit.value)) ++
            Vector[ConfigEntry](SequenceChunkSize(scalaxbChunkSize.value)) ++
            (if (scalaxbNamedAttributes.value) Vector[ConfigEntry](NamedAttributes) else Vector[ConfigEntry]()) ++
            (if (scalaxbLaxAny.value) Vector[ConfigEntry](LaxAny) else Vector[ConfigEntry]()) ++
            Vector[ConfigEntry](DispatchVersion(scalaxbDispatchVersion.value)) ++
            Vector[ConfigEntry](Http4sVersion(scalaxbHttp4sVersion.value)) ++
            Vector[ConfigEntry](GigahorseVersion(scalaxbGigahorseVersion.value)) ++
            Vector[ConfigEntry](GigahorseBackend(scalaxbGigahorseBackend.value.toString)) ++
            (if (scalaxbIgnoreUnknown.value) Vector[ConfigEntry](IgnoreUnknown) else Vector[ConfigEntry]()) ++
            (if (scalaxbVararg.value && !scalaxbGenerateMutable.value) Vector[ConfigEntry](VarArg) else Vector[ConfigEntry]()) ++
            (if (scalaxbGenerateMutable.value) Vector[ConfigEntry](GenerateMutable) else Vector[ConfigEntry]()) ++
            (if (scalaxbGenerateVisitor.value) Vector[ConfigEntry](GenerateVisitor) else Vector[ConfigEntry]()) ++
            (if (scalaxbGenerateLens.value) Vector[ConfigEntry](GenerateLens) else Vector[ConfigEntry]()) ++
            (if (scalaxbAutoPackages.value) Vector[ConfigEntry](AutoPackages) else Vector[ConfigEntry]()) ++
            (if (scalaxbCapitalizeWords.value) Vector[ConfigEntry](CapitalizeWords) else Vector[ConfigEntry]()) ++
            Vector[ConfigEntry](SymbolEncoding.withName(scalaxbSymbolEncodingStrategy.value.toString)) ++
            Vector[ConfigEntry](EnumNameMaxLength(scalaxbEnumNameMaxLength.value)) ++
            (if (scalaxbMapK.value) Vector[ConfigEntry](GenerateMapK) else Vector[ConfigEntry]()) ++
            (if (scalaxbUseLists.value) Vector[ConfigEntry](UseLists) else Vector[ConfigEntry]()) ++
            Vector[ConfigEntry](TargetScalaVersion(scalaVersion.value)) ++
            Vector[ConfigEntry](ConfigEntry.JaxbPackage.withPackageName(scalaxbJaxbPackage.value.toString)) ++
            Vector[ConfigEntry](scalaxbHttpClientStyle.value match {
              case HttpClientStyle.Sync => ConfigEntry.HttpClientStyle.Sync
              case HttpClientStyle.Future => ConfigEntry.HttpClientStyle.Future
              case HttpClientStyle.Tagless => ConfigEntry.HttpClientStyle.Tagless
            })
          )

        xxx : Vector[ConfigEntry]

        Vector.empty[ConfigEntry]
      }
  ))
}
