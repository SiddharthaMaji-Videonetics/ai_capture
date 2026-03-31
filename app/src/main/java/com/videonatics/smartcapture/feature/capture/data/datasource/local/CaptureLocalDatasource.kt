package com.videonatics.smartcapture.feature.capture.data.datasource.local

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Build
import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

interface CaptureLocalDatasource{
    fun getLocationFlow(): Flow<Location>
}

class CaptureLocalDatasourceImpl @Inject constructor(context: Context) : CaptureLocalDatasource{
    private val client = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    override fun getLocationFlow(): Flow<Location> = callbackFlow {

        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            2000L
        ).setMinUpdateIntervalMillis(1000L)
            .build()

        var lastStableLocation: Location? = null
        var smoothedAltitude = 0.0

        fun smooth(new: Double): Double {
            smoothedAltitude = 0.85 * smoothedAltitude + 0.15 * new
            return smoothedAltitude
        }

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {

                val loc = result.lastLocation ?: return

                val hasGoodAltitude = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                    loc.hasAltitude() &&
                            loc.hasVerticalAccuracy() &&
                            loc.verticalAccuracyMeters < 20
                }else{
                    false
                }


                val finalLocation = if (hasGoodAltitude) {

                    val newAlt = smooth(loc.altitude)

                    val updated = Location(loc).apply {
                        altitude = newAlt
                    }

                    lastStableLocation = updated
                    updated

                } else {
                    lastStableLocation ?: loc
                }

                trySend(finalLocation)
            }
        }

        client.requestLocationUpdates(
            request,
            callback,
            Looper.getMainLooper()
        )

        awaitClose {
            client.removeLocationUpdates(callback)
        }
    }

}