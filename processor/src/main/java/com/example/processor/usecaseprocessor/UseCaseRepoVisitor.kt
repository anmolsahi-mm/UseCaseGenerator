package com.example.processor.usecaseprocessor

import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ksp.KotlinPoetKspPreview
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo

data class Funcs(
    val functionName: String,
    val functionParams: List<KSValueParameter>,
    var returnType: KSTypeReference? = null
)

class UseCaseRepoVisitor(private val codeGenerator: CodeGenerator, private val logger: KSPLogger) : KSVisitorVoid() {

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
                            .addParameter(getConstrucotrParams(classDeclaration))
                            .build()
                    ).addProperty(
                        PropertySpec.builder("$classDeclaration".lowercase(), getTypeName(classDeclaration))
                            .initializer("$classDeclaration".lowercase()).build()
                    ).addModifiers(KModifier.OPEN)
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
            val listOfFunctionParams = mutableListOf<ParameterSpec>()
            var parametersToGenerate = ""

            it.functionParams.forEach { functionParam ->
                val parameterName = functionParam.toString()
                parametersToGenerate += "$parameterName,"
                listOfFunctionParams.add(
                    ParameterSpec.builder(parameterName, functionParam.type.toTypeName()).build()
                )
            }

            listOfFunctions.add(
                FunSpec.builder("invoke")
                    .addModifiers(KModifier.OPERATOR, KModifier.SUSPEND)
                    .addParameters(
                        listOfFunctionParams
                    )
                    .returns(returnType)
                    .addStatement("return $paramName.${it.functionName}($parametersToGenerate)")
                    .build()
            )
        }
        return listOfFunctions
    }

    private fun getConstrucotrParams(classDeclaration: KSClassDeclaration): ParameterSpec {
        return ParameterSpec("$classDeclaration".lowercase(), getTypeName(classDeclaration)).toBuilder()
            .addModifiers(KModifier.PRIVATE).build()
    }

    private fun getTypeName(classDeclaration: KSClassDeclaration): TypeName {
        return ClassName(classDeclaration.packageName.asString(), "$classDeclaration")
    }


    override fun visitFunctionDeclaration(function: KSFunctionDeclaration, data: Unit) {
        functions.add(Funcs(function.toString(), function.parameters))
        function.returnType!!.accept(this, Unit)
    }

    override fun visitTypeReference(typeReference: KSTypeReference, data: Unit) {
        functions.firstOrNull {
            it.functionName == typeReference.parent.toString()
        }?.returnType = typeReference
        super.visitTypeReference(typeReference, data)
    }
}
