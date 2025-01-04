package com.tulmunchi.walkingdogapp.datamodel

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import java.io.Serializable

@Keep
data class UserInfo(
    @SerializedName("email")
    var email: String = "",
    @SerializedName("name")
    var name: String = "",
    @SerializedName("gender")
    var gender: String = "",
    @SerializedName("birth")
    var birth: String = ""
) : Serializable