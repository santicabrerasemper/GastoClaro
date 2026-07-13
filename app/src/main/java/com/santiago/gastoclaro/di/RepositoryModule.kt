package com.santiago.gastoclaro.di

import com.santiago.gastoclaro.data.repository.FinanceRepositoryImpl
import com.santiago.gastoclaro.data.repository.ProfileRepositoryImpl
import com.santiago.gastoclaro.domain.repository.FinanceRepository
import com.santiago.gastoclaro.domain.repository.ProfileRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindProfileRepository(implementation: ProfileRepositoryImpl): ProfileRepository

    @Binds
    @Singleton
    abstract fun bindFinanceRepository(implementation: FinanceRepositoryImpl): FinanceRepository
}
