package com.example.processor.usecaseprocessor

import com.example.annotations.SkipUseCase
import com.example.annotations.UseCaseRepo
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.validate

class UseCaseProcessor(private val codeGenerator: CodeGenerator) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val useCaseClass = UseCaseRepo::class.qualifiedName
        val skipUseCaseClass = SkipUseCase::class.qualifiedName

        val skipUseCaseFunctions = skipUseCaseClass?.let {
            resolver.getSymbolsWithAnnotation(it)
                .filterIsInstance<KSFunctionDeclaration>()
                .filter(KSFunctionDeclaration::validate)
        }

        val symbols = useCaseClass?.let {
            resolver.getSymbolsWithAnnotation(it)
                .filterIsInstance<KSClassDeclaration>()
                .filter(KSClassDeclaration::validate)
        }

        symbols?.forEach {
            if (skipUseCaseFunctions != null) {
                it.accept(UseCaseRepoVisitor(codeGenerator, skipUseCaseFunctions), Unit)
            }
        }

        return symbols?.filter { !it.validate() }?.toList() ?: emptyList()
    }
}