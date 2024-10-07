// IAppCommunicationService.aidl
package com.app.lockcompose;
parcelable AppInfo;

// Interface for communication between apps
interface IAppCommunicationService {
    void sendAppData(in List<AppInfo> apps, in String interval, in String pinCode);
}