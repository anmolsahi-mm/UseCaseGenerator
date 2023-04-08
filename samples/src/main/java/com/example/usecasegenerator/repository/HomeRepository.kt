package com.example.usecasegenerator.repository

import com.example.annotations.UseCaseRepo

@UseCaseRepo
interface HomeRepository {
    suspend fun getHomePageData(nameOfUser: String) : List<String>
}