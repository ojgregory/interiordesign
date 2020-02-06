package uk.ac.plymouth.interiordesign

import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.TextureView
import androidx.annotation.RequiresApi
import kotlinx.android.synthetic.main.activity_main.*
import uk.ac.plymouth.interiordesign.MainActivity.SurfaceTextureListener as SurfaceTextureListener

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class MainActivity : AppCompatActivity() {
    private lateinit var cameraDevice : CameraDevice
    private val cameraManager by lazy {
        getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    private lateinit var backgroundThread: HandlerThread
    private lateinit var backgroundHandler: Handler

    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("Camera2 Kotlin").also { it.start() }
        backgroundHandler = Handler(backgroundThread.looper)
    }

    private fun stopBackgroundThread() {
        backgroundThread.quitSafely()
        try {
            backgroundThread.join()
        } catch (e: InterruptedException) {
            Log.e(TAG, e.toString())
        }
    }

    private val deviceStateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            Log.d(TAG, "camera device opened")
            cameraDevice = camera
        }

        override fun onDisconnected(camera: CameraDevice) {
            Log.d(TAG, "camera device disconnected")
            camera.close()
        }

        override fun onError(camera: CameraDevice, error: Int) {
            Log.d(TAG, "camera device error")
            finish()
        }

    }

    private val surfaceListener = object: TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {
        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) = Unit

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?) = true

        override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
            Log.d(TAG, "textureSurface width: $width height: $height")
            openCamera()
        }

    }

    companion object {
        const val REQUEST_CAMERA_PERMISSION = 100
        private val TAG = MainActivity::class.qualifiedName
        @JvmStatic fun newInstance() = MainActivity()
    }

    override fun onResume() {
        super.onResume()

        startBackgroundThread()
        if (textureview.isAvailable)
            openCamera()
        else
            textureview.surfaceTextureListener = surfaceListener
    }

    override fun onPause() {

        stopBackgroundThread()
        super.onPause()
    }

    private fun openCamera() {
        checkCameraPermission()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

    }
}
