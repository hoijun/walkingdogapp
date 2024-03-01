package com.example.walkingdogapp

class DogInfo {
    var name = ""
    var breed = ""
    var gender = ""
    var birth = ""
    var neutering = ""
    var vaccination = ""
    var weight = 0
    var feature = ""
    var dates = listOf<Walkdate>()
}
class Walkdate(day: String, startTime: String, endTime: String, distance: Float, time: Int) {
    var day = ""
    var startTime = ""
    var endTime = ""
    var distance = 0.0f
    var time = 0
    init {
        this.day = day
        this.startTime = startTime
        this.endTime = endTime
        this.distance = distance
        this.time = time
    }
}