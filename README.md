# Lock Screen Permission App  ( CLIENT )

## Overview
The **LockCompose** app is designed to provide users with a customizable interface for selecting installed applications and managing their usage rules. The app includes features for sending selected applications' data to another application through a ContentProvider, along with an easy-to-use interface for selecting time intervals and entering a PIN code.

## Features
- **Display Installed Apps**: The app fetches and displays a list of all installed applications on the device, allowing users to select which apps they want to manage.
- **Selectable Time Intervals**: Users can choose from predefined time intervals for managing application usage.
- **PIN Code Entry**: The app includes a secure input field for entering a PIN code, which is required for confirming actions.
- **Data Transmission**: Selected application data, including package name, app name, icon, selected interval, and PIN code, can be sent to another application via a ContentProvider.
- **User-Friendly UI**: Built with Jetpack Compose, the app features a responsive and modern user interface.

## Components
- **ShowAppList**: The main Composable function that renders the app's UI, including a list of installed applications and the controls for selecting intervals and entering the PIN code.
- **InstalledApp Data Class**: Represents an installed application, holding its package name, name, and icon.
- **getInstalledApps Function**: Fetches the list of installed applications on the device.
- **AppListItem Composable**: Renders each installed application in the list with its icon and name.

## Requirements
- Android 5.0 (API level 21) or higher.
- Jetpack Compose for UI components.
