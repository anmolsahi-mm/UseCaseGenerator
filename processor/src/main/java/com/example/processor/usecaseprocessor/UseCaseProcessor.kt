package com.example.processor.usecaseprocessor

import com.example.annotations.UseCaseRepo
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.validate

class UseCaseProcessor(private val codeGenerator: CodeGenerator, private val logger: KSPLogger): SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = UseCaseRepo::class.qualifiedName?.let { decoratorClass ->
            resolver.getSymbolsWithAnnotation(decoratorClass)
        }
        symbols?.let { symbol ->
            symbol.filter { it is KSClassDeclaration && it.validate() }.forEach {
                it.accept(UseCaseRepoVisitor(codeGenerator), Unit)
            }
        }
        return symbols?.filter { !it.validate() }?.toList() ?: emptyList()
    }
}