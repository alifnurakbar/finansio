package com.finansio.app.data.model

import com.google.gson.annotations.SerializedName

data class ReceiptParseResult(
    @SerializedName("merchant_name") val merchantName: String?,
    @SerializedName("date") val date: String?,
    @SerializedName("items") val items: List<ReceiptItem> = emptyList(),
    @SerializedName("total_amount") val totalAmount: Long?
)

data class ReceiptItem(
    @SerializedName("name") val name: String,
    @SerializedName("price") val price: Long,
    @SerializedName("quantity") val quantity: Int = 1,
    @SerializedName("suggested_category") val suggestedCategory: String? = null,
    @SerializedName("suggested_type") val suggestedType: String? = null
)
