// File: android/src/main/kotlin/com/example/flutter_upi_payment/FlutterUpiPaymentPlugin.kt

package com.example.flutter_upi_payment

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Base64
import androidx.annotation.NonNull
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry
import java.util.*

class FlutterUpiPaymentPlugin: FlutterPlugin, MethodCallHandler, ActivityAware, PluginRegistry.ActivityResultListener {
    private lateinit var channel: MethodChannel
    private var activity: Activity? = null
    private var pendingResult: Result? = null
    private val PAYMENT_REQUEST_CODE = 123

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "flutter_upi_payment")
        channel.setMethodCallHandler(this)
    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        when (call.method) {
            "getUPIApps" -> {
                val upiApps = getUPIApps()
                result.success(upiApps)
            }
            "initiateTransaction" -> {
                val upiId = call.argument<String>("upiId")
                val amount = call.argument<String>("amount")
                val appPackageName = call.argument<String>("appPackageName")
                val transactionRef = call.argument<String>("transactionRef")
                val transactionNote = call.argument<String>("transactionNote")
                val merchantCode = call.argument<String>("merchantCode")

                if (upiId == null || amount == null || appPackageName == null) {
                    result.error("INVALID_ARGUMENTS", "Required parameters missing", null)
                    return
                }

                initiateTransaction(upiId, amount, appPackageName, transactionRef, transactionNote, merchantCode, result)
            }
            else -> result.notImplemented()
        }
    }

    private fun getUPIApps(): List<Map<String, String>> {
        val packageManager = activity?.packageManager ?: return emptyList()
        val upiApps = mutableListOf<Map<String, String>>()

        val upiIntent = Intent()
        upiIntent.data = Uri.parse("upi://pay")

        val resolveInfoList = packageManager.queryIntentActivities(upiIntent, 0)
        
        for (resolveInfo in resolveInfoList) {
            val packageName = resolveInfo.activityInfo.packageName
            val appName = resolveInfo.loadLabel(packageManager).toString()
            
            // Get app icon and convert to Base64
            val icon = resolveInfo.loadIcon(packageManager)
            val iconBase64 = try {
                // Convert drawable to Base64 string
                // Implementation details omitted for brevity
                ""
            } catch (e: Exception) {
                null
            }

            upiApps.add(mapOf(
                "packageName" to packageName,
                "appName" to appName,
                "iconBase64" to (iconBase64 ?: "")
            ))
        }

        return upiApps
    }

    private fun initiateTransaction(
        upiId: String,
        amount: String,
        appPackageName: String,
        transactionRef: String?,
        transactionNote: String?,
        merchantCode: String?,
        result: Result
    ) {
        val uri = Uri.Builder()
            .scheme("upi")
            .authority("pay")
            .appendQueryParameter("pa", upiId)
            .appendQueryParameter("pn", "Merchant Name") // Can be made configurable
            .appendQueryParameter("tn", transactionNote ?: "UPI Payment")
            .appendQueryParameter("am", amount)
            .appendQueryParameter("cu", "INR")
            .appendQueryParameter("tr", transactionRef ?: UUID.randomUUID().toString())
            
        if (merchantCode != null) {
            uri.appendQueryParameter("mc", merchantCode)
        }

        val upiPayIntent = Intent()
        upiPayIntent.data = uri.build()
        upiPayIntent.setPackage(appPackageName)

        try {
            pendingResult = result
            activity?.startActivityForResult(upiPayIntent, PAYMENT_REQUEST_CODE)
        } catch (e: Exception) {
            result.error("FAILED", "Failed to launch UPI app", e.message)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (requestCode == PAYMENT_REQUEST_CODE) {
            if (data != null) {
                val response = mapOf(
                    "Status" to (data.getStringExtra("Status") ?: "FAILED"),
                    "txnId" to data.getStringExtra("txnId"),
                    "responseCode" to data.getStringExtra("responseCode"),
                    "ApprovalRefNo" to data.getStringExtra("ApprovalRefNo"),
                    "txnRef" to data.getStringExtra("txnRef")
                )
                pendingResult?.success(response)
            } else {
                pendingResult?.error("FAILED", "Payment canceled", null)
            }
            pendingResult = null
            return true
        }
        return false
    }

    // Implementation of other required interface methods
    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activity = binding.activity
        binding.addActivityResultListener(this)
    }

    override fun onDetachedFromActivityForConfigChanges() {
        activity = null
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        activity = binding.activity
        binding.addActivityResultListener(this)
    }

    override fun onDetachedFromActivity() {
        activity = null
    }
}