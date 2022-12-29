package com.hl3hl3.arcoremeasure

import android.R.attr
import android.net.Uri
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.service.autofill.OnClickAction
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.Logger
import com.google.ar.core.*
import com.google.ar.core.examples.java.helloar.DisplayRotationHelper
import kotlinx.android.synthetic.main.activity_measure.*
import java.util.*
import java.util.concurrent.ArrayBlockingQueue
import kotlin.math.sqrt
import com.hl3hl3.arcoremeasure.R // добавил
import kotlin.math.abs
import android.widget.Button
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.activity_entry.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import android.content.Intent
import android.util.Log
import android.webkit.WebView
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL
import okhttp3.OkHttpClient
import android.R.attr.data
import android.R.attr.firstBaselineToTopHeight
import java.lang.Exception
import java.io.BufferedReader
import java.io.InputStreamReader
import android.os.AsyncTask
import android.os.StrictMode

import android.R.string.no
import android.os.Build
import android.os.StrictMode.ThreadPolicy
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONStringer
import com.google.gson.JsonObject

import com.google.gson.JsonArray
import com.google.gson.annotations.SerializedName


class MeasureActivity : AppCompatActivity() {
    private var session: Session? = null
    private var gestureDetector: GestureDetector? = null
    private val client = OkHttpClient()
    private var displayRotationHelper: DisplayRotationHelper? = null
    private var id = ""
    private var name = ""
    private var image = ""
    // Tap handling and UI.
    private val QUEUED_SIZE= 16
    private val queuedSingleTaps = ArrayBlockingQueue<MotionEvent>(QUEUED_SIZE)
    private val queuedScrollDx = ArrayBlockingQueue<Float>(QUEUED_SIZE)
    private val queuedScrollDy = ArrayBlockingQueue<Float>(QUEUED_SIZE)
    private val gestureDetectorListener = object : SimpleOnGestureListener() {
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            // Queue tap if there is space. Tap is lost if queue is full.
            queuedSingleTaps.offer(e)
            //            log(TAG, "onSingleTapUp, e=" + e.getRawX() + ", " + e.getRawY());
            return true
        }

        override fun onDown(e: MotionEvent): Boolean {
            return true
        }

        override fun onScroll(
            e1: MotionEvent, e2: MotionEvent,
            distanceX: Float, distanceY: Float
        ): Boolean {
//            log(TAG, "onScroll, dx=" + distanceX + " dy=" + distanceY);
            queuedScrollDx.offer(distanceX)
            queuedScrollDy.offer(distanceY)
            return true
        }
    }

    private var currentSelected = 0
    private var glSerfaceRenderer: GLSurfaceRenderer? = null

    private val anchors = ArrayList<Anchor>()
    private val renderListener = object : GLSurfaceRenderer.RenderListener {

        fun drawCube(index: Int, lastTap: MotionEvent?, renderer: GLSurfaceRenderer) {
            renderer.drawCube(anchors[index])

            lastTap?.let {
                if (renderer.isHitObject(it)) {
                    currentSelected = index
                    queuedSingleTaps.poll()
                }
            }
        }

        override fun onFrame(
            renderer: GLSurfaceRenderer,
            frame: Frame,
            camera: Camera,
            viewWidth: Int,
            viewHeight: Int
        ) {
            // draw cube & line from last frame
            if (anchors.size < 1) {
                // no point
                showResult("")
            } else {
                // draw selected cube
                renderer.drawSelectedCube(anchors[currentSelected])

                val sb = StringBuilder()
                var total = 0.0
                var point1: Pose
                var area = 0.0

                // draw first cube
                var point0 = anchors[0].pose
                var firsstep = point0
                val lastTap = queuedSingleTaps.peek()
                drawCube(0, lastTap, renderer)
                val size = anchors.size
                var arrayOfVertex = Array(size + 1) { FloatArray(2) }

                // draw the rest cube
                arrayOfVertex[0][0] = point0.tx()
                arrayOfVertex[0][1] = point0.tz()
                for (i in 1 until anchors.size) {
                    point1 = anchors[i].pose

                    Logger.log("onDrawFrame()", "before drawObj()")
                    drawCube(i, lastTap, renderer)

                    Logger.log("onDrawFrame()", "before drawLine()")
                    renderer.drawLine(point0, point1)
                    renderer.drawLine(firsstep,point1)
                    val distanceCm = (getDistance(point0, point1) * 1000).toInt() / 10.0f
                    val areaCm = sqrt((getArea(arrayOfVertex) ))

                    total += distanceCm.toDouble()
                    area +=(areaCm)
                    sb.append(" + ").append(distanceCm)

                    point0 = point1

                    arrayOfVertex[i][0] = point0.tx()
                    arrayOfVertex[i][1] = point0.tz()
                }

                val button = findViewById<Button>(R.id.button)
                button.setOnClickListener{

                   /* val launchIntent =
                        packageManager.getLaunchIntentForPackage("hc.elite.getceiling")
                    launchIntent?.let { startActivity(it) }*/
                    Log.i("buttonadd", "sssss")

                    val gson = Gson()

                    val ar = gson.toJson(area)
                    val tot = gson.toJson(total)




                    try {
                        if (Build.VERSION.SDK_INT > 9) {
                            val gfgPolicy = ThreadPolicy.Builder().permitAll().build()
                            StrictMode.setThreadPolicy(gfgPolicy)
                        }

                      /*  val array = JsonArray()
                        array.add("test1")
                        array.add("test2")
                        val `object` = JsonObject()
                        `object`.add("arr", array)*/



                        val url = URL("https://ezmaven.com:7027/users/setParamsAr")
                        val postData = "id="+image+"&name="+name+ "&image="+id +"&total="+total

                        val conn = url.openConnection()
                        conn.doOutput = true
                        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")


                        DataOutputStream(conn.getOutputStream()).use { it.writeBytes(postData) }
                        BufferedReader(InputStreamReader(conn.getInputStream())).use { bf ->
                            var line: String?
                            while (bf.readLine().also { line = it } != null) {
                                println(line)
                            }
                        }
                    } catch (res: Exception){
                        Log.i("serverres" , res.toString())
                    }

                         finishAndRemoveTask();
                }
                val btn = findViewById<Button>(R.id.cancel)
                btn.setOnClickListener{
                    for (i in 1 until anchors.size){
                       /* if (anchors.size == 1) {
                            anchors[0].detach()

                            anchors.removeAt(0)*/
                        Log.i("detfir", "ssss")
                        anchors[i].detach()
                        anchors.removeAt(i)
                        if (anchors.size <= 1) {

                            anchors[0].detach()
                            anchors.removeAt(0)
                        }


                        }
                    }



                // show result



                showResult(
                    sb.toString().replaceFirst(
                        "[+]".toRegex(),
                        ""


                    ) + "" + " см"// + (total * 10f).toInt() / 10f + "cm"  +  (area * 10.0f).toInt() / 10f + "m^2 "

                )

            }


            // check if there is any touch event
            queuedSingleTaps.poll()?.let { tap ->
                if (camera.trackingState == TrackingState.TRACKING) {
                    for (hit in frame.hitTest(tap)) {
                        // Check if any plane was hit, and if it was hit inside the plane polygon.j
                        val trackable = hit.trackable
                        // Creates an anchor if a plane or an oriented point was hit.
                        if (trackable is Plane && trackable.isPoseInPolygon(hit.hitPose)
                            || (trackable is Point
                                    && trackable.orientationMode
                                    == Point.OrientationMode.ESTIMATED_SURFACE_NORMAL)
                        ) {
                            // Cap the number of objects created. This avoids overloading both the
                            // rendering system and ARCore.
                            if (anchors.size >= 2) {

                                anchors[0].detach()
                                anchors.removeAt(0)
                            }

                            // Adding an Anchor tells ARCore that it should track this position in
                            // space. This anchor will be used in PlaneAttachment to place the 3d model
                            // in the correct position relative both to the world and to the plane.
                            anchors.add(hit.createAnchor())
                            break
                        }
                    }
                }
            }
        }
    }

    private fun getDistance(pose0: Pose, pose1: Pose ): Double {
        val dx = pose0.tx() - pose1.tx()
        val dy = pose0.ty() - pose1.ty()
        val dz = pose0.tz() - pose1.tz()
        return sqrt((dx * dx + dz * dz + dy * dy).toDouble())
    }

    private fun getArea(arr: Array<FloatArray>): Double {
        val n = arr.size

        arr[n - 1][0] = arr[0][0]
        arr[n - 1][1] = arr[0][1]
        var det = 0.0

        for (i in 0 until n - 1) det += (arr[i][0] * arr[i + 1][1]).toDouble()

        for (i in 0 until n - 1) det -= (arr[i][1] * arr[i + 1][0]).toDouble()

        det = abs(det)
        det /= 2.0
        return det
    }

    private fun showResult(result: String) {
        runOnUiThread { tv_result.text = result }
    }
    private fun showMessage(message: String) {
        tvResult.text = message
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_measure)
        id = intent.extras!!.getString("getter").toString()
        name = intent.extras!!.getString("setter").toString()
        image = intent.extras!!.getString("infa").toString()
    }

    override fun onResume() {
        super.onResume()
        Logger.logStatus("onResume()")
        initiate()
        session?.resume()
        surfaceView?.onResume()
        displayRotationHelper?.onResume()
    }

    override fun onPause() {
        super.onPause()
        Logger.logStatus("onPause()")
        session?.pause()
        surfaceView?.onPause()
        displayRotationHelper?.onPause()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        Logger.logStatus("onWindowFocusChanged()")
        if (hasFocus) {
            // Standard Android full-screen functionality.
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun initiate() {
        val rotationHelper = DisplayRotationHelper(this)

        // session
        val arcoreSession = Session(this)
        val config = Config(arcoreSession)
        arcoreSession.configure(config)
        session = arcoreSession

        // renderer & surfaceview
        if (gestureDetector == null) {
            glSerfaceRenderer =
                GLSurfaceRenderer(this, arcoreSession, rotationHelper, renderListener)
            gestureDetector = GestureDetector(this, gestureDetectorListener)
            surfaceView?.apply {
                setOnTouchListener { v, event ->
                    gestureDetector?.onTouchEvent(
                        event
                    ) ?: false
                }

                preserveEGLContextOnPause = true
                setEGLContextClientVersion(2)
                setEGLConfigChooser(8, 8, 8, 8, 16, 0) // Alpha used for plane blending.
                setRenderer(glSerfaceRenderer)
                renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
            }
        }
        displayRotationHelper = rotationHelper
    }
}

