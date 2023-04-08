package com.example.usecasegenerator.repository

import com.example.annotations.UseCaseRepo

@UseCaseRepo
interface HomeRepository {
    suspend fun getHomePageData(id: String, authToken: String) : List<String>

    suspend fun saveUserInfo(userInfo: UserInfo)
}

data class UserInfo(
    val name: String = "",
    val age : Int = 25
)