package com.example.processor.usecaseprocessor

import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.KotlinPoetKspPreview
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import com.squareup.kotlinpoet.typeNameOf

data class Funcs(val functionName: String, var returnType: KSTypeReference? = null)

class UseCaseRepoVisitor(private val codeGenerator: CodeGenerator, private val logger: KSPLogger) : KSVisitorVoid(){

    private val functions = mutableListOf<Funcs>()

    @OptIn(KotlinPoetKspPreview::class)
    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
        classDeclaration.getDeclaredFunctions().forEach { it.accept(this, Unit) }
        val packageName = classDeclaration.packageName.asString()
        val className = "${classDeclaration}UseCase"
        val fileSpec = FileSpec.builder(packageName, className).apply {
            addType(
                TypeSpec.classBuilder(className)
                    .primaryConstructor(
                        FunSpec.constructorBuilder()
                            .addParameter(getConstrucotrParams(classDeclaration)).build()
                    ).addProperty(
                        PropertySpec.builder("$classDeclaration".lowercase(), getTypeName(classDeclaration)).initializer("$classDeclaration".lowercase()).build()
                    ).addSuperinterface(getTypeName(classDeclaration))
                    .addModifiers(KModifier.OPEN, KModifier.PUBLIC)
                    .addFunctions(getAllFunctions(classDeclaration)).build()
            )
        }.build()
        fileSpec.writeTo(codeGenerator, true)
    }

    @OptIn(KotlinPoetKspPreview::class, ExperimentalStdlibApi::class)
    private fun getAllFunctions(classDeclaration: KSClassDeclaration): Iterable<FunSpec> {
        val listOfFunctions = mutableListOf<FunSpec>()
        functions.forEach {
            val returnType = it.returnType!!.toTypeName()
            val paramName = classDeclaration.toString().lowercase()
            listOfFunctions.add(
                FunSpec.builder(it.functionName)
                    .addModifiers(KModifier.OVERRIDE, KModifier.SUSPEND)
                    .addParameter(
                        ParameterSpec.builder("id", typeNameOf<String>()).build()
                    )
                    .returns(returnType)
                    .addStatement("return $paramName.${it.functionName}(id)")
                    .build()
            )
        }
        return listOfFunctions
    }

    private fun getConstrucotrParams(classDeclaration: KSClassDeclaration): ParameterSpec {
        return ParameterSpec("$classDeclaration".lowercase(), getTypeName(classDeclaration))
    }

    private fun getTypeName(classDeclaration: KSClassDeclaration): TypeName {
        return ClassName(classDeclaration.packageName.asString(), "$classDeclaration")
    }


    override fun visitFunctionDeclaration(function: KSFunctionDeclaration, data: Unit) {
        functions.add(Funcs(function.toString()))
        function.returnType!!.accept(this, Unit)
    }

    override fun visitTypeReference(typeReference: KSTypeReference, data: Unit) {
        functions.firstOrNull {
            it.functionName == typeReference.parent.toString()
        }?.returnType = typeReference
        super.visitTypeReference(typeReference, data)
    }
}
