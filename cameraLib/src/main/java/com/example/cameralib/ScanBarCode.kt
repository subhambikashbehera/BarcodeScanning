package com.example.cameralib

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.io.IOException

object ScanBarCode {

    fun scanBarCodeFromImage(uri: Uri,context: Context,callBack:(String)->Unit){
        val barcodeScanner = BarcodeScanning.getClient()
        val image: InputImage
        try {
            image = InputImage.fromFilePath(context, uri)
            barcodeScanner.process(image)
                .addOnSuccessListener { barcodeList ->
                    if (!barcodeList.isNullOrEmpty()) {
                        barcodeList[0].rawValue?.let { callBack(it) }
                    }
                }.addOnFailureListener {
                    it.printStackTrace()
                }.addOnCompleteListener {
                }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }


}