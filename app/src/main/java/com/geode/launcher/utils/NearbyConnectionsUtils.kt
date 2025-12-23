package com.geode.launcher.utils

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
        this.activity = WeakReference(activity);
    }

    private var nearbyConnectionsEnabled: Boolean = false
    private var nearbyConnectionsName: String? = null

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            // An endpoint was found. We request a connection to it.
            val context = activity.get() ?: return
            val name = nearbyConnectionsName ?: return
            Nearby.getConnectionsClient(context)
                .requestConnection(
                    name,
                    endpointId,
                    connectionLifecycleCallback
                )
                .addOnSuccessListener {
                    // We successfully requested a connection.
                    // Now both sides must accept before the connection is established.
                }
                .addOnFailureListener { e ->
                    // Nearby Connections failed to request the connection.
                }
        }

        override fun onEndpointLost(endpointId: String) {
            // A previously discovered endpoint has gone away.
        }
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
//            AlertDialog.Builder(context)
//                .setTitle("Accept connection to ${info.endpointName}")
//                .setMessage(
//                    "Confirm the code matches on both devices: ${info.authenticationDigits}"
//                )
//                .setPositiveButton("Accept") { _, _ ->
//                    // The user confirmed, so we can accept the connection.
//                    Nearby.getConnectionsClient(context)
//                        .acceptConnection(endpointId, payloadCallback)
//                }
//                .setNegativeButton(android.R.string.cancel) { _, _ ->
//                    // The user canceled, so we should reject the connection.
//                    Nearby.getConnectionsClient(context)
//                        .rejectConnection(endpointId)
//                }
//                .setIcon(android.R.drawable.ic_dialog_alert)
//                .show()
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    // We're connected! Can now start sending and receiving data.
                }

                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                    // The connection was rejected by one or both sides.
                }

                ConnectionsStatusCodes.STATUS_ERROR -> {
                    // The connection broke before it was able to be accepted.
                }

                else -> {
                    // Unknown status code
                }
            }
        }

        override fun onDisconnected(endpointId: String) {
            // We've been disconnected from this endpoint.
            // No more data can be sent or received.
        }
    }

    private val payloadCallback = object : PayloadCallback() {
        public override fun onPayloadReceived(endpointId: String, payload: Payload) {
            // This always gets the full data of the payload. Is null if it's not a BYTES payload.
            if (payload.type == Payload.Type.BYTES) {
                val receivedBytes = payload.asBytes()
            }
        }

        public override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            // Bytes payloads are sent as a single chunk, so you'll receive a SUCCESS update immediately
            // after the call to onPayloadReceived().
        }
    }

    private fun startAdvertising(strategy: Strategy = Strategy.P2P_POINT_TO_POINT) {
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
                // We're advertising!
            }
            .addOnFailureListener { e ->
                // We were unable to start advertising.
            }
    }

    private fun startDiscovery(strategy: Strategy = Strategy.P2P_POINT_TO_POINT) {
        val discoveryOptions = DiscoveryOptions.Builder()
            .setStrategy(strategy)
            .build()

        val context = activity.get() ?: return
        val name = nearbyConnectionsName ?: return

        Nearby.getConnectionsClient(context)
            .startDiscovery(
                context.packageName,
                endpointDiscoveryCallback,
                discoveryOptions
            )
            .addOnSuccessListener {
                // We're discovering!
            }
            .addOnFailureListener { e ->
                // We're unable to start discovering.
            }
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
    fun beginDiscovery(strategy: Strategy) {
        TODO("balls")
    }

    @JvmStatic
    fun beginAdvertising(strategy: Strategy) {
        TODO("balls")
    }

    @JvmStatic
    fun connectToEndpoint(endpoint: String) {
        TODO("balls")
    }

    @JvmStatic
    fun acceptConnection(endpoint: String) {
        val context = activity.get() ?: return
        Nearby.getConnectionsClient(context)
            .acceptConnection(endpoint, payloadCallback)
    }

    @JvmStatic
    fun rejectConnection(endpoint: String) {
        val context = activity.get() ?: return
        Nearby.getConnectionsClient(context)
            .rejectConnection(endpoint)
    }
x
    @JvmStatic
    fun sendData(endpoint: String, data: ByteArray) {
        TODO("balls")
    }

    @JvmStatic
    fun close(endpoint: String) {
        TODO("balls")
    }



    // call enableDiscovery(enabled: Boolean) after providing local functions for all of these
    // call setDiscoveryName(name: String) to set a name you will appear as

    // call beginDiscovery(strategy: Strategy) or beginAdvertising(strategy: Strategy) where strategy is probably Strategy.P2P_POINT_TO_POINT
    // though actually for jni maybe it should just be an int

    external fun discoveryStartCallback();
    external fun discoveryFailureCallback(error: String);
    external fun advertisingStartCallback();
    external fun advertisingFailureCallback(error: String);

    // called for the discoverer
    external fun endpointFoundCallback(endpoint: String);
    external fun endpointLostCallback(endpoint: String);

    // call connectToEndpoint(endpoint: String) for the discoverer

    external fun connectionInitiatedCallback(endpoint: String);

    // call acceptConnection(endpoint: String) for both

    external fun connectionSuccessCallback(endpoint: String);
    external fun connectionRejectedCallback(endpoint: String);
    external fun connectionErrorCallback(endpoint: String);

    // call sendData(endpoint: String, data: ByteArray) for both

    external fun dataReceived(endpoint: String, data: ByteArray);

    external fun connectionClosedCallback(endpoint: String);
}