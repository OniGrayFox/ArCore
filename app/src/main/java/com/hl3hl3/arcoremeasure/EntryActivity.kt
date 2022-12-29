package com.hl3hl3.arcoremeasure

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.Logger
import com.google.ar.core.*
import com.google.ar.core.ArCoreApk.InstallStatus
import com.google.ar.core.examples.java.helloar.CameraPermissionHelper
import com.google.ar.core.exceptions.UnavailableApkTooOldException
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException
import com.google.ar.core.exceptions.UnavailableSdkTooOldException
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException
import kotlinx.android.synthetic.main.activity_entry.*
import com.hl3hl3.arcoremeasure.R // добавил
import androidx.appcompat.app.AppCompatDialogFragment


class MyDialogFragment : AppCompatDialogFragment()
class EntryActivity : AppCompatActivity() {
    private val TAG: String = "EntryActivity"
    private var installRequested: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_entry)
        val exstras = intent.extras
        val data: Uri? = intent.data

        if (data != null){
            val paramenters: List <String> = data.pathSegments
            val param1 = paramenters[paramenters.size-1]
            val param2 = paramenters[paramenters.size-3]
            val param3 = paramenters[paramenters.size-5]
            Log.i ("settor" , param1)
            Log.i ("settor" , param2)
            Log.i ("infa" , param3)
            val i = Intent(this, MeasureActivity::class.java)

            i.putExtra("getter" , param1)
            i.putExtra("setter", param2)
            i.putExtra("infa", param3)
            startActivity(i)

        }

        Log.i("intent",exstras.toString())
        val intent1 = intent
        val name = intent1.getExtras()
        Logger.logStatus(name.toString())
        Log.i("intent",name.toString())
        val uri = Uri.parse("https://vk.com/")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK


    }

    override fun onResume() {

        super.onResume()
        Logger.logStatus("onResume()")
        checkCanGo()


    }

    private fun checkCanGo() {
        var message: String? = null
        try {
            when (ArCoreApk.getInstance().requestInstall(this, !installRequested)) {
                InstallStatus.INSTALL_REQUESTED -> {
                    installRequested = true
                    return
                }
                InstallStatus.INSTALLED -> {
                }
            }

            // ARCore requires camera permissions to operate. If we did not yet obtain runtime
            // permission on Android M and above, now is a good time to ask the user for it.
            if (!CameraPermissionHelper.hasCameraPermission(this)) {
                CameraPermissionHelper.requestCameraPermission(this)
                return
            }
        } catch (e: UnavailableArcoreNotInstalledException) {
            message = "Please install ARCore"
            Logger.log(e)
        } catch (e: UnavailableUserDeclinedInstallationException) {
            message = "Please install ARCore"
            Logger.log(e)
        } catch (e: UnavailableApkTooOldException) {
            message = "Please update ARCore"
            Logger.log(e)
        } catch (e: UnavailableSdkTooOldException) {
            message = "Please update this app"
            Logger.log(e)
        }catch (e:com.google.ar.core.exceptions.FatalException )
        {
            message = "ZZZ"
            Logger.log(e)
        }
        catch (e: Exception) {
            message = "This device does not support AR"
            Logger.log(e)
        }

                  if (message != null) {
            showMessage(message)
            return
        } else {
            // TODO go
            startActivity(Intent(this, MeasureActivity::class.java))
        }
    }

    private fun showMessage(message: String) {
        tvResult.text = message
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Logger.logStatus("onRequestPermissionsResult()")
        if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(this)) {
            // Permission denied with checking "Do not ask again".
            CameraPermissionHelper.launchPermissionSettings(this)
        } else {

        }
    }

}