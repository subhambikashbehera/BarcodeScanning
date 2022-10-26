package com.example.cameralib

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.DisplayMetrics
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.example.cameralib.databinding.ActivityMainBinding
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage

import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {


    private val screenAspectRatio by lazy {
        val metrics = DisplayMetrics().also { binding.previewView.display.getRealMetrics(it) }
        metrics.getAspectRatio()
    }

    private lateinit var cameraProvider:ProcessCameraProvider
    private lateinit var camera : Camera
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        supportActionBar?.hide()
        cameraExecutor = Executors.newSingleThreadExecutor()



        binding.insertPhoto.setOnClickListener {
            ImagePicker.with(this)
                .crop()	    			//Crop image(Optional), Check Customization for more option
                .compress(1024)			//Final image size will be less than 1 MB(Optional)
                .maxResultSize(1080, 1080)	//Final image resolution will be less than 1080 x 1080(Optional)
                .start()
        }







    }

    override fun onResume() {
        super.onResume()
        checkPermission()
    }


    private fun checkPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        } else {
            startCameraOperation()
        }
    }

    private fun startCameraOperation() {
        startCamera()
        setupClickListeners()

    }


    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (it) {
                //start the operation
                startCameraOperation()
            } else {
                // again request the permission
                Toast.makeText(this, "Please allow the permission", Toast.LENGTH_SHORT).show()
            }
        }


    private fun startCamera() {
        val barcodeScanner = BarcodeScanning.getClient()
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
             cameraProvider = cameraProviderFuture.get()

            val previewUseCase = Preview.Builder()
                .setTargetRotation(binding.previewView.display.rotation)
                .setTargetAspectRatio(screenAspectRatio)
                .build().also {
                    it.setSurfaceProvider(binding.previewView.surfaceProvider)
                }

            val analysisUseCase = ImageAnalysis.Builder()
                .setTargetRotation(binding.previewView.display.rotation)
                .setTargetAspectRatio(screenAspectRatio)
                .build().also {
                    it.setAnalyzer(
                        cameraExecutor
                    ) { imageProxy ->
                        processImageProxy(barcodeScanner, imageProxy)
                    }
                }

            val useCaseGroup = UseCaseGroup.Builder()
                .addUseCase(previewUseCase)
                .addUseCase(analysisUseCase)
                .build()

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()
                // Bind use cases to camera
              camera=  cameraProvider.bindToLifecycle(this, cameraSelector,useCaseGroup)

            } catch (exc: Exception) {
                exc.printStackTrace()
            }

        }, ContextCompat.getMainExecutor(this))

    }




    private fun setupClickListeners() {
        binding.flash.setOnClickListener {
            when (camera.cameraInfo.torchState.value) {
                TorchState.ON -> {camera.cameraControl.enableTorch(false)
                binding.flash.setImageResource(R.drawable.ic_baseline_flash_off_24)
                }
                TorchState.OFF -> {camera.cameraControl.enableTorch(true)
                    binding.flash.setImageResource(R.drawable.ic_baseline_flash_on_24)
                }
            }
        }
    }




    @SuppressLint("UnsafeOptInUsageError")
    private fun processImageProxy(barcodeScanner: BarcodeScanner, imageProxy: ImageProxy) {
        imageProxy.image?.let { image ->
            val inputImage = InputImage.fromMediaImage(image, imageProxy.imageInfo.rotationDegrees)
            barcodeScanner.process(inputImage)
                .addOnSuccessListener { barcodeList ->
                    if (!barcodeList.isNullOrEmpty()) {
                        cameraProvider.unbindAll()
                        val  intent = Intent()
                        intent.putExtra("data",barcodeList[0].rawValue!!)
                        setResult(RESULT_OK,intent)
                        finish()
                    }
                }.addOnFailureListener {
                    it.printStackTrace()
                }.addOnCompleteListener {
                    imageProxy.close()
                }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }



    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            //Image Uri will not be null for RESULT_OK
            val uri: Uri = data?.data!!
            val barcodeScanner = BarcodeScanning.getClient()

            val image: InputImage
            try {
                image = InputImage.fromFilePath(this, uri)


                barcodeScanner.process(image)
                    .addOnSuccessListener { barcodeList ->
                        if (!barcodeList.isNullOrEmpty()) {
                            cameraProvider.unbindAll()
                            val  intent = Intent()
                            intent.putExtra("data",barcodeList[0].rawValue!!)
                            setResult(RESULT_OK,intent)
                            finish()
                        }
                    }.addOnFailureListener {
                        it.printStackTrace()
                    }.addOnCompleteListener {

                    }

            } catch (e: IOException) {
                e.printStackTrace()
            }







        } else if (resultCode == ImagePicker.RESULT_ERROR) {
            Toast.makeText(this, ImagePicker.getError(data), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Task Cancelled", Toast.LENGTH_SHORT).show()
        }
    }






}