package dev.azora.sdk.core.io.di

import org.koin.core.module.Module
import org.koin.dsl.module

expect val platformCoreIoModule: Module

val coreIoModule = module {
    includes(platformCoreIoModule)
}