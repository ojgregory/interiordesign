package uk.ac.plymouth.interiordesign.Fragments

import android.Manifest
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.renderscript.RenderScript
import android.util.DisplayMetrics
import android.util.Log
import android.util.Size
import android.view.*
import android.view.GestureDetector.SimpleOnGestureListener
import android.widget.AdapterView
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import kotlinx.android.synthetic.main.fragment_camera.*
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions
import uk.ac.plymouth.interiordesign.CameraWrapper
import uk.ac.plymouth.interiordesign.Room.Colour
import uk.ac.plymouth.interiordesign.ColourActivity
import uk.ac.plymouth.interiordesign.Processors.ProcessingCoordinator
import uk.ac.plymouth.interiordesign.R
import uk.ac.plymouth.interiordesign.SettingsActivity

// The main fragment sets up the camera, shows preview and initialises ProcessingCoordinator
class CameraFragment : Fragment(), CameraWrapper.ErrorDisplayer, CameraWrapper.CameraReadyListener {
    private lateinit var processingCoordinator: ProcessingCoordinator
    private lateinit var mRS: RenderScript
    private lateinit var mUiHandler: Handler
    private lateinit var mPreviewRequest: CaptureRequest
    private lateinit var mPreviewSurface: Surface
    private var colour =
        Colour(255, 0, 0, 255, "Red")

    private var cameraWrapper: CameraWrapper? = null
    private lateinit var outputSize : Size


    private val cameraManager by lazy {
        activity?.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    // Finds the optimal output size based on possible camera outputs,
    // a given maximum width and a target aspect ratio
    private fun selectOutputSize(outputSizes: Array<Size>, MAX_WIDTH:Int, TARGET_ASPECT:Float, ASPECT_TOLERANCE : Float) : Size {
        var outputSize: Size = outputSizes.last()
        var outputAspect : Float =
            (outputSize.width / outputSize.height).toFloat()
        for (candidateSize in outputSizes) {
            if (candidateSize.width > MAX_WIDTH) continue
            val candidateAspect : Float =
                candidateSize.width.toFloat() / candidateSize.height
            val goodCandidateAspect: Boolean =
                Math.abs(candidateAspect - TARGET_ASPECT) < ASPECT_TOLERANCE
            val goodOutputAspect: Boolean =
                Math.abs(outputAspect - TARGET_ASPECT) < ASPECT_TOLERANCE
            if ((goodCandidateAspect && !goodOutputAspect) || (candidateSize.width > outputSize.width)) {
                outputSize = candidateSize
                outputAspect = candidateAspect
            }
        }
        return outputSize
    }

    // Sets up preview
    private fun previewSession() {
        val displayMetrics = DisplayMetrics()
        activity?.getWindowManager()?.getDefaultDisplay()?.getMetrics(displayMetrics)
        val height = displayMetrics.heightPixels
        val width = displayMetrics.widthPixels
        val MAX_WIDTH = 1280
        var TARGET_ASPECT : Float = width.toFloat() / height
        val ASPECT_TOLERANCE = 0.01f

        // Find possible output sizes based on camera outputs
        val outputSizes: Array<Size> = cameraCharacteristics(
            cameraId(CameraCharacteristics.LENS_FACING_BACK),
            CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
        )!!
            .getOutputSizes(ImageFormat.YUV_420_888)

        // Select an outputSize that matches the criteria of max_width and target_aspect
        var outputSize = selectOutputSize(outputSizes, MAX_WIDTH, TARGET_ASPECT, ASPECT_TOLERANCE)
        var outputAspect = outputSize.width.toFloat() / outputSize.height

        // If the chosen output is too far from the aim based on screen aspect
        // Aim for 16:9
        if (!(Math.abs(outputAspect - TARGET_ASPECT) < ASPECT_TOLERANCE)) {
            TARGET_ASPECT = 16.0f / 9.0f

            outputSize = selectOutputSize(outputSizes, MAX_WIDTH, TARGET_ASPECT, ASPECT_TOLERANCE)
            outputAspect = outputSize.width.toFloat() / outputSize.height
        }

        // Show output resolution for debugging
        Log.i(TAG, "Resolution chosen: $outputSize")

        // Load stored preferences, if not selected defaults will be chosen
        val prefs = PreferenceManager.getDefaultSharedPreferences(this.context)
        val preprocessor_choice = prefs.getString("preprocessor_list", "1")!!.toInt()
        val preprocessor_sigma_choice = prefs.getString("sigma", "1.6")!!.toDouble()
        val preprocessor_mask_choice = prefs.getString("preprocessor_mask_list", "1")!!.toInt()
        val processor_choice = prefs.getString("processor_list", "1")!!.toInt()
        val filler_choice = prefs.getString("filler_list", "2")!!.toInt()

        // Initialise coordinator
        processingCoordinator = ProcessingCoordinator(
            preprocessor_choice,
            processor_choice,
            filler_choice,
            mRS,
            outputSize
        )
        // Set colour for filler
        processingCoordinator.setColour(colour)
        // Set config values for Gaussian Blur
        processingCoordinator.setGaussianMaskSize(preprocessor_mask_choice)
        processingCoordinator.setGaussianSigma(preprocessor_sigma_choice)
        setupProcessor()
        this.outputSize = outputSize

        // Configure the output view - this will fire surfaceChanged
        previewSurfaceView.setAspectRatio(outputAspect)
        previewSurfaceView.getHolder().setFixedSize(outputSize.width, outputSize.height)
    }

    // Give surfaces to output camera to
    private fun setupProcessor() {
        if (!(::processingCoordinator.isInitialized) || !(::mPreviewSurface.isInitialized)) return
        processingCoordinator.setOutputSurface(mPreviewSurface)

        // Creates list of Surfaces where the camera will output frames
        val targets = listOf(processingCoordinator.getInputSurface())
        cameraWrapper!!.setSurfaces(targets)
    }

    private val surfaceViewGestureListener: GestureDetector.OnGestureListener =
        object : SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean {
                return true
            }

            override fun onSingleTapUp(e: MotionEvent): Boolean {
                // Translate surface x and y to image x and y for filler
                // Not completely accurate - needs refinement
                val x = (e.rawX * outputSize.width) / previewSurfaceView.width
                val y = (e.rawY * outputSize.height) / previewSurfaceView.height
                processingCoordinator.setFillerXandY(x.toInt(), y.toInt())
                return true
            }
        }

    private val surfaceHolderCallback = object : SurfaceHolder.Callback {
        override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
            mPreviewSurface = holder!!.surface
            setupProcessor()
        }

        override fun surfaceDestroyed(holder: SurfaceHolder?) {
            mPreviewSurface.release()
        }

        // Once available setup camera
        override fun surfaceCreated(holder: SurfaceHolder?) {
            openCamera()
        }

    }

    // Open colour list on selecting button
    private val colourButtonListener = object : View.OnClickListener {
        override fun onClick(v: View?) {
            val intent = Intent(context, ColourActivity::class.java).apply{}
            startActivityForResult(intent, 0)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 0) {
            if (resultCode == RESULT_OK) {
                // Reassemble colour from individual values
                val name = data!!.getStringExtra("name")
                val r = data.getIntExtra("r", 0)
                val g = data.getIntExtra("g", 0)
                val b = data.getIntExtra("b", 0)
                val a = data.getIntExtra("a", 0)
                colour =
                    Colour(r, g, b, a, name!!)
                // Also apply to other places where needed
                colourDisplay.setBackgroundColor(colour.rgba)
                processingCoordinator.setColour(colour)
            }
        }

    }

    // Returns characteristics for chosen camera
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

    // Asks for camera permission, if found find compatible camera and open
    @AfterPermissionGranted(REQUEST_CAMERA_PERMISSION)
    private fun checkCameraPermission() {
        if (EasyPermissions.hasPermissions(requireActivity(), Manifest.permission.CAMERA)) {
            Log.d(TAG, "App has camera permission")
            findAndOpenCamera()
        } else {
            EasyPermissions.requestPermissions(
                requireActivity(),
                getString(R.string.camera_request_rationale),
                REQUEST_CAMERA_PERMISSION,
                Manifest.permission.CAMERA
            )
        }
    }

    // Opens settings upon settings button being pressed
    private val settingsButtonListener = object : View.OnClickListener {
        override fun onClick(v: View?) {
            val intent = Intent(context, SettingsActivity::class.java).apply{}
            startActivity(intent)
        }

    }

    override fun onPause() {
        super.onPause()
        // Wait until camera is closed to ensure the next application can open it
        if (cameraWrapper != null) {
            cameraWrapper!!.closeCameraAndWait()
            cameraWrapper = null
        }

        processingCoordinator.closeAllocationsAndStop()
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
        // Setup various listeners
        previewSurfaceView.holder.addCallback(surfaceHolderCallback)
        previewSurfaceView.setGestureListener(this.context, surfaceViewGestureListener)
        settings_button.setOnClickListener(settingsButtonListener)
        colourDisplay.setOnClickListener(colourButtonListener)
        colourDisplay.setBackgroundColor(colour.rgba)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mRS = RenderScript.create(requireContext().applicationContext)
        mUiHandler = Handler(Looper.getMainLooper())
    }

    private fun openCamera() {
        checkCameraPermission()
    }

    override fun onCameraReady() {
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
        val errorMessage = when (e!!.reason) {
            CameraAccessException.CAMERA_DISABLED -> getString(R.string.camera_disabled)
            CameraAccessException.CAMERA_DISCONNECTED -> getString(R.string.camera_disconnected)
            CameraAccessException.CAMERA_ERROR -> getString(R.string.camera_error)
            else -> getString(R.string.camera_unknown, e.reason)
        }
        return errorMessage
    }
}
