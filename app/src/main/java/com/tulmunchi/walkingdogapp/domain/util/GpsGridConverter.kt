package com.tulmunchi.walkingdogapp.domain.util

import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

object GpsGridConverter {
    data class LatXLngY(
        var lat: Double? = null,
        var lng: Double? = null,

        var x: Int? = null,
        var y: Int? = null
    )

    const val MODE_GRID = 1;
    const val MODE_GPS = 0;

    fun convertGRIDBetweenGPS(mode: Int, lat: Double, lng: Double): LatXLngY {

        // 지구 반경(km)
        val RE = 6371.00877
        // 격자 간격(km)
        val GRID = 5.0
        // 투영 위도1(degree)
        val SLAT1 = 30.0
        // 투영 위도2(degree)
        val SLAT2 = 60.0
        // 기준점 경도(degree)
        val OLON = 126.0
        // 기준점 위도(degree)
        val OLAT = 38.0
        // 기준점 X좌표(GRID)
        val XO = 43.0
        // 기준점 Y좌표(GRID)
        val YO = 136.0

        val DEGRAD = Math.PI / 180.0
        val RADDEG = 180.0 / Math.PI

        val re = RE / GRID
        val slat1 = SLAT1 * DEGRAD
        val slat2 = SLAT2 * DEGRAD
        val olon = OLON * DEGRAD
        val olat = OLAT * DEGRAD

        var sn = tan(Math.PI * 0.25 + slat2 * 0.5) / tan(Math.PI * 0.25 + slat1 * 0.5)
        sn = ln(cos(slat1) / cos(slat2)) / ln(sn)

        var sf = tan(Math.PI * 0.25 + slat1 * 0.5)
        sf = sf.pow(sn) * cos(slat1) / sn

        var ro = tan(Math.PI * 0.25 + olat * 0.5)
        ro = re * sf / ro.pow(sn)

        val rs = LatXLngY()

        if (mode == MODE_GRID) {
            rs.lat = lat
            rs.lng = lng
            var ra = tan(Math.PI * 0.25 + lat * DEGRAD * 0.5)
            ra = re * sf / ra.pow(sn)
            var theta = lng * DEGRAD - olon
            if (theta > Math.PI) theta -= 2.0 * Math.PI
            if (theta < -Math.PI) theta += 2.0 * Math.PI
            theta *= sn
            rs.x = floor(ra * sin(theta) + XO + 0.5).toInt()
            rs.y = floor(ro - ra * cos(theta) + YO + 0.5).toInt()
        } else {
            rs.x = lat.toInt()
            rs.y = lng.toInt()
            val xn = lat - XO
            val yn = ro - lng + YO
            var ra = sqrt(xn * xn + yn * yn)
            if (sn < 0.0) {
                ra = -ra
            }
            var alat = (re * sf / ra).pow(1.0 / sn)
            alat = 2.0 * atan(alat) - Math.PI * 0.5

            var theta = 0.0
            if (abs(xn) <= 0.0) {
                theta = 0.0
            } else {
                if (abs(yn) <= 0.0) {
                    theta = Math.PI * 0.5
                    if (xn < 0.0) {
                        theta = -theta
                    }
                } else {
                    theta = atan2(xn, yn)
                }
            }
            val alon = theta / sn + olon
            rs.lat = alat * RADDEG
            rs.lng = alon * RADDEG
        }
        return rs
    }
}