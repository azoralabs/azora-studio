package dev.azora.studio.di

import dev.azora.local.database.di.coreDatabaseModule
import org.koin.core.module.Module

actual val initCache: Module = coreDatabaseModule
