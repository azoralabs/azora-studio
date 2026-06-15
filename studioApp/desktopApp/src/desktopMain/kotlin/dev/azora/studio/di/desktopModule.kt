package dev.azora.studio.di

import org.koin.dsl.module

/**
 * Desktop-specific DI bindings.
 *
 * The AzScript language runners (Kotlin/C#/JS/LLVM) were removed while the Azora
 * language toolchain is disabled; this module is intentionally empty for now.
 */
val desktopModule = module {
}
