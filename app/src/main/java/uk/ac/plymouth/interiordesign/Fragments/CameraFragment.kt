package uk.ac.plymouth.interiordesign.Fragments

import android.Manifest
import android.content.Context
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.renderscript.RenderScript
import android.util.Log
import android.util.Size
import android.view.*
import android.widget.AdapterView
import android.widget.Button
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_camera.*
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions
import uk.ac.plymouth.interiordesign.CameraWrapper
import uk.ac.plymouth.interiordesign.Processors.GaussianProcessor
import uk.ac.plymouth.interiordesign.Processors.ProcessingCoordinator
import uk.ac.plymouth.interiordesign.R
import uk.ac.plymouth.interiordesign.Processors.SobelProcessor


class CameraFragment : Fragment(), CameraWrapper.ErrorDisplayer, CameraWrapper.CameraReadyListener {
    private lateinit var processingCoordinator: ProcessingCoordinator
    private lateinit var mRS: RenderScript
    private lateinit var mUiHandler: Handler
    private lateinit var mPreviewRequest: CaptureRequest

    private var cameraWrapper: CameraWrapper? = null


    private val cameraManager by lazy {
        activity?.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }


    private fun previewSession() {
        val MAX_WIDTH = 1280
        val TARGET_ASPECT = textureview.width / textureview.height
        val ASPECT_TOLERANCE = 0.1f;
        val surfaceTexture = textureview.surfaceTexture
        val surface = Surface(surfaceTexture)

        // Initialize an image reader which will be used to apply filter to preview
        val outputSizes: Array<Size> = cameraCharacteristics(
            cameraId(CameraCharacteristics.LENS_FACING_BACK),
            CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
        )!!
            .getOutputSizes(ImageFormat.YUV_420_888)

        var outputSize: Size = outputSizes.last()
        var outputAspect =
            outputSize.width / outputSize.height
        for (candidateSize in outputSizes) {
            if (candidateSize.width > MAX_WIDTH) continue
            val candidateAspect =
                candidateSize.width / candidateSize.height
            val goodCandidateAspect: Boolean =
                Math.abs(candidateAspect - TARGET_ASPECT) < ASPECT_TOLERANCE
            val goodOutputAspect: Boolean =
                Math.abs(outputAspect - TARGET_ASPECT) < ASPECT_TOLERANCE
            if (goodCandidateAspect && !goodOutputAspect ||
                candidateSize.width > outputSize.width
            ) {
                outputSize = candidateSize
                outputAspect = candidateAspect
            }
        }
        // Switch width and height for portrait
        val size:Size = Size(outputSize.height, outputSize.width)
        Log.i(TAG, "Resolution chosen: $size")

        textureview.rotation = 90.0f

        // Configure processing
        // Configure processing
        processingCoordinator = ProcessingCoordinator(
            0,
            0,
            mRS,
            size
        )
        processingCoordinator.setOutputSurface(surface)

        // Creates list of Surfaces where the camera will output frames
        val targets = listOf(processingCoordinator.getInputSurface())
        cameraWrapper!!.setSurfaces(targets)
    }

    private val surfaceListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureSizeChanged(
            surface: SurfaceTexture?,
            width: Int,
            height: Int
        ) {
        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) = Unit

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?) = true

        override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
            Log.d(TAG, "textureSurface width: $width height: $height")
            openCamera()
        }

    }


    private fun <T> cameraCharacteristics(cameraId: String, key: CameraCharacteristics.Key<T>): T? {
        val characteristics = cameraManager.getCameraCharacteristics(cameraId)
        return when (key) {
            CameraCharacteristics.LENS_FACING -> characteristics.get(key)
            CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP -> characteristics.get(key)
            else -> throw  IllegalArgumentException("Key not recognized")
        }
    }

    private fun cameraId(lens: Int): String {
        var deviceId = listOf<String>()
        try {
            val cameraIdList = cameraManager.cameraIdList
            deviceId = cameraIdList.filter {
                lens == cameraCharacteristics(
                    it,
                    CameraCharacteristics.LENS_FACING
                )
            }
        } catch (e: CameraAccessException) {
            Log.e(TAG, e.toString())
        }
        return deviceId[0]
    }

    companion object {
        const val REQUEST_CAMERA_PERMISSION = 100
        private val TAG = CameraFragment::class.qualifiedName
        @JvmStatic
        fun newInstance() = CameraFragment()
    }

    /**
     * Attempt to initialize the camera.
     */
    private fun initializeCamera() {
        if (cameraManager != null) {
            cameraWrapper = CameraWrapper(
                cameraManager,  /*errorDisplayer*/
                this,  /*readyListener*/
                this,  /*readyHandler*/
                mUiHandler
            )
        } else {
            Log.e(
                TAG,
                "Couldn't initialize the camera"
            )
        }
    }

    private fun hasCapability(capabilities: IntArray, capability: Int): Boolean {
        for (c in capabilities) {
            if (c == capability) return true
        }
        return false
    }

    private fun findAndOpenCamera() {
        var errorMessage: String? = "Unknown error"
        var foundCamera = false
        initializeCamera()
        if (cameraWrapper != null) {
            try { // Find first back-facing camera that has necessary capability.
                val cameraIds: Array<String> = cameraManager.getCameraIdList()
                for (id in cameraIds) {
                    val info: CameraCharacteristics = cameraManager.getCameraCharacteristics(id)
                    val facing = info.get(CameraCharacteristics.LENS_FACING)
                    val level =
                        info.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
                    val hasFullLevel =
                        level == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL
                    val capabilities = info
                        .get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
                    val syncLatency = info.get(CameraCharacteristics.SYNC_MAX_LATENCY)
                    val hasManualControl: Boolean = hasCapability(
                        capabilities!!,
                        CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR
                    )
                    val hasEnoughCapability =
                        hasManualControl && syncLatency == CameraCharacteristics.SYNC_MAX_LATENCY_PER_FRAME_CONTROL
                    // All these are guaranteed by
                    // CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL, but checking
                    // for only the things we care about expands range of devices we can run on.
                    // We want:
                    //  - Back-facing camera
                    //  - Manual sensor control
                    //  - Per-frame synchronization (so that exposure can be changed every frame)
                    if (facing == CameraCharacteristics.LENS_FACING_BACK &&
                        (hasFullLevel || hasEnoughCapability)
                    ) { // Found suitable camera - get info, open, and set up outputs
                        cameraWrapper!!.openCamera(id)
                        previewSession()
                        foundCamera = true
                        break
                    }
                }
                if (!foundCamera) {
                    //errorMessage = getString(R.string.camera_no_good)
                }
            } catch (e: CameraAccessException) {
                errorMessage = getErrorString(e)
            }
            if (!foundCamera) {
                showErrorDialog(errorMessage)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    @AfterPermissionGranted(REQUEST_CAMERA_PERMISSION)
    private fun checkCameraPermission() {
        if (EasyPermissions.hasPermissions(activity!!, Manifest.permission.CAMERA)) {
            Log.d(TAG, "App has camera permission")
            findAndOpenCamera()
        } else {
            EasyPermissions.requestPermissions(
                activity!!,
                getString(R.string.camera_request_rationale),
                REQUEST_CAMERA_PERMISSION,
                Manifest.permission.CAMERA
            )
        }
    }

    private val processorSpinnerListener = object : AdapterView.OnItemSelectedListener {
        override fun onNothingSelected(parent: AdapterView<*>?) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            processingCoordinator.chooseProcessor(position)
        }
    }

    private val preProcessorSpinnerListener = object : AdapterView.OnItemSelectedListener {
        override fun onNothingSelected(parent: AdapterView<*>?) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            processingCoordinator.choosePreProcessor(position)
        }
    }

    private val gaussianSpinnerListener = object : AdapterView.OnItemSelectedListener {
        override fun onNothingSelected(parent: AdapterView<*>?) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            when(position) {
                0 -> processingCoordinator.setGaussianMaskSize(3)
                1 -> processingCoordinator.setGaussianMaskSize(5)
            }
        }
    }

    private val gaussianButtonListener = object : View.OnClickListener {
        override fun onClick(v: View?) {
            processingCoordinator.setGaussianSigma(gaussianSigmaEditText.text.toString().toDouble())
        }

    }

    override fun onResume() {
        super.onResume()
        if (textureview.isAvailable)
            openCamera()
        else
            textureview.surfaceTextureListener = surfaceListener

        activity?.window?.decorView?.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    }

    override fun onPause() {
        super.onPause()
        // Wait until camera is closed to ensure the next application can open it
        // Wait until camera is closed to ensure the next application can open it
        if (cameraWrapper != null) {
            cameraWrapper!!.closeCameraAndWait()
            cameraWrapper = null
        }

        processingCoordinator.closeAllocations()

        activity?.window?.decorView?.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_camera, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity?.window?.decorView?.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        preprocesserSpinner.onItemSelectedListener = preProcessorSpinnerListener
        processorSpinner.onItemSelectedListener = processorSpinnerListener
        gaussianSpinner.onItemSelectedListener = gaussianSpinnerListener
        gaussianButton.setOnClickListener(gaussianButtonListener)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mRS = RenderScript.create(this.activity!!)
        mUiHandler = Handler(Looper.getMainLooper());
    }

    private fun openCamera() {
        checkCameraPermission()
    }

    override fun onCameraReady() {
        // Ready to send requests in, so set them up
        // Ready to send requests in, so set them up
        try {
            val previewBuilder: CaptureRequest.Builder =
                cameraWrapper!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            //previewBuilder.addTarget(sobelProcessor.getInputNormalSurface())
            previewBuilder.addTarget(processingCoordinator.getInputSurface())
            mPreviewRequest = previewBuilder.build()
            cameraWrapper!!.setRepeatingRequest(mPreviewRequest, null, mUiHandler)
        } catch (e: CameraAccessException) {
            val errorMessage = getErrorString(e)
            showErrorDialog(errorMessage)
        }
    }

    override fun showErrorDialog(errorMessage: String?) {
        /**
         * Utility methods

        MessageDialogFragment.newInstance(errorMessage)
        .show(
        getSupportFragmentManager(),
        com.example.android.hdrviewfinder.HdrViewfinderActivity.FRAGMENT_DIALOG
        )
         */

    }

    override fun getErrorString(e: CameraAccessException?): String? {
        val errorMessage: String = "AAAAAA!"
        /*errorMessage = when (e!!.reason) {
            CameraAccessException.CAMERA_DISABLED -> getString(R.string.camera_disabled)
            CameraAccessException.CAMERA_DISCONNECTED -> getString(R.string.camera_disconnected)
            CameraAccessException.CAMERA_ERROR -> getString(R.string.camera_error)
            else -> getString(R.string.camera_unknown, e.reason)
        }*/
        return errorMessage
    }
}
