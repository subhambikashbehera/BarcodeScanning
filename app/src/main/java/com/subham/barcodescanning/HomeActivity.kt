package com.subham.barcodescanning

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import com.example.cameralib.MainActivity
import com.example.cameralib.databinding.ActivityMainBinding
import com.subham.barcodescanning.databinding.ActivityHomeBinding

class HomeActivity : AppCompatActivity() {
    lateinit var binding: ActivityHomeBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this,R.layout.activity_home)
        binding.btnScanner.setOnClickListener {
            val intent = Intent(this,MainActivity::class.java)
            intentLauncher.launch(intent)
        }
    }




    private val intentLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        if (it.resultCode == RESULT_OK){
            val data = it.data?.extras
            val value = data?.getString("data")
            value?.let {result->
                binding.result.text = result
            }
        }
    }
}