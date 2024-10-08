

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.core.graphics.drawable.toBitmap
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowAppList() {
    val context = LocalContext.current

    // List of installed apps
    val allApps = remember { getInstalledApps(context) }
    val availableApps by remember { mutableStateOf(allApps.toMutableList()) }
    val selectedApps = remember { mutableStateListOf<InstalledApp>() }

    // Time intervals for the dropdown menu
    var expanded by remember { mutableStateOf(false) }
    var selectedInterval by remember { mutableStateOf("Select Interval") }
    val timeIntervals = listOf("1 min", "15 min", "30 min", "45 min", "60 min", "75 min", "90 min", "120 min")

    // PIN code input field
    var pinCode by remember { mutableStateOf("") }

    fun sendSelectedAppsToAnotherApp(context: Context, selectedApps: List<InstalledApp>, selectedInterval: Int, pinCode: String) {
        val contentResolver = context.contentResolver

        selectedApps.forEach { app ->
            val values = ContentValues().apply {
                put("package_name", app.packageName)
                put("name", app.name)
                put("icon", app.icon?.let { drawableToByteArray(it) }) // Convert Drawable to ByteArray
                put("interval", selectedInterval.toString())
                put("pin_code", pinCode)
            }

            contentResolver.insert(
                Uri.parse("content://com.app.lockcomposeAdmin.provider/apps"), values // Insert the data
            )
        }

        Toast.makeText(context, "Data sent via ContentProvider", Toast.LENGTH_SHORT).show()
    }

    fun parseInterval(interval: String): Int {
        return interval.replace(" min", "").toIntOrNull() ?: 0
    }


    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("》Rules list") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFE0E0E0)
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Time Interval Picker (Fixed Dropdown Menu)
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                TextField(
                    value = selectedInterval,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Select Time Interval") },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                ) {
                    timeIntervals.forEach { interval ->
                        DropdownMenuItem(
                            text = { Text(interval) },
                            onClick = {
                                selectedInterval = interval
                                expanded = false
                            }
                        )
                    }
                }
            }

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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            )

            Button(
                onClick = {
                    try {
                        if (pinCode.isNotEmpty() && selectedApps.isNotEmpty() && selectedInterval != "Select Interval") {
                            val intervalInMinutes = parseInterval(selectedInterval)

                            try {
                                sendSelectedAppsToAnotherApp(context, selectedApps, intervalInMinutes, pinCode)
                                Toast.makeText(context, "Data sent successfully", Toast.LENGTH_SHORT).show()
                            } catch (e: IllegalArgumentException) {
                                Toast.makeText(context, "Failed to send data: Invalid ContentProvider URI", Toast.LENGTH_LONG).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Failed to send data: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            Toast.makeText(context, "Please fill all required fields", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Unexpected error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
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
}

// Helper function to convert Drawable to ByteArray
fun drawableToByteArray(drawable: Drawable): ByteArray {
    val bitmap = when (drawable) {
        is BitmapDrawable -> drawable.bitmap
        is AdaptiveIconDrawable -> {
            val bitmap = Bitmap.createBitmap(
                drawable.intrinsicWidth,
                drawable.intrinsicHeight,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        }
        else -> throw IllegalArgumentException("Unsupported drawable type")
    }

    val stream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
    return stream.toByteArray()
}

// InstalledApp data class
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