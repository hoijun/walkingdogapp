package com.tulmunchi.walkingdogapp.data.model

import com.google.gson.annotations.SerializedName

data class WeatherResponseDto(
    @SerializedName("response")
    val response: Response
) {
    data class Response(
        @SerializedName("body")
        val body: Body
    )

    data class Body(
        @SerializedName("items")
        val items: Items
    )

    data class Items(
        @SerializedName("item")
        val item: List<Item>
    )

    data class Item(
        @SerializedName("category")
        val category: String,
        @SerializedName("fcstTime")
        val fcstTime: String,
        @SerializedName("fcstValue")
        val fcstValue: String
    )
}