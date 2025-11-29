package com.tulmunchi.walkingdogapp.core.permission

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import javax.inject.Inject

/**
 * 권한 관리를 위한 인터페이스
 */
interface PermissionHandler {
    /**
     * 권한이 부여되었는지 확인 (Activity)
     * @param activity Activity 컨텍스트
     * @param permissions 확인할 권한 목록
     * @return 모든 권한이 부여되었으면 true, 아니면 false
     */
    fun checkPermissions(activity: Activity, permissions: Array<String>): Boolean

    /**
     * 권한이 부여되었는지 확인 (Context)
     * @param context Context (Service 등에서 사용)
     * @param permissions 확인할 권한 목록
     * @return 모든 권한이 부여되었으면 true, 아니면 false
     */
    fun checkPermissions(context: Context, permissions: Array<String>): Boolean

    /**
     * 권한 요청
     * @param activity Activity 컨텍스트
     * @param permissions 요청할 권한 목록
     * @param requestCode 요청 코드
     */
    fun requestPermissions(activity: Activity, permissions: Array<String>, requestCode: Int)
}

/**
 * PermissionHandler 구현체
 */
class PermissionHandlerImpl @Inject constructor() : PermissionHandler {

    override fun checkPermissions(activity: Activity, permissions: Array<String>): Boolean {
        return permissions.all { permission ->
            ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun checkPermissions(context: Context, permissions: Array<String>): Boolean {
        return permissions.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun requestPermissions(activity: Activity, permissions: Array<String>, requestCode: Int) {
        ActivityCompat.requestPermissions(activity, permissions, requestCode)
    }
}
