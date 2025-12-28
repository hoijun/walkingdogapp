package com.tulmunchi.walkingdogapp.core.datastore

import kotlinx.coroutines.flow.Flow

/**
 * 사용자 인증 정보를 저장하는 DataStore 인터페이스
 */
interface UserPreferencesDataStore {
    /**
     * 사용자 이메일을 가져옵니다
     * @return 이메일 Flow
     */
    fun getEmail(): Flow<String?>

    /**
     * 사용자 패스워드를 가져옵니다
     * @return 패스워드 Flow
     */
    fun getPassword(): Flow<String?>

    /**
     * 사용자 이메일을 저장합니다
     * @param email 저장할 이메일
     */
    suspend fun saveEmail(email: String)

    /**
     * 사용자 패스워드를 저장합니다
     * @param password 저장할 패스워드
     */
    suspend fun savePassword(password: String)

    /**
     * 이메일과 패스워드를 동시에 저장합니다
     * @param email 저장할 이메일
     * @param password 저장할 패스워드
     */
    suspend fun saveCredentials(email: String, password: String)

    /**
     * 모든 사용자 정보를 삭제합니다
     */
    suspend fun clearAll()
}
