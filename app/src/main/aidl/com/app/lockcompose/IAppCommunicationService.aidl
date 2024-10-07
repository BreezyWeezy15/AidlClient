// IAppCommunicationService.aidl
package com.app.lockcompose;

interface IAppCommunicationService {
    void sendAppData(in List<String> selectedAppPackages, in String timeInterval, in String pinCode);
}