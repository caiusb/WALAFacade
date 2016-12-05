package edu.illinois.wala.ipa.callgraph.excluded

import java.util
import java.util.Collections

import com.ibm.wala.classLoader._
import com.ibm.wala.ipa.cha.IClassHierarchy
import com.ibm.wala.types._
import com.ibm.wala.util.strings.Atom

class ExcludedClass(tr: TypeReference, cha: IClassHierarchy) extends SyntheticClass(tr, cha) {

	override def getDirectInterfaces: util.Collection[_ <: IClass] = Collections.emptyList()

	override def getDeclaredMethods: util.Collection[IMethod] = Collections.emptyList()

	override def isReferenceType: Boolean = false

	override def getAllFields: util.Collection[IField] = Collections.emptyList()

	override def isPublic: Boolean = true

	override def getModifiers: Int = 0

	override def getClassInitializer: IMethod = null

	override def getAllMethods: util.Collection[IMethod] = Collections.emptyList()

	override def getAllImplementedInterfaces: util.Collection[IClass] = Collections.emptyList()

	override def getSuperclass: IClass = cha.getRootClass

	override def getField(name: Atom): IField = null

	override def getDeclaredInstanceFields: util.Collection[IField] = Collections.emptyList()

	override def getDeclaredStaticFields: util.Collection[IField] = Collections.emptyList()

	override def isPrivate: Boolean = false

	override def getAllInstanceFields: util.Collection[IField] = Collections.emptyList()

	override def getMethod(selector: Selector): IMethod = null

	override def getAllStaticFields: util.Collection[IField] = Collections.emptyList()

	override def getClassLoader(): IClassLoader = cha.getLoader(cha.getScope.getSyntheticLoader)
}
