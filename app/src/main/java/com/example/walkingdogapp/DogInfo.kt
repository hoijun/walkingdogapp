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
class Walkdate(distance: Float, time: Int) {
    var distance = 0.0f
    var time = 0
    init {
        this.distance = distance
        this.time = time
    }
}