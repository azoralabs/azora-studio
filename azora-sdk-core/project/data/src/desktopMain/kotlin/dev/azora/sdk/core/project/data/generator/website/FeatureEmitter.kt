package dev.azora.sdk.core.project.data.generator.website

import dev.azora.sdk.core.project.domain.website.WebFeatureType

/**
 * Emits `SiteConfig.kt`: compile-time flags for each [WebFeatureType], reflecting which features the
 * model has enabled. Generated code (and the developer) can branch on these. Emits nothing when no
 * features are configured.
 */
class FeatureEmitter : KobwebEmitter {

    override fun emit(ctx: KobwebGenContext): List<GeneratedSource> {
        val features = ctx.model.features
        if (features.isEmpty()) return emptyList()

        val code = buildSource {
            packageLine(ctx.pkg)
            write("/** Generated feature flags, derived from the Azora website model. */")
            write("object SiteConfig {")
            gen {
                features.forEach { feature ->
                    write("const val ${flagName(feature.type)} = ${feature.enabled}")
                }
            }
            write("}")
        }

        return listOf(GeneratedSource("SiteConfig.kt", code))
    }

    /** `DARK_MODE` -> `darkModeEnabled`. */
    private fun flagName(type: WebFeatureType): String {
        val camel = type.name.lowercase().split('_')
            .mapIndexed { i, part ->
                if (i == 0) part else part.replaceFirstChar { it.uppercaseChar() }
            }
            .joinToString("")
        return "${camel}Enabled"
    }
}
