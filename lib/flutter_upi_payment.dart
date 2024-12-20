// First, create the package structure
// File: lib/flutter_upi_payment.dart

import 'package:flutter/services.dart';
import 'dart:async';

class UPIApp {
  final String packageName;
  final String appName;
  final String? iconBase64;

  UPIApp({
    required this.packageName,
    required this.appName,
    this.iconBase64,
  });
}

class UPIResponse {
  final String status;
  final String? txnId;
  final String? txnRef;
  final String? responseCode;
  final String? approvalRefNo;

  UPIResponse({
    required this.status,
    this.txnId,
    this.txnRef,
    this.responseCode,
    this.approvalRefNo,
  });

  factory UPIResponse.fromMap(Map<String, dynamic> map) {
    return UPIResponse(
      status: map['Status'] ?? '',
      txnId: map['txnId'],
      txnRef: map['txnRef'],
      responseCode: map['responseCode'],
      approvalRefNo: map['ApprovalRefNo'],
    );
  }
}

class FlutterUPIPayment {
  static const MethodChannel _channel = MethodChannel('flutter_upi_payment');

  // Get list of all UPI apps installed on device
  static Future<List<UPIApp>> getUPIApps() async {
    try {
      final List<dynamic> apps = await _channel.invokeMethod('getUPIApps');
      return apps.map((app) => UPIApp(
        packageName: app['packageName'],
        appName: app['appName'],
        iconBase64: app['iconBase64'],
      )).toList();
    } on PlatformException catch (e) {
      throw 'Failed to get UPI apps: ${e.message}';
    }
  }

  // Initiate payment using selected UPI app
  static Future<UPIResponse> initiateTransaction({
    required String upiId,
    required double amount,
    required String appPackageName,
    String? transactionRef,
    String? transactionNote,
    String? merchantCode,
  }) async {
    try {
      final Map<String, dynamic> args = {
        'upiId': upiId,
        'amount': amount.toString(),
        'appPackageName': appPackageName,
        'transactionRef': transactionRef ?? DateTime.now().millisecondsSinceEpoch.toString(),
        'transactionNote': transactionNote ?? 'UPI Payment',
        'merchantCode': merchantCode,
      };

      final Map<String, dynamic> response = await _channel.invokeMethod('initiateTransaction', args);
      return UPIResponse.fromMap(response);
    } on PlatformException catch (e) {
      throw 'Payment failed: ${e.message}';
    }
  }
}

// Example usage:
/*
void makePayment() async {
  try {
    // Get list of UPI apps
    List<UPIApp> upiApps = await FlutterUPIPayment.getUPIApps();
    
    // Select an app (e.g., first available app)
    if (upiApps.isNotEmpty) {
      UPIResponse response = await FlutterUPIPayment.initiateTransaction(
        upiId: 'merchant@upi',
        amount: 100.0,
        appPackageName: upiApps[0].packageName,
        transactionNote: 'Test Payment',
      );
      
      // Handle response
      if (response.status == 'SUCCESS') {
        print('Payment successful');
      } else {
        print('Payment failed');
      }
    }
  } catch (e) {
    print('Error: $e');
  }
}
*/