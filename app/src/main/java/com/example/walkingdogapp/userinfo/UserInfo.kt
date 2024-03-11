package com.example.walkingdogapp.userinfo

import java.io.Serializable

class UserInfo : Serializable {
    var email = ""
    var name = ""
    var gender = ""
    var birth = ""
}

data class totalWalkInfo(var totaldistance: Float = 0.0f, var totaltime: Int = 0)