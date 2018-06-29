package com.techjini.changeuploadphoto

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.support.annotation.RequiresApi
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Button
import android.widget.Toast
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE
import de.hdodenhof.circleimageview.CircleImageView
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private val REQUEST_CAMERA = 0
    private var userChoosenTask: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val btnSelect = findViewById<Button>(R.id.selectButton)
        btnSelect.setOnClickListener { selectImage() }
    }

    fun onSelectImageClick() {
        CropImage.startPickImageActivity(this)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            Utility.MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE -> if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (userChoosenTask == "Take Photo")
                    cameraIntent()
                else if (userChoosenTask == "Choose from Library")
                    galleryIntent()
            }
        }
    }

    private fun selectImage() {
        val items = arrayOf<CharSequence>("Take a Photo", "Choose Photo", "Cancel")
        val builder = AlertDialog.Builder(this@MainActivity)
        builder.setTitle("Choose Photo")
        builder.setItems(items) { dialog, item ->
            val result = Utility.checkPermission(this@MainActivity)

            if (items[item] == "Take a Photo") {
                userChoosenTask = "Take a Photo"
                if (result)
                    cameraIntent()
            } else if (items[item] == "Choose Photo") {
                userChoosenTask = "Choose Photo"
                if (result)
                    onSelectImageClick()
            } else if (items[item] == "Cancel") {
                dialog.dismiss()
            }
        }
        builder.show()
    }

    private fun cameraIntent() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(intent, REQUEST_CAMERA)
    }

    private fun galleryIntent() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT//
        val SELECT_FILE = 1
        startActivityForResult(Intent.createChooser(intent, "Select File"), SELECT_FILE)
    }

    private fun startCropImageActivity(imageUri: Uri) {
        CropImage.activity(imageUri)
                .start(this)
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        // handle result of pick image chooser
        if (requestCode == CropImage.PICK_IMAGE_CHOOSER_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val imageUri = CropImage.getPickImageResultUri(this, data)

            // For API >= 23 we need to check specifically that we have permissions to read external storage.
            if (CropImage.isReadExternalStoragePermissionsRequired(this, imageUri)) {
                // request permissions and handle the result in onRequestPermissionsResult()
                requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 0)
            } else {
                // no permissions required or already grunted, can start crop image activity
                startCropImageActivity(imageUri)
            }
        }

        // handle result of CropImageActivity
        if (requestCode == CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            val result = CropImage.getActivityResult(data)
            if (resultCode == Activity.RESULT_OK) {
                (findViewById<View>(R.id.profile_image) as CircleImageView).setImageURI(result.uri)
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Toast.makeText(this, "Cropping failed: " + result.error, Toast.LENGTH_LONG).show()
            }
        } else if (requestCode == REQUEST_CAMERA)
            startCropImageActivity(onCaptureImageResult(data))
    }

    private fun onCaptureImageResult(data: Intent): Uri {
        val thumbnail = data.extras!!.get("data") as Bitmap
        val bytes = ByteArrayOutputStream()
        thumbnail.compress(Bitmap.CompressFormat.JPEG, 90, bytes)

        val destination = File(Environment.getExternalStorageDirectory(),
                System.currentTimeMillis().toString() + ".jpg")

        val fo: FileOutputStream
        try {
            destination.createNewFile()
            fo = FileOutputStream(destination)
            fo.write(bytes.toByteArray())
            fo.flush()
            fo.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return Uri.fromFile(destination)
    }
}