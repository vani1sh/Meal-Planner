package com.example.mealplanner.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query


data class SearchResponse(
    @SerializedName("products") val products: List<ProductDto>
)

data class ProductDto(
    @SerializedName("_id") val id: String,
    @SerializedName("product_name") val productName: String?,
    @SerializedName("brands") val brands: String?,
    @SerializedName("nutriments") val nutriments: NutrimentsDto?
)

data class NutrimentsDto(
    @SerializedName("energy-kcal_100g") val calories: Float?,
    @SerializedName("proteins_100g") val proteins: Float?,
    @SerializedName("fat_100g") val fat: Float?,
    @SerializedName("carbohydrates_100g") val carbs: Float?
)


interface OpenFoodFactsApi {
    @GET("cgi/search.pl?search_simple=1&action=process&json=1")
    suspend fun searchProducts(
        @Query("search_terms") query: String,
        @Query("page_size") pageSize: Int = 20
    ): SearchResponse
}