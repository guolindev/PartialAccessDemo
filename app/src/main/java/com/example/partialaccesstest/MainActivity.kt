package com.example.partialaccesstest

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.READ_MEDIA_IMAGES
import android.Manifest.permission.READ_MEDIA_VIDEO
import android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.concurrent.thread
import android.view.ViewTreeObserver.OnPreDrawListener as OnPreDrawListener1

class MainActivity : AppCompatActivity() {

    private lateinit var cardLayout: CardView
    private lateinit var button: Button
    private lateinit var textView: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AlbumAdapter

    private val imageList = ArrayList<Image>()

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { _ ->
            checkPermission()
            loadImages()
        }

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            permissionLauncher.launch(
                arrayOf(READ_MEDIA_IMAGES,
                    READ_MEDIA_VIDEO)
            )
        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(arrayOf(READ_MEDIA_IMAGES, READ_MEDIA_VIDEO))
        } else {
            permissionLauncher.launch(arrayOf(READ_EXTERNAL_STORAGE))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        cardLayout = findViewById(R.id.card_layout)
        button = findViewById(R.id.button)
        textView = findViewById(R.id.text_view)
        recyclerView = findViewById(R.id.recycler_view)

        button.setOnClickListener {
            requestPermissions()
        }

        recyclerView.viewTreeObserver.addOnPreDrawListener(object : OnPreDrawListener1 {
            override fun onPreDraw(): Boolean {
                recyclerView.viewTreeObserver.removeOnPreDrawListener(this)
                val columns = 3
                val imageSize = recyclerView.width / columns
                adapter = AlbumAdapter(this@MainActivity, imageList, imageSize)
                recyclerView.layoutManager = GridLayoutManager(this@MainActivity, columns)
                recyclerView.adapter = adapter
                loadImages()
                return false
            }
        })
        checkPermission()
    }

    private fun checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            && (ContextCompat.checkSelfPermission(this, READ_MEDIA_IMAGES) == PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this, READ_MEDIA_VIDEO) == PERMISSION_GRANTED)
        ) {
            // Full access on Android 13 (API level 33) or higher
            cardLayout.visibility = View.GONE
        } else if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            ContextCompat.checkSelfPermission(this, READ_MEDIA_VISUAL_USER_SELECTED) == PERMISSION_GRANTED
        ) {
            // Partial access on Android 14 (API level 34) or higher
            textView.text = "你已授权访问部分相册的照片和视频"
            button.text = "管理"
            cardLayout.visibility = View.VISIBLE
        } else if (ContextCompat.checkSelfPermission(this, READ_EXTERNAL_STORAGE) == PERMISSION_GRANTED) {
            // Full access up to Android 12 (API level 32)
            cardLayout.visibility = View.GONE
        } else {
            // Access denied
            textView.text = "你还未授权访问相册的照片和视频"
            button.text = "请求"
            cardLayout.visibility = View.VISIBLE
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun loadImages() {
        thread {
            imageList.clear()
            val cursor = contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null,
                null, null, "${MediaStore.MediaColumns.DATE_ADDED} desc"
            )
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    val id =
                        cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                    val uri =
                        ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                    imageList.add(Image(uri))
                }
                cursor.close()
            }
            runOnUiThread {
                adapter.notifyDataSetChanged()
            }
        }
    }
}
