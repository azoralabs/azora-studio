package dev.azora.sdk.core.io.di

import dev.azora.sdk.core.io.FileSystem
import org.koin.dsl.module

actual val platformCoreIoModule = module {
    single { FileSystem() }
}