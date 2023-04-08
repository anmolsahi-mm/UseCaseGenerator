package com.example.usecasegenerator.repository

class HomeRepositoryImpl: HomeRepository {
    override suspend fun getHomePageData(nameOfUser: String): List<String> {
        return listOf("abc", "xyz")
    }
}