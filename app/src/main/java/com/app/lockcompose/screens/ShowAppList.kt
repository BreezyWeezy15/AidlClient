
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.IBinder
import android.os.RemoteException
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.lockcompose.IAppCommunicationService


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowAppList() {
    val context = LocalContext.current
    var appService: IAppCommunicationService? by remember { mutableStateOf(null) }
    var isBound by remember { mutableStateOf(false) }

    // List of installed apps
    val allApps = remember { getInstalledApps(context) }
    val availableApps by remember { mutableStateOf(allApps.toMutableList()) }
    val selectedApps = remember { mutableStateListOf<InstalledApp>() }

    // Time intervals for the dropdown menu
    var expanded by remember { mutableStateOf(false) }
    var selectedInterval by remember { mutableStateOf("Select Interval") }  // Use mutable state to allow reassignment
    val timeIntervals = listOf("1 min", "15 min", "30 min", "45 min", "60 min", "75 min", "90 min", "120 min")

    // PIN code input field
    var pinCode by remember { mutableStateOf("") }

    // AIDL service connection
    val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            appService = IAppCommunicationService.Stub.asInterface(service)
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            appService = null
            isBound = false
        }
    }

    // Bind to the remote service when the composable enters the composition
    DisposableEffect(Unit) {
        val intent = Intent().apply {
            component = ComponentName("com.example.remoteapp", "com.example.remoteapp.AppCommunicationService")
        }
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)

        onDispose {
            context.unbindService(connection)
        }
    }

    // Function to send selected apps, interval, and PIN to the AIDL service
    fun sendSelectedAppsToAnotherApp(selectedApps: List<InstalledApp>, selectedInterval: Int, pinCode: String) {
        val packageNames = selectedApps.map { it.packageName }
        if (isBound && appService != null && pinCode.isNotEmpty()) {
            try {
                appService?.sendAppData(packageNames, selectedInterval.toString(), pinCode)
                // Reset the state after sending data
                //resetSelections()  // Use resetSelections to reset the values
            } catch (e: RemoteException) {
                e.printStackTrace()
            }
        }
    }

    // Function to convert the interval from string to an integer (remove "min")
    fun parseInterval(interval: String): Int {
        return interval.replace(" min", "").toIntOrNull() ?: 0
    }

    // Function to reset selections after sending data
    fun resetSelections() {
        selectedInterval = "Select Interval"  // Reset interval
        selectedApps.clear()  // Clear the selected apps list
        pinCode = ""  // Reset the PIN code
    }

    // User Interface for selecting apps and interval
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp) // Ensure padding around the entire UI
    ) {

        // Time Interval Picker (Fixed Dropdown Menu)
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }, // Toggle expanded state
            modifier = Modifier.fillMaxWidth() // Ensure the ExposedDropdownMenuBox fills the width
        ) {
            // TextField to show the selected interval
            TextField(
                value = selectedInterval,
                onValueChange = {},
                readOnly = true, // Prevent direct editing
                label = { Text("Select Time Interval") },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null
                    )
                },
                modifier = Modifier
                    .fillMaxWidth() // Ensure the TextField fills the width
                    .menuAnchor() // Attach dropdown to TextField
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .fillMaxWidth() // Ensure the dropdown menu fills the width
                    .heightIn(max = 200.dp) // Limit dropdown height for better UI
            ) {
                timeIntervals.forEach { interval ->
                    DropdownMenuItem(
                        text = { Text(interval) },
                        onClick = {
                            selectedInterval = interval
                            expanded = false // Close after selection
                        }
                    )
                }
            }
        }

        // List of apps with selection functionality
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(15.dp)
        ) {
            items(availableApps) { app ->
                val isSelected = selectedApps.contains(app)
                AppListItem(
                    app = app,
                    isSelected = isSelected,
                    onClick = {
                        if (isSelected) {
                            selectedApps.remove(app)
                        } else {
                            selectedApps.add(app)
                        }
                    }
                )
            }
        }

        // PIN Code Input
        TextField(
            value = pinCode,
            onValueChange = { pinCode = it },
            label = { Text("Enter PIN Code") },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
        )

        // Button to send selected apps and interval to the server app
        Button(
            onClick = {
                // Check if pinCode, selectedApps, and selectedInterval are valid
                if (pinCode.isNotEmpty() && selectedApps.isNotEmpty() && selectedInterval != "Select Interval") {
                    // Convert the interval string to an integer
                    val intervalInMinutes = parseInterval(selectedInterval)
                    sendSelectedAppsToAnotherApp(selectedApps, intervalInMinutes, pinCode)
                    Toast.makeText(context, "Data sent successfully", Toast.LENGTH_SHORT).show()
                } else {
                    // Show a message if validation fails
                    Toast.makeText(context, "Please fill all required fields", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Green)
        ) {
            Text(
                text = "Send to Another App",
                color = Color.White
            )
        }
    }
}

data class InstalledApp(
    val packageName: String,
    val name: String,
    val icon: Drawable?
)

// Fetch installed apps from the device
fun getInstalledApps(context: Context): List<InstalledApp> {
    val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
        addCategory(Intent.CATEGORY_LAUNCHER)
    }
    val pkgAppsList: List<ResolveInfo> = context.packageManager.queryIntentActivities(mainIntent, 0)

    return pkgAppsList.map { resolveInfo ->
        val packageName = resolveInfo.activityInfo.packageName
        val name = resolveInfo.loadLabel(context.packageManager).toString()
        val icon = resolveInfo.loadIcon(context.packageManager)
        InstalledApp(packageName, name, icon)
    }
}

// Function to convert Drawable to Bitmap for displaying app icons
fun Drawable.toBitmap(): Bitmap {
    val bitmap = Bitmap.createBitmap(intrinsicWidth, intrinsicHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    setBounds(0, 0, canvas.width, canvas.height)
    draw(canvas)
    return bitmap
}

// Composable for rendering each app in the list
@Composable
fun AppListItem(
    app: InstalledApp,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val iconPainter = rememberDrawablePainter(app.icon)

    val borderModifier = if (isSelected) {
        Modifier.border(2.dp, SolidColor(Color.Blue), RoundedCornerShape(8.dp))
    } else {
        Modifier
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .then(borderModifier)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = iconPainter,
                contentDescription = app.name,
                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp))
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = app.name,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 20.sp),
                modifier = Modifier
                    .padding(start = 8.dp)
                    .weight(1f),
                color = Color.Black
            )
        }
    }
}

// Remember a Drawable painter for app icons
@Composable
fun rememberDrawablePainter(drawable: Drawable?): Painter {
    return remember(drawable) {
        if (drawable != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && drawable is AdaptiveIconDrawable) {
                val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
                BitmapPainter(bitmap.asImageBitmap())
            } else {
                val bitmap = drawable.toBitmap()
                BitmapPainter(bitmap.asImageBitmap())
            }
        } else {
            BitmapPainter(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888).asImageBitmap())
        }
    }
}