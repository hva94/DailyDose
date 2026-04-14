package com.hvasoft.dailydose.domain.interactor.profile

interface UpdateProfileNameUseCase {
    suspend operator fun invoke(
        userId: String,
        newName: String,
        fallbackDisplayName: String,
    ): Result<Unit>
}
