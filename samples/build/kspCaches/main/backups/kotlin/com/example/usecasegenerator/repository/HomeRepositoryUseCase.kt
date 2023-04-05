package com.example.usecasegenerator.repository

import kotlin.String
import kotlin.collections.List

public open class HomeRepositoryUseCase(
  public val homerepository: HomeRepository
) : HomeRepository {
  public override suspend fun getHomePageData(id: String): List<String> =
      homerepository.getHomePageData(id)
}
