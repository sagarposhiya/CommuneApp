package com.devlomi.commune.adapters.messaging.holders

import android.content.Context
import android.view.View
import android.widget.TextView
import com.devlomi.commune.R
import com.devlomi.commune.adapters.messaging.holders.base.BaseReceivedHolder
import com.devlomi.commune.model.realms.Message
import com.devlomi.commune.model.realms.User
import com.devlomi.commune.utils.Util
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class ReceivedLocationHolder(context: Context, itemView: View) : BaseReceivedHolder(context, itemView)
        , OnMapReadyCallback {

    private var mGoogleMap: GoogleMap? = null
    private var mMapLocation: LatLng? = null
    private val placeName: TextView
    private val placeAddress: TextView
    private val mapView: MapView

    override fun onMapReady(googleMap: GoogleMap) {
        mGoogleMap = googleMap
        MapsInitializer.initialize(context)
        googleMap.uiSettings.isMapToolbarEnabled = false

        // If we have mapView data, update the mapView content.
        if (mMapLocation != null) {
            updateMapContents()
        }


    }

    fun setMapLocation(location: LatLng?) {
        mMapLocation = location

        // If the mapView is ready, update its content.
        if (mGoogleMap != null) {
            updateMapContents()
        }
    }

    override fun bind(message: Message, user: User) {
        super.bind(message, user)
        val latlng = message.location.latlng
        placeAddress.text = message.location.address
        if (!Util.isNumeric(message.location.name)) {
            placeName.text = message.location.name
            placeName.visibility = View.VISIBLE
        } else placeName.visibility = View.GONE

        setMapLocation(latlng)




    }

    protected fun updateMapContents() {
        // Since the mapView is re-used, need to remove pre-existing mapView features.
        mGoogleMap?.clear()

        // Update the mapView feature data and camera position.
        mMapLocation?.let { mapLocation ->


            mGoogleMap?.addMarker(MarkerOptions().position(mapLocation))
            val cameraUpdate = CameraUpdateFactory.newLatLngZoom(mapLocation, 17f)
            mGoogleMap?.moveCamera(cameraUpdate)
        }
    }

    init {
        mapView = itemView.findViewById(R.id.map_view)
        placeName = itemView.findViewById(R.id.place_name)
        placeAddress = itemView.findViewById(R.id.place_address)
        mapView.onCreate(null)
        mapView.getMapAsync(this)
    }


}