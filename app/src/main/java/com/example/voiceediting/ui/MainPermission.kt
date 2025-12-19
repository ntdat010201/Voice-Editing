package com.example.voiceediting.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.voiceediting.ui.permission.AudioPermissionHelper
import com.example.voiceediting.ui.permission.RecordPermissionHelper
import com.example.voiceediting.R
import com.example.voiceediting.databinding.ActivityMainPermissionBinding

class MainPermission : AppCompatActivity() {

    private lateinit var binding: ActivityMainPermissionBinding
    private lateinit var audioPermissionHelper: AudioPermissionHelper
    private lateinit var recordPermissionHelper: RecordPermissionHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainPermissionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initData()
        initView()
        initListener()
    }

    private fun initData() {
        audioPermissionHelper = AudioPermissionHelper(this, onPermissionGranted = {
            loadMp3AudioFiles()
        })
        recordPermissionHelper = RecordPermissionHelper(this, onPermissionGranted = {
            startRecord()
        })

        audioPermissionHelper.registerLauncher(requestPermissionLauncherAudio)
        recordPermissionHelper.registerLauncher(requestPermissionLauncherRecord)

    }

    private fun initView() {

    }

    private fun initListener() {
        binding.permission.setOnClickListener {
            audioPermissionHelper.checkAndRequest()
        }

        binding.record.setOnClickListener {
            recordPermissionHelper.checkAndRequest()
        }
    }

    private val requestPermissionLauncherAudio = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        audioPermissionHelper.handlePermissionResult(isGranted)
    }

    private val requestPermissionLauncherRecord = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        recordPermissionHelper.handlePermissionResult(isGranted)
    }

    private fun loadMp3AudioFiles() {
        val intent = Intent(this@MainPermission, ShowActivity::class.java)
        startActivity(intent)
    }

    private fun startRecord() {
/*        val intent = Intent(this@MainPermission, RecordActivity::class.java)
        startActivity(intent)*/
    }

}
