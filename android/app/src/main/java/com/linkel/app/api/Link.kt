package com.linkel.app.api

import com.google.gson.annotations.SerializedName

data class Link(
    val id: Int,
    val username: String,
    val title: String,
    val url: String,
    val description: String?,
    @SerializedName("created_at") val createdAt: String
)

data class NewLink(
    val username: String,
    val title: String,
    val url: String,
    val description: String?
)

data class DeleteRequest(
    val username: String
)
