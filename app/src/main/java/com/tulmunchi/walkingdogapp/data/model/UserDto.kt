package com.tulmunchi.walkingdogapp.data.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

/**
 * Data Transfer Object for User (Firebase)
 */
@Keep
data class UserDto(
    @SerializedName("email")
    val email: String = "",
    @SerializedName("name")
    val name: String = "",
    @SerializedName("gender")
    val gender: String = "",
    @SerializedName("birth")
    val birth: String = ""
)
