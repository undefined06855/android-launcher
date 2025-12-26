package com.geode.launcher.utils

import android.util.Log
import androidx.annotation.Keep
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import java.lang.ref.WeakReference


@Keep
@Suppress("unused", "KotlinJniMissingFunction")
object NearbyConnectionsUtils {
    private lateinit var activity: WeakReference<AppCompatActivity>

    fun setContext(activity: AppCompatActivity) {
        this.activity = WeakReference(activity)
    }

    private var nearbyConnectionsEnabled: Boolean = false
    private var nearbyConnectionsName: String? = null

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpoint: String, info: DiscoveredEndpointInfo) {
            if (nearbyConnectionsEnabled) endpointFoundCallback(endpoint, info.endpointName)
        }

        override fun onEndpointLost(endpoint: String) {
            if (nearbyConnectionsEnabled) endpointLostCallback(endpoint)
        }
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpoint: String, info: ConnectionInfo) {
            if (nearbyConnectionsEnabled) connectionInitiatedCallback(endpoint, info.authenticationDigits)
        }

        override fun onConnectionResult(endpoint: String, result: ConnectionResolution) {
            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    if (nearbyConnectionsEnabled) connectionSuccessCallback(endpoint)
                }

                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                    if (nearbyConnectionsEnabled) connectionRejectedCallback(endpoint)
                }

                ConnectionsStatusCodes.STATUS_ERROR -> {
                    if (nearbyConnectionsEnabled) connectionBrokenCallback(endpoint)
                }

                else -> {
                    if (nearbyConnectionsEnabled) connectionBrokenCallback(endpoint)
                }
            }
        }

        override fun onDisconnected(endpoint: String) {
            if (nearbyConnectionsEnabled) connectionClosedCallback(endpoint)
        }
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpoint: String, payload: Payload) {
            if (payload.type != Payload.Type.BYTES) return

            val receivedBytes = payload.asBytes()!! // null if it's not bytes but we know it is

            if (nearbyConnectionsEnabled) dataReceivedCallback(endpoint, receivedBytes)
        }

        override fun onPayloadTransferUpdate(endpoint: String, update: PayloadTransferUpdate) {
            if (nearbyConnectionsEnabled) dataSendUpdateCallback(endpoint, update.bytesTransferred, update.totalBytes, update.status)
        }
    }

    fun integerToStrategy(integer: Int): Strategy? {
        return when(integer) {
            1 -> Strategy.P2P_CLUSTER
            2 -> Strategy.P2P_POINT_TO_POINT
            3 -> Strategy.P2P_STAR
            else -> null
        }
    }


    private fun requiredPermissions(): Array<String> {
        val perms = mutableListOf<String>()

        perms += android.Manifest.permission.CHANGE_WIFI_STATE
        perms += android.Manifest.permission.ACCESS_FINE_LOCATION
        perms += android.Manifest.permission.ACCESS_COARSE_LOCATION
        perms += android.Manifest.permission.BLUETOOTH_ADMIN
        perms += android.Manifest.permission.BLUETOOTH

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            perms += android.Manifest.permission.NEARBY_WIFI_DEVICES
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            perms += android.Manifest.permission.BLUETOOTH_SCAN
            perms += android.Manifest.permission.BLUETOOTH_CONNECT
            perms += android.Manifest.permission.BLUETOOTH_ADVERTISE
        }

        return perms.toTypedArray()
    }

    @JvmStatic
    fun hasPermissions(): Boolean {
        val context = activity.get() ?: return false

        for (permission in requiredPermissions()) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(context, permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Log.w("Geode", "permission $permission not granted!")
            }
        }

        Log.i("Geode", "our sdk version is ${android.os.Build.VERSION.SDK_INT} just saying")

        return requiredPermissions().all { permission ->
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                permission
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }

    @JvmStatic
    fun requestPermissions() {
        val activity = activity.get() ?: return

        val missingPermissions = requiredPermissions().filter { permission ->
            androidx.core.content.ContextCompat.checkSelfPermission(
                activity,
                permission
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isEmpty()) return

        androidx.core.app.ActivityCompat.requestPermissions(
            activity,
            missingPermissions.toTypedArray(),
            1001 // permission request code
        )
    }

    @JvmStatic
    fun enableNearbyConnectionsCallbacks() {
        nearbyConnectionsEnabled = true
    }

    @JvmStatic
    fun setDiscoveryName(name: String) {
        nearbyConnectionsName = name
    }

    @JvmStatic
    fun beginDiscovery(strategy: Int) {
        if (!nearbyConnectionsEnabled) return

        val strategy = integerToStrategy(strategy) ?: return

        val discoveryOptions = DiscoveryOptions.Builder()
            .setStrategy(strategy)
            .build()

        val context = activity.get() ?: return

        Nearby.getConnectionsClient(context)
            .startDiscovery(
                context.packageName,
                endpointDiscoveryCallback,
                discoveryOptions
            )
            .addOnSuccessListener {
                if (nearbyConnectionsEnabled) discoveryStartCallback()
            }
            .addOnFailureListener { error ->
                if (nearbyConnectionsEnabled) discoveryFailureCallback(error.message ?: "unknown error")
            }
    }

    @JvmStatic
    fun beginAdvertising(strategy: Int) {
        if (!nearbyConnectionsEnabled) return

        val strategy = integerToStrategy(strategy) ?: return

        val advertisingOptions = AdvertisingOptions.Builder()
            .setStrategy(strategy)
            .build()

        val context = activity.get() ?: return
        val name = nearbyConnectionsName ?: return
        Nearby.getConnectionsClient(context)
            .startAdvertising(
                name,
                context.packageName,
                connectionLifecycleCallback,
                advertisingOptions
            )
            .addOnSuccessListener {
                if (nearbyConnectionsEnabled) advertisingStartCallback()
            }
            .addOnFailureListener { error ->
                if (nearbyConnectionsEnabled) advertisingFailureCallback(error.message ?: "unknown error")
            }
    }

    @JvmStatic
    fun endDiscovery() {
        val context = activity.get() ?: return
        Nearby.getConnectionsClient(context)
            .stopDiscovery()
    }

    @JvmStatic
    fun endAdvertising() {
        val context = activity.get() ?: return
        Nearby.getConnectionsClient(context)
            .stopAdvertising()
    }

    @JvmStatic
    fun requestConnection(endpoint: String) {
        if (!nearbyConnectionsEnabled) return

        val context = activity.get() ?: return
        val name = nearbyConnectionsName ?: return

        Nearby.getConnectionsClient(context)
            .requestConnection(
                name,
                endpoint,
                connectionLifecycleCallback
            )
            .addOnSuccessListener {
                if (nearbyConnectionsEnabled) connectionRequestSuccessCallback(endpoint)
            }
            .addOnFailureListener { error ->
                if (nearbyConnectionsEnabled) connectionRequestFailureCallback(endpoint, error.message ?: "unknown error")
            }
    }

    @JvmStatic
    fun acceptConnection(endpoint: String) {
        if (!nearbyConnectionsEnabled) return

        val context = activity.get() ?: return
        Nearby.getConnectionsClient(context)
            .acceptConnection(endpoint, payloadCallback)
    }

    @JvmStatic
    fun rejectConnection(endpoint: String) {
        if (!nearbyConnectionsEnabled) return

        val context = activity.get() ?: return
        Nearby.getConnectionsClient(context)
            .rejectConnection(endpoint)
    }

    @JvmStatic
    fun sendData(endpoint: String, data: ByteArray) {
        if (!nearbyConnectionsEnabled) return

        val context = activity.get() ?: return
        val bytesPayload = Payload.fromBytes(data)
        Nearby.getConnectionsClient(context).sendPayload(endpoint, bytesPayload)
    }

    @JvmStatic
    fun disconnect(endpoint: String) {
        if (!nearbyConnectionsEnabled) return

        val context = activity.get() ?: return
        Nearby.getConnectionsClient(context)
            .disconnectFromEndpoint(endpoint)
    }

    // call hasPermissions() to check if we have permissions, and if not call requestPermissions()

    // call enableNearbyConnectionsCallbacks() after providing local functions for all of these
    // call setDiscoveryName(name: String) to set a name you will appear as

    // call beginDiscovery(strategy: Int) or beginAdvertising(strategy: Int) where strategy is probably Strategy.P2P_POINT_TO_POINT (2)

    external fun discoveryStartCallback()
    external fun discoveryFailureCallback(error: String)
    external fun advertisingStartCallback()
    external fun advertisingFailureCallback(error: String)

    // called for the discoverer
    external fun endpointFoundCallback(endpoint: String, name: String)
    external fun endpointLostCallback(endpoint: String)

    // call requestConnection(endpoint: String) for the discoverer

    external fun connectionRequestSuccessCallback(endpoint: String)
    external fun connectionRequestFailureCallback(endpoint: String, error: String)

    external fun connectionInitiatedCallback(endpoint: String, digits: String) // at this point you should prompt

    // call acceptConnection(endpoint: String) for both or rejectConnection(endpoint: String)

    external fun connectionSuccessCallback(endpoint: String)
    external fun connectionRejectedCallback(endpoint: String)
    external fun connectionBrokenCallback(endpoint: String)

    // should call endDiscovery() or endAdvertisement() here

    // call sendData(endpoint: String, data: ByteArray) for both

    external fun dataSendUpdateCallback(endpoint: String, bytesTransferred: Long, totalBytes: Long, status: Int)
    external fun dataReceivedCallback(endpoint: String, data: ByteArray)

    // call disconnect(endpoint: String) here

    external fun connectionClosedCallback(endpoint: String)
}