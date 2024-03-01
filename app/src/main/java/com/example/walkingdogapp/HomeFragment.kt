package com.example.walkingdogapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.walkingdogapp.databinding.FragmentHomeBinding
import kotlinx.coroutines.launch
import okhttp3.internal.wait
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.reflect.jvm.internal.impl.builtins.SuspendFunctionTypesKt

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val myViewModel: userInfoViewModel by activityViewModels()


    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.POST_NOTIFICATIONS), 999)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater,container, false)

        setUserInfo()
        MainActivity.preFragment = "Home"

        myViewModel.currentCoord.observe(viewLifecycleOwner) {
            myViewModel.getCurrentAddress(myViewModel.currentCoord.value!!) {
                binding.textLocation.text = it
            }
        }

        binding.apply {
            btnRegister.setOnClickListener {
                val settingIntent = Intent(requireContext(), SettingDogActivity::class.java)
                startActivity(settingIntent)
            }

            btnWalk.setOnClickListener {
                if (ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    val builder = AlertDialog.Builder(requireContext())
                    builder.setTitle("산책을 위해 위치 권한을 \n항상 허용으로 해주세요!")
                    val listener = DialogInterface.OnClickListener { _, ans ->
                        when (ans) {
                            DialogInterface.BUTTON_POSITIVE -> {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                intent.data =
                                    Uri.fromParts("package", requireContext().packageName, null)
                                startActivity(intent)
                            }
                        }
                    }
                    builder.setPositiveButton("네", listener)
                    builder.setNegativeButton("아니오", null)
                    builder.show()
                    return@setOnClickListener
                }
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                    if (ContextCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        )
                        == PackageManager.PERMISSION_GRANTED
                    ) {
                        val intent = Intent(requireContext(), WalkingActivity::class.java)
                        startActivity(intent)
                    } else {
                        val builder = AlertDialog.Builder(requireContext())
                        builder.setTitle("산책을 하기 위해 위치 권한을 \n항상 허용으로 해주세요!")
                        val listener = DialogInterface.OnClickListener { _, ans ->
                            when (ans) {
                                DialogInterface.BUTTON_POSITIVE -> {
                                    ActivityCompat.requestPermissions(
                                        requireActivity(),
                                        arrayOf(
                                            Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                                        ), 998
                                    )
                                }
                            }
                        }
                        builder.setPositiveButton("네", listener)
                        builder.setNegativeButton("아니오", null)
                        builder.show()
                    }
                } else {
                    val intent = Intent(requireContext(), WalkingActivity::class.java)
                    startActivity(intent)
                }
            }
        }
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setUserInfo() {
        val userdoginfo = myViewModel.doginfo.value
        binding.apply {
            if (userdoginfo != null) {
                if (userdoginfo.name != "") {
                    msgRegister.visibility = View.INVISIBLE
                    btnRegister.visibility = View.INVISIBLE
                    dogName.text = userdoginfo.name
                    dogName.visibility = View.VISIBLE

                    if (myViewModel.imgdrawble.value != null) {
                        imgDog.setImageDrawable(myViewModel.imgdrawble.value)
                    }
                }
            }
        }
    }
}