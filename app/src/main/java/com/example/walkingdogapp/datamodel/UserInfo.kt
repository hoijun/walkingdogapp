package com.example.walkingdogapp.datamodel

import java.io.Serializable

data class UserInfo(
    var email: String = "",
    var name: String = "",
    var gender: String = "",
    var birth: String = ""
) : Serializable