package com.videonatics.smartcapture.feature.capture.data.datasource.local

import android.annotation.SuppressLint
import android.content.Context
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
    fun getLocationFlow(): Flow<Pair<Double, Double>>
}

class CaptureLocalDatasourceImpl @Inject constructor(context: Context) : CaptureLocalDatasource{
    private val client = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    override fun getLocationFlow(): Flow<Pair<Double, Double>> = callbackFlow {

        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            2000L
        ).setMinUpdateIntervalMillis(1000L)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let {
                    trySend(it.latitude to it.longitude)
                }
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