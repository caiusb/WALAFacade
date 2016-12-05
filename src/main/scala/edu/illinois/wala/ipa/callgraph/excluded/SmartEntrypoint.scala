package edu.illinois.wala.ipa.callgraph.excluded

import java.util.logging.{Level, Logger}

import com.ibm.wala.classLoader.IMethod
import com.ibm.wala.ipa.callgraph.impl.SubtypesEntrypoint
import com.ibm.wala.ipa.cha.IClassHierarchy
import com.ibm.wala.ipa.summaries.BypassSyntheticClassLoader
import com.ibm.wala.types.{MethodReference, TypeReference}

class SmartEntrypoint(mr: MethodReference, cha: IClassHierarchy) extends SubtypesEntrypoint(mr, cha) {

	private lazy val syntheticLoaderReference = cha.getScope.getSyntheticLoader
	private lazy val syntheticLoader = getCha.getLoader(syntheticLoaderReference).asInstanceOf[BypassSyntheticClassLoader]
	private lazy val logger = Logger.getLogger("com.brindescu.slicer")

	override def makeParameterTypes(m: IMethod, i: Int): Array[TypeReference] = {
		val nominal = m.getParameterType(i)
		if (nominal.isArrayType) {
			val actualType = nominal.getInnermostElementType
			if (actualType.isReferenceType || actualType.isClassType)
				registerIfAbsent(actualType)
			val loader = cha.getScope.getArrayClassLoader
			if (getCha.lookupClass(nominal) == null) {
				val k = loader.lookupClass(actualType.getName, syntheticLoader, getCha)
				assert(k != null, "Could not create array class " + nominal.getName)
				syntheticLoader.registerClass(k.getName, k)
				assert(getCha.lookupClass(nominal) != null, "Failed creating a synthetic class for excluded " + nominal.getName)
			}
		}
		else if (nominal.isClassType || nominal.isReferenceType)
			registerIfAbsent(nominal)

		return super.makeParameterTypes(m, i)
	}

	private def registerIfAbsent(nominal: TypeReference) = {
		val k = getCha.lookupClass(nominal)
		if (k == null) {
			val correct = TypeReference.findOrCreate(syntheticLoaderReference, nominal.getName)
			syntheticLoader.registerClass(correct.getName, new ExcludedClass(correct, getCha))
			assert(getCha.lookupClass(nominal) != null, "Failed creating a synthetic class for excluded " + nominal.getName)
			logger.log(Level.INFO, "Successfully loaded synthetic class for " + nominal.getName)
		}
	}
}