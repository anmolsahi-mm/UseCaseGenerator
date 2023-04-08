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

class UseCaseRepoVisitor(private val codeGenerator: CodeGenerator, private val skipUseCaseSymbols: Sequence<KSFunctionDeclaration>) : KSVisitorVoid() {

    private val functions = mutableListOf<Funcs>()

    @OptIn(KotlinPoetKspPreview::class)
    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
        classDeclaration.getDeclaredFunctions().forEach { it.accept(this, Unit) }
        val packageName = classDeclaration.packageName.asString()

        val constructorParameter = getConstructorParamName(classDeclaration)

        val qualifiedFunctionDeclarations = compareSequences(classDeclaration.getDeclaredFunctions(), skipUseCaseSymbols)

        qualifiedFunctionDeclarations.forEachIndexed { index, functionDeclaration ->
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
                                .addParameter(getConstructorParameter(constructorParameter, classDeclaration))
                                .build()
                        ).addProperty(
                            PropertySpec.builder(constructorParameter, getTypeName(classDeclaration))
                                .initializer(constructorParameter).build()
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

        val constructorParamName = getConstructorParamName(classDeclaration)

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
            .addStatement("return $constructorParamName.${indexedFunction.functionName}(${parametersToGenerate.removeSuffix(",")})")
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

    private fun getConstructorParamName(classDeclaration: KSClassDeclaration): String {
       val param =  "$classDeclaration"
        return buildString {
            append(param[0].lowercase())
            append(param.substring(1))
        }
    }

    private fun getTypeName(classDeclaration: KSClassDeclaration): TypeName {
        return ClassName(classDeclaration.packageName.asString(), "$classDeclaration")
    }

    private fun compareSequences(
        firstSeq: Sequence<KSFunctionDeclaration>,
        secondSeq: Sequence<KSFunctionDeclaration>
    ): Sequence<KSFunctionDeclaration> {
        val secondSet = secondSeq.toSet()
        return firstSeq.filterNot { secondSet.contains(it) }
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
