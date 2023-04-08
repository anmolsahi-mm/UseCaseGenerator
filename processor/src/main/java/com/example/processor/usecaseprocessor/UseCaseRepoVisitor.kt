package com.example.processor.usecaseprocessor

import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.processing.CodeGenerator
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

class UseCaseRepoVisitor(private val codeGenerator: CodeGenerator) : KSVisitorVoid() {

    private val functions = mutableListOf<Funcs>()

    @OptIn(KotlinPoetKspPreview::class)
    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
        classDeclaration.getDeclaredFunctions().forEach { it.accept(this, Unit) }
        val packageName = classDeclaration.packageName.asString()

        val constructorParameter = "$classDeclaration"
        val constructorParamName: String = buildString {
            append(constructorParameter[0].lowercase())
            append(constructorParameter.substring(1))
        }

        classDeclaration.getDeclaredFunctions().forEachIndexed { index, functionDeclaration ->
            val classString = "${functionDeclaration}UseCase"
            val className = buildString {
                append(classString[0].uppercase())
                append(classString.substring(1))
            }

            val fileSpec = FileSpec.builder(packageName, className).apply {
                addType(
                    TypeSpec.classBuilder(className)
                        .primaryConstructor(
                            FunSpec.constructorBuilder()
                                .addParameter(getConstructorParameter(constructorParamName, classDeclaration))
                                .build()
                        ).addProperty(
                            PropertySpec.builder(constructorParamName, getTypeName(classDeclaration))
                                .initializer(constructorParamName).build()
                        ).addFunction(getIndexedFunction(classDeclaration, index)).build()
                )
            }.build()
            fileSpec.writeTo(codeGenerator, true)
        }
    }

    @OptIn(KotlinPoetKspPreview::class)
    private fun getIndexedFunction(classDeclaration: KSClassDeclaration, index: Int): FunSpec {

        val indexedFunction = functions[index]

        val returnType = indexedFunction.returnType!!.toTypeName()

        val constructorParamName = "$classDeclaration"
        val paramName = buildString {
            append(constructorParamName[0].lowercase())
            append(constructorParamName.substring(1))
        }

        val listOfFunctionParams = mutableListOf<ParameterSpec>()
        var parametersToGenerate = ""

        indexedFunction.functionParams.forEach { functionParam ->
            val parameterName = functionParam.toString()
            parametersToGenerate += "$parameterName,"
            listOfFunctionParams.add(
                ParameterSpec.builder(parameterName, functionParam.type.toTypeName()).build()
            )
        }

        return FunSpec.builder("invoke")
            .addModifiers(KModifier.OPERATOR, KModifier.SUSPEND)
            .addParameters(
                listOfFunctionParams
            )
            .returns(returnType)
            .addStatement("return $paramName.${indexedFunction.functionName}(${parametersToGenerate.removeSuffix(",")})")
            .build()
    }

    private fun getConstructorParameter(
        constructorParamName: String,
        classDeclaration: KSClassDeclaration
    ): ParameterSpec {


        return ParameterSpec(constructorParamName, getTypeName(classDeclaration)).toBuilder()
            .addModifiers(KModifier.PRIVATE)
            .build()
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
