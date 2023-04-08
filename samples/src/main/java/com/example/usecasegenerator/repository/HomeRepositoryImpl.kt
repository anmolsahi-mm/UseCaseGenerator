package com.example.usecasegenerator.repository

class HomeRepositoryImpl: HomeRepository {
    override suspend fun getHomePageData(id: String): List<String> {
        return listOf("abc", "xyz")
    }

    override suspend fun saveUserInfo(userInfo: UserInfo) {
        // save the user info here
    }
}