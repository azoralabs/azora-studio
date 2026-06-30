package dev.azora.sdk.core.presentation.permissions

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Android implementation of [PermissionController].
 *
 * Backs [requestPermission] with the Activity Result `RequestPermission` contract, registered
 * on-demand against the bound [ComponentActivity]. A [ComponentActivity] must be supplied via
 * [attach] (done by [rememberPermissionController]); without one, requests resolve to
 * [PermissionState.DENIED].
 */
actual class PermissionController {

    private var activity: ComponentActivity? = null

    /** Binds the [ComponentActivity] used to host permission requests. */
    fun attach(activity: ComponentActivity?) {
        this.activity = activity
    }

    actual suspend fun requestPermission(permission: Permission): PermissionState {
        val act = activity ?: return PermissionState.DENIED
        val manifestPermission = permission.toManifestPermission()
            ?: return PermissionState.GRANTED // not required on this API level

        if (ContextCompat.checkSelfPermission(act, manifestPermission) == PackageManager.PERMISSION_GRANTED) {
            return PermissionState.GRANTED
        }

        return suspendCancellableCoroutine { continuation ->
            val key = "azora_perm_${manifestPermission}_${System.nanoTime()}"
            var launcher: ActivityResultLauncher<String>? = null
            launcher = act.activityResultRegistry.register(
                key,
                ActivityResultContracts.RequestPermission()
            ) { granted ->
                launcher?.unregister()
                val state = when {
                    granted -> PermissionState.GRANTED
                    ActivityCompat.shouldShowRequestPermissionRationale(act, manifestPermission) ->
                        PermissionState.DENIED
                    else -> PermissionState.PERMANENTLY_DENIED
                }
                if (continuation.isActive) continuation.resume(state)
            }
            continuation.invokeOnCancellation { launcher.unregister() }
            launcher.launch(manifestPermission)
        }
    }

    private fun Permission.toManifestPermission(): String? = when (this) {
        Permission.LOCATION -> Manifest.permission.ACCESS_FINE_LOCATION
        Permission.NOTIFICATIONS ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.POST_NOTIFICATIONS
            } else {
                null // notifications need no runtime permission before Android 13
            }
    }
}
