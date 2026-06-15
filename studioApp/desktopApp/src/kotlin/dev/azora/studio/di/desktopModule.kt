package org.azora.studio.di

import org.azora.studio.az_script.*
import org.koin.dsl.module

val desktopModule = module {
    single<KotlinRunner> { DesktopKotlinRunner() }
    single<CSharpRunner> { DesktopCSharpRunner() }
    single<JavaScriptRunner> { DesktopJavaScriptRunner() }
    single<LlvmRunner> { DesktopLlvmRunner() }
}
