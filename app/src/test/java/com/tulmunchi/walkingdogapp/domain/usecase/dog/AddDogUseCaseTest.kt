package com.tulmunchi.walkingdogapp.domain.usecase.dog

import com.tulmunchi.walkingdogapp.domain.model.Dog
import com.tulmunchi.walkingdogapp.domain.model.DomainError
import com.tulmunchi.walkingdogapp.domain.model.WalkRecord
import com.tulmunchi.walkingdogapp.domain.repository.DogRepository
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AddDogUseCaseTest {

    @Test
    fun `강아지 이름이 비어있으면 ValidationError를 반환하고 repository를 호출하지 않는다`() = runBlocking {
        val repository = FakeDogRepository()
        val useCase = AddDogUseCase(repository)

        val result = useCase(Dog(name = ""), imageUriString = null)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is DomainError.ValidationError)
        assertFalse(repository.addCalled)
    }

    @Test
    fun `강아지 이름이 유효하면 repository addDog를 호출한다`() = runBlocking {
        val repository = FakeDogRepository()
        val useCase = AddDogUseCase(repository)
        val dog = Dog(name = "Mongi")

        val result = useCase(dog, imageUriString = "content://dog.jpg")

        assertTrue(result.isSuccess)
        assertTrue(repository.addCalled)
        assertEquals(dog, repository.lastDog)
        assertEquals("content://dog.jpg", repository.lastImageUri)
    }

    private class FakeDogRepository : DogRepository {
        var addCalled = false
        var lastDog: Dog? = null
        var lastImageUri: String? = null

        override suspend fun getAllDogs(): Result<List<Dog>> = Result.success(emptyList())

        override suspend fun getDog(name: String): Result<Dog> = Result.success(Dog(name = name))

        override suspend fun addDog(dog: Dog, imageUriString: String?): Result<Unit> {
            addCalled = true
            lastDog = dog
            lastImageUri = imageUriString
            return Result.success(Unit)
        }

        override suspend fun updateDog(
            oldName: String,
            dog: Dog,
            imageUriString: String?,
            walkRecords: List<WalkRecord>,
            existingDogNames: List<String>
        ): Result<Unit> = Result.success(Unit)

        override suspend fun deleteDog(name: String): Result<Unit> = Result.success(Unit)

        override suspend fun getDogImage(dogName: String): Result<String> = Result.success("")

        override suspend fun getAllDogImages(): Result<Map<String, String>> = Result.success(emptyMap())

        override suspend fun getAllDogsWithImages(): Result<Map<Dog, String>> = Result.success(emptyMap())
    }
}
