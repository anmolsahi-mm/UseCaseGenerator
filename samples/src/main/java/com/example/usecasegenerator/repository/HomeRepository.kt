package com.example.usecasegenerator.repository

import com.example.annotations.UseCaseRepo

@UseCaseRepo
interface HomeRepository {
    suspend fun getHomePageData(id: String) : List<String>
}