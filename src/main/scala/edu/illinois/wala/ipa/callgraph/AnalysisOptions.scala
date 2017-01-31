package edu.illinois.wala.ipa.callgraph

import com.ibm.wala.cast.java.translator.jdt.ecj.ECJClassLoaderFactory
import com.ibm.wala.classLoader.Language
import com.ibm.wala.ipa.callgraph.Entrypoint
import com.ibm.wala.ipa.callgraph.impl.DefaultEntrypoint
import com.ibm.wala.ipa.cha.{ClassHierarchy, ClassHierarchyFactory}
import com.ibm.wala.types.{MethodReference, TypeName, TypeReference}
import com.typesafe.config.{Config, ConfigFactory}
import edu.illinois.wala.ipa.callgraph.ConfigConstants._
import edu.illinois.wala.ipa.callgraph.excluded.SmartEntrypoint

import scala.collection.JavaConversions._


class AnalysisOptions(scope: AnalysisScope, entrypoints: java.lang.Iterable[Entrypoint], val cha: ClassHierarchy, val isSourceAnalysis: Boolean)
  extends com.ibm.wala.ipa.callgraph.AnalysisOptions(scope, entrypoints) {
}

object AnalysisOptions {

  // TODO: replace below to use the above class

  def apply(
    extraEntrypoints: Iterable[(String, String)],
    extraDependencies: Iterable[Dependency])(
      implicit config: Config): AnalysisOptions = {

    implicit val scope = AnalysisScope(extraDependencies)

    implicit val cha = getClassHierarchy

    new AnalysisOptions(scope, entrypoints(extraEntrypoints), cha, true) // !srcDep.isEmpty
  }

  def getClassHierarchy(implicit scope: AnalysisScope): ClassHierarchy = {
    val classLoaderFactory = new ECJClassLoaderFactory(scope.getExclusions())

    ClassHierarchyFactory.make(scope, classLoaderFactory, Language.JAVA)
  }

  def entrypoints(extraEntrypoints: Iterable[(String, String)] = Seq())(
    implicit config: Config, cha: ClassHierarchy, scope: AnalysisScope) = {

    val oneEntryPoint =
    if (config.hasPath(EntryClass)) {
        Some((config.getString(EntryClass), config.getString(EntryMethod)))
      }
    else
    None

    val multipleEntryPoints =
      if (config.hasPath(MultipleEntryPoints))
        config.getConfigList(MultipleEntryPoints) map { v => (v.getString("class"), v.getString("method")) }
      else
        Seq()

    val entryPointsFromPattern =
      if (config.hasPath(EntrySignaturePattern)) {
        val signaturePattern = config.getString(EntrySignaturePattern)
        val matchingMethods = cha.iterator() flatMap { c =>
          c.getAllMethods() filter { m =>
            m.getSignature() matches signaturePattern
          }
        }
        matchingMethods map { new DefaultEntrypoint(_, cha) } toSeq
      } else
        Seq()

    val entrypoints = entryPointsFromPattern ++
      ((extraEntrypoints ++ oneEntryPoint ++ multipleEntryPoints) map { case (klass, method) => makeEntrypoint(klass, method) })
        .filter { _.isDefined }.map{ _.get }

    if (entrypoints.size == 0)
      System.err.println("WARNING: no entrypoints")

    entrypoints
  }

  // helper apply methods

  def apply()(implicit config: Config = ConfigFactory.load): AnalysisOptions = {
    apply(Seq(), Seq())
  }

  def apply(klass: String, method: String)(implicit config: Config): AnalysisOptions = apply((klass, method), Seq())

  def apply(entrypoint: (String, String),
    dependencies: Iterable[Dependency])(implicit config: Config): AnalysisOptions = apply(Seq(entrypoint), dependencies)

  val mainMethod = "main([Ljava/lang/String;)V"

  private def makeEntrypoint(entryClass: String, entryMethod: String)(implicit scope: AnalysisScope, cha: ClassHierarchy): Option[Entrypoint] =
    scope.getLoaders.toStream
      .map { TypeReference.findOrCreate(_, TypeName.string2TypeName(entryClass)) }
      .map { MethodReference.findOrCreate(_, entryMethod.substring(0, entryMethod.indexOf('(')), entryMethod.substring(entryMethod.indexOf('('))) }
      .find { cha.resolveMethod(_) != null } match {
        case Some(m) => Some(new SmartEntrypoint(m, cha))
        case None => None
      }

}