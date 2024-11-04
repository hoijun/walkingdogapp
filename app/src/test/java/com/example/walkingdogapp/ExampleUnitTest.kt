package com.example.walkingdogapp

import com.naver.maps.geometry.LatLng
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    private val angleThreshold = 20

    @Test
    fun testNeedToFlatWithVariousAngles() {
        val testCases = arrayListOf(
            arrayListOf(LatLng(37.5666, 126.9784), LatLng(37.5668, 126.9786), LatLng(37.5670, 126.9788)), // 작은 각도
            arrayListOf(LatLng(37.5666, 126.9784), LatLng(37.5668, 126.9786), LatLng(37.5667, 126.9789)), // 중간 각도
            arrayListOf(LatLng(37.5666, 126.9784), LatLng(37.5668, 126.9786), LatLng(37.5665, 126.9789)), // 큰 각도
            arrayListOf(LatLng(37.5666, 126.9784), LatLng(37.5668, 126.9786), LatLng(37.5670, 126.9786)), // 직각
            arrayListOf(LatLng(37.5666, 126.9784), LatLng(37.5668, 126.9786), LatLng(37.5666, 126.9788))  // U턴
        )

        testCases.forEachIndexed { index, list ->
            val point1 = list[0]
            val point2 = list[1]
            val point3 = list[2]
            val shouldFlatten = needToFlat(point1, point2, point3)
            val angle1 = calculateAngle(point1, point2)
            val angle2 = calculateAngle(point2, point3)
            val angleDifference = abs(angle1 - angle2)

            println("Test Case ${index + 1}:")
            println("  Point 1: $point1")
            println("  Point 2: $point2")
            println("  Point 3: $point3")
            println("  Angle 1: $angle1")
            println("  Angle 2: $angle2")
            println("  Angle Difference: $angleDifference")
            println("  Should Flatten: $shouldFlatten")

            // 각도 차이가 threshold보다 작거나 같으면 flatten, 크면 유지
            assertEquals(angleDifference <= angleThreshold, shouldFlatten)
            println(list.toString())
            removeUseless(shouldFlatten, list)

            if (shouldFlatten) {
                println(list.toString())
                assertEquals(true, list[0] == point1 && list[1] == point3)
            } else {
                println(list.toString())
                assertEquals(true, list[0] == point1 && list[1] == point2 && list[2] == point3)
            }

            println()
        }
    }

    private fun removeUseless(shouldFlatten: Boolean, list: ArrayList<LatLng>) {
        if (shouldFlatten) {
            list.removeAt(list.size - 2)
        }
    }

    private fun needToFlat(point1: LatLng, point2: LatLng, point3: LatLng): Boolean {
        var needToFlat = false
        val angle1 = calculateAngle(point1, point2)
        val angle2 = calculateAngle(point2, point3)

        if (abs(angle1 - angle2) <= angleThreshold) {
            needToFlat = true
        }

        return needToFlat
    }

    private fun calculateAngle(point1: LatLng, point2: LatLng): Float {
        val dx = point2.longitude - point1.longitude
        val dy = point2.latitude - point1.latitude

        var angle = Math.toDegrees(atan2(dx, dy)).toFloat()

        // 각도를 0에서 360도 사이의 값으로 조정
        if (angle < 0) {
            angle += 360f
        }

        return angle
    }
}

