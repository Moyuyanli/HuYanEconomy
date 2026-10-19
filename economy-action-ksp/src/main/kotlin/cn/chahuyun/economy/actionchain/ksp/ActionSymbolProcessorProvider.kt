package cn.chahuyun.economy.actionchain.ksp

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

/** Entry point registered through `META-INF/services/com.google.devtools.ksp.processing.SymbolProcessorProvider`. */
class ActionSymbolProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        ActionSymbolProcessor(environment)
}
