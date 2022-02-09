package dev.brella.magicauth

import android.Manifest
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat

class AppPermissions(
    val requestCaller: ActivityResultCaller,
) : ActivityResultCaller by requestCaller {
    var permissions: Map<String, Boolean> = emptyMap()
        private set

    private val callbacks: MutableMap<String, PermissionRequest> = HashMap()

    private var resultLauncher: ActivityResultLauncher<Array<String>>? = null

    fun requestPermissions(options: ActivityOptionsCompat? = null, vararg permissions: String) =
        register().launch(permissions as Array<String>, options)

    fun requestPermissions(perms: Collection<String>, options: ActivityOptionsCompat? = null) =
        register().launch(perms.toTypedArray(), options)

    fun requestPermissions(
        options: ActivityOptionsCompat? = null,
        builder: MutableList<PermissionRequest>.() -> Unit
    ) {
        val perms = buildPermissionRequests(builder)

        if (perms.keys.all { permissions[it] == true }) {
            perms.forEach { (_, v) -> v.onGranted.invoke(this)  }
        } else {
            register()

            this.callbacks.putAll(perms)
            launch(perms.keys.toTypedArray(), options)
        }
    }

    public fun register(): AppPermissions {
        if (this.resultLauncher == null) {
            this.resultLauncher = registerForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions(),
                this::permissionsGranted
            )
        }

        return this
    }

    public fun launch(permissions: Array<String>, options: ActivityOptionsCompat? = null) =
        this.resultLauncher?.launch(permissions, options)

    fun permissionsGranted(permissions: Map<String, Boolean>) {
        this.permissions = permissions

        permissions.forEach { (perm, granted) ->
            if (granted) callbacks.remove(perm)?.onGranted?.invoke(this)
            else callbacks.remove(perm)?.onDenied?.invoke(this)
        }
    }
}

data class PermissionRequest(
    val permission: String,
    val onGranted: AppPermissions.() -> Unit,
    val onDenied: AppPermissions.() -> Unit
)

inline fun buildPermissionRequests(init: MutableList<PermissionRequest>.() -> Unit): Map<String, PermissionRequest> {
    val builder = ArrayList<PermissionRequest>()
    builder.init()
    return builder.associateBy(PermissionRequest::permission)
}

inline fun MutableList<PermissionRequest>.add(
    permission: String,
    noinline onGranted: AppPermissions.() -> Unit,
    noinline onDenied: AppPermissions.() -> Unit
) =
    add(PermissionRequest(permission, onGranted, onDenied))

inline fun MutableList<PermissionRequest>.camera(
    noinline onGranted: AppPermissions.() -> Unit = {},
    noinline onDenied: AppPermissions.() -> Unit = {}
) =
    add(Manifest.permission.CAMERA, onGranted, onDenied)

inline fun MutableList<PermissionRequest>.internet(
    noinline onGranted: AppPermissions.() -> Unit = {},
    noinline onDenied: AppPermissions.() -> Unit = {}
) =
    add(Manifest.permission.INTERNET, onGranted, onDenied)