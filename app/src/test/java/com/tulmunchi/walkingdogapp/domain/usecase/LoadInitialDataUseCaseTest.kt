package com.tulmunchi.walkingdogapp.domain.usecase

import com.tulmunchi.walkingdogapp.domain.model.Dog
import com.tulmunchi.walkingdogapp.domain.model.DomainError
import com.tulmunchi.walkingdogapp.domain.model.User
import com.tulmunchi.walkingdogapp.domain.model.WalkRecord
import com.tulmunchi.walkingdogapp.domain.model.WalkStats
import com.tulmunchi.walkingdogapp.domain.repository.CollectionRepository
import com.tulmunchi.walkingdogapp.domain.repository.DogRepository
import com.tulmunchi.walkingdogapp.domain.repository.UserRepository
import com.tulmunchi.walkingdogapp.domain.repository.WalkRepository
import com.tulmunchi.walkingdogapp.domain.usecase.collection.GetAllCollectionsUseCase
import com.tulmunchi.walkingdogapp.domain.usecase.dog.GetAllDogsUseCase
import com.tulmunchi.walkingdogapp.domain.usecase.dog.GetDogImagesUseCase
import com.tulmunchi.walkingdogapp.domain.usecase.user.GetUserInfoUseCase
import com.tulmunchi.walkingdogapp.domain.usecase.walk.GetAllWalkHistoryUseCase
import com.tulmunchi.walkingdogapp.domain.usecase.walk.GetTotalWalkStatsUseCase
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LoadInitialDataUseCaseTest {

    @Test
    fun `loadImages가 true면 이미지 포함 초기 데이터를 반환한다`() = runBlocking {
        val user = User(email = "test@example.com", name = "tester", gender = "남", birth = "2000/1/1")
        val dogs = listOf(Dog(name = "Mongi"))
        val dogImages = mapOf("Mongi" to "https://image")
        val totalStats = WalkStats(distance = 1000f, time = 3600)
        val walkHistory = mapOf("Mongi" to emptyList<WalkRecord>())
        val collections = mapOf("001" to true)

        val userRepository = FakeUserRepository(user = user)
        val dogRepository = FakeDogRepository(dogs = dogs, dogImages = dogImages)
        val walkRepository = FakeWalkRepository(totalStats = totalStats, walkHistory = walkHistory)
        val collectionRepository = FakeCollectionRepository(collections = collections)

        val useCase = createUseCase(
            userRepository = userRepository,
            dogRepository = dogRepository,
            walkRepository = walkRepository,
            collectionRepository = collectionRepository
        )

        val result = useCase(loadImages = true)

        assertTrue(result.isSuccess)
        val initialData = result.getOrThrow()
        assertEquals(user, initialData.user)
        assertEquals(dogs, initialData.dogs)
        assertEquals(dogImages, initialData.dogImages)
        assertEquals(totalStats, initialData.totalWalkStats)
        assertEquals(walkHistory, initialData.walkHistory)
        assertEquals(collections, initialData.collections)
        assertTrue(dogRepository.getAllDogImagesCalled)
    }

    @Test
    fun `loadImages가 false면 이미지를 로드하지 않는다`() = runBlocking {
        val userRepository = FakeUserRepository(user = User("test@example.com", "tester", "남", "2000/1/1"))
        val dogRepository = FakeDogRepository(
            dogs = listOf(Dog(name = "Mongi")),
            dogImages = mapOf("Mongi" to "https://image")
        )
        val walkRepository = FakeWalkRepository(totalStats = WalkStats(), walkHistory = emptyMap())
        val collectionRepository = FakeCollectionRepository(collections = emptyMap())
        val useCase = createUseCase(userRepository, dogRepository, walkRepository, collectionRepository)

        val result = useCase(loadImages = false)

        assertTrue(result.isSuccess)
        assertEquals(emptyMap(), result.getOrThrow().dogImages)
        assertFalse(dogRepository.getAllDogImagesCalled)
    }

    @Test
    fun `하위 usecase에서 예외가 발생하면 UnknownError를 반환한다`() = runBlocking {
        val userRepository = FakeUserRepository(
            user = User("test@example.com", "tester", "남", "2000/1/1"),
            throwOnGetUser = true
        )
        val dogRepository = FakeDogRepository(dogs = emptyList(), dogImages = emptyMap())
        val walkRepository = FakeWalkRepository(totalStats = WalkStats(), walkHistory = emptyMap())
        val collectionRepository = FakeCollectionRepository(collections = emptyMap())
        val useCase = createUseCase(userRepository, dogRepository, walkRepository, collectionRepository)

        val result = useCase(loadImages = true)

        assertTrue(result.isFailure)
        val error = result.exceptionOrNull()
        assertTrue(error is DomainError.UnknownError)
        assertTrue(error.message?.contains("boom") == true)
    }

    private fun createUseCase(
        userRepository: UserRepository,
        dogRepository: DogRepository,
        walkRepository: WalkRepository,
        collectionRepository: CollectionRepository
    ): LoadInitialDataUseCase {
        return LoadInitialDataUseCase(
            getUserInfoUseCase = GetUserInfoUseCase(userRepository),
            getAllDogsUseCase = GetAllDogsUseCase(dogRepository),
            getDogImagesUseCase = GetDogImagesUseCase(dogRepository),
            getTotalWalkStatsUseCase = GetTotalWalkStatsUseCase(walkRepository),
            getAllWalkHistoryUseCase = GetAllWalkHistoryUseCase(walkRepository),
            getAllCollectionsUseCase = GetAllCollectionsUseCase(collectionRepository)
        )
    }

    private class FakeUserRepository(
        private val user: User,
        private val throwOnGetUser: Boolean = false
    ) : UserRepository {
        override suspend fun getUser(): Result<User> {
            if (throwOnGetUser) {
                throw IllegalStateException("boom")
            }
            return Result.success(user)
        }

        override suspend fun updateUser(user: User): Result<Unit> = Result.success(Unit)

        override suspend fun signUp(email: String): Result<Unit> = Result.success(Unit)

        override suspend fun deleteAccount(): Result<Unit> = Result.success(Unit)
    }

    private class FakeDogRepository(
        private val dogs: List<Dog>,
        private val dogImages: Map<String, String>
    ) : DogRepository {
        var getAllDogImagesCalled = false

        override suspend fun getAllDogs(): Result<List<Dog>> = Result.success(dogs)

        override suspend fun getDog(name: String): Result<Dog> = Result.success(Dog(name = name))

        override suspend fun addDog(dog: Dog, imageUriString: String?): Result<Unit> = Result.success(Unit)

        override suspend fun updateDog(
            oldName: String,
            dog: Dog,
            imageUriString: String?,
            walkRecords: List<WalkRecord>,
            existingDogNames: List<String>
        ): Result<Unit> = Result.success(Unit)

        override suspend fun deleteDog(name: String): Result<Unit> = Result.success(Unit)

        override suspend fun getDogImage(dogName: String): Result<String> = Result.success("")

        override suspend fun getAllDogImages(): Result<Map<String, String>> {
            getAllDogImagesCalled = true
            return Result.success(dogImages)
        }

        override suspend fun getAllDogsWithImages(): Result<Map<Dog, String>> = Result.success(emptyMap())
    }

    private class FakeWalkRepository(
        private val totalStats: WalkStats,
        private val walkHistory: Map<String, List<WalkRecord>>
    ) : WalkRepository {
        override suspend fun saveWalkRecord(dogNames: List<String>, walkRecord: WalkRecord): Result<Unit> {
            return Result.success(Unit)
        }

        override suspend fun getAllWalkHistory(): Result<Map<String, List<WalkRecord>>> = Result.success(walkHistory)

        override suspend fun getTotalWalkStats(): Result<WalkStats> = Result.success(totalStats)
    }

    private class FakeCollectionRepository(
        private val collections: Map<String, Boolean>
    ) : CollectionRepository {
        override suspend fun getAllCollections(): Result<Map<String, Boolean>> = Result.success(collections)

        override suspend fun isCollectionOwned(collectionNum: String): Result<Boolean> {
            return Result.success(collections[collectionNum] ?: false)
        }
    }
}
