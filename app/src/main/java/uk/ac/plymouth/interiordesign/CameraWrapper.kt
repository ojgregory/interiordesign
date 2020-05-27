/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.plymouth.interiordesign

import android.hardware.camera2.*
import android.hardware.camera2.CameraCaptureSession.CaptureCallback
import android.os.ConditionVariable
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import androidx.annotation.NonNull


class CameraWrapper(manager: CameraManager, errorDisplayer: ErrorDisplayer,
                    readyListener: CameraReadyListener, readyHandler: Handler) {
    private val TAG = CameraWrapper::class.qualifiedName
    private val CAMERA_CLOSE_TIMEOUT: Long = 2000 // ms

    private lateinit var mCaptureSession: CameraCaptureSession
    private var mCameraDevice: CameraDevice? = null
    private val mCameraManager : CameraManager
    private var mSurfaces: List<Surface>? = null
    private val mCloseWaiter = ConditionVariable()

    private val mCameraThread: HandlerThread
    private lateinit var mCameraHandler: Handler

    private val mErrorDisplayer: ErrorDisplayer

    private val mReadyListener: CameraReadyListener
    private val mReadyHandler: Handler


    init {
        mCameraThread = HandlerThread("CameraOpsThread")
        mCameraThread.start()

        require(!(manager == null || errorDisplayer == null || readyListener == null || readyHandler == null)) { "Need valid displayer, listener, handler" }

        mCameraManager = manager
        mErrorDisplayer = errorDisplayer
        mReadyListener = readyListener
        mReadyHandler = readyHandler
    }

    /**
     * Open the first back-facing camera listed by the camera manager.
     * Displays a dialog if it cannot open a camera.
     */
    fun openCamera(cameraId: String?) {
        mCameraHandler = Handler(mCameraThread.looper)
        mCameraHandler.post {
            check(mCameraDevice == null) { "Camera already open" }
            try {
                mCameraManager.openCamera(cameraId!!, deviceStateCallback, mCameraHandler)
            } catch (e: CameraAccessException) {
                val errorMessage = mErrorDisplayer.getErrorString(e)
                mErrorDisplayer.showErrorDialog(errorMessage)
            }
        }
    }

    /**
     * Close the camera and wait for the close callback to be called in the camera thread.
     * Times out after @{value CAMERA_CLOSE_TIMEOUT} ms.
     */
    fun closeCameraAndWait() {
        mCloseWaiter.close()
        mCameraHandler.post(mCloseCameraRunnable)
        val closed =  mCloseWaiter.block(CAMERA_CLOSE_TIMEOUT)
        if (!closed) {
            Log.e(
                TAG,
                "Timeout closing camera"
            )
        }
    }

    private val mCloseCameraRunnable = Runnable {
        if (mCameraDevice != null) {
            mCameraDevice?.close()
        }
    }

    /**
     * Set the output Surfaces, and finish configuration if otherwise ready.
     */
    fun setSurfaces(surfaces: List<Surface>) {
        mCameraHandler.post {
            mSurfaces = surfaces
            startCameraSession()
        }
    }

    /**
     * Get a request builder for the current camera.
     */
    @Throws(CameraAccessException::class)
    fun createCaptureRequest(template: Int): CaptureRequest.Builder {
        val device = mCameraDevice
            ?: throw IllegalStateException("Can't get requests when no camera is open")
        return device.createCaptureRequest(template)
    }

    /**
     * Set a repeating request.
     */
    fun setRepeatingRequest(
        request: CaptureRequest,
        listener: CaptureCallback?,
        handler: Handler?
    ) {
        mCameraHandler.post {
            try {
                mCaptureSession.setRepeatingRequest(request, listener, handler)
            } catch (e: CameraAccessException) {
                val errorMessage = mErrorDisplayer.getErrorString(e)
                mErrorDisplayer.showErrorDialog(errorMessage)
            }
        }
    }

    /**
     * Set a repeating request.
     */
    fun setRepeatingBurst(
        requests: List<CaptureRequest>,
        listener: CaptureCallback,
        handler: Handler
    ) {
        mCameraHandler.post {
            try {
                mCaptureSession.setRepeatingBurst(requests, listener, handler)
            } catch (e: CameraAccessException) {
                val errorMessage = mErrorDisplayer.getErrorString(e)
                mErrorDisplayer.showErrorDialog(errorMessage)
            }
        }
    }

    /**
     * Configure the camera session.
     */
    private fun startCameraSession() { // Wait until both the camera device is open and the SurfaceView is ready
        if (mCameraDevice == null || mSurfaces == null) return
        try {
            mCameraDevice?.createCaptureSession(
                mSurfaces!!, mCameraSessionListener, mCameraHandler
            )
        } catch (e: CameraAccessException) {
            val errorMessage = mErrorDisplayer.getErrorString(e)
            mErrorDisplayer.showErrorDialog(errorMessage)
            mCameraDevice?.close()
        }
    }

    /**
     * Main listener for camera session events
     * Invoked on mCameraThread
     */
    private val mCameraSessionListener: CameraCaptureSession.StateCallback =
        object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(@NonNull session: CameraCaptureSession) {
                mCaptureSession = session
                mReadyHandler.post(Runnable {
                    // This can happen when the screen is turned off and turned back on.
                    if (null == mCameraDevice) {
                        return@Runnable
                    }
                    mReadyListener.onCameraReady()
                })
            }

            override fun onConfigureFailed(@NonNull session: CameraCaptureSession) {
                mErrorDisplayer.showErrorDialog("Unable to configure the capture session")
                mCameraDevice?.close()
            }
        }

    private val deviceStateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            Log.d(TAG, "camera device opened")
            if (camera != null) {
                mCameraDevice = camera
                startCameraSession()
            }

        }

        override fun onClosed(camera: CameraDevice) {
            mCloseWaiter.open()
        }

        override fun onDisconnected(camera: CameraDevice) {
            mErrorDisplayer.showErrorDialog("The camera device has been disconnected.")
            camera.close()
        }

        override fun onError(camera: CameraDevice, error: Int) {
            Log.d(TAG, "camera device error")
            mErrorDisplayer.showErrorDialog("The camera encountered an error:" + error)
            camera.close()
       }

    }

    /**
     * Simple listener for main code to know the camera is ready for requests, or failed to
     * start.
     */
    interface CameraReadyListener {
        fun onCameraReady()
    }

    /**
     * Simple listener for displaying error messages
     */
    interface ErrorDisplayer {
        fun showErrorDialog(errorMessage: String?)
        fun getErrorString(e: CameraAccessException?): String?
    }
}