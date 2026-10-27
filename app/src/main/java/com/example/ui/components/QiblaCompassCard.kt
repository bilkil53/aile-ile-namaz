package com.example.ui.components

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlin.math.abs

/**
 * Interactive Qibla Compass Card for TrackerScreen ("Bugün" sekmesi).
 * Uses device rotation / orientation sensors to determine magnetic North and points to Kaaba (Makkah).
 */
@Composable
fun QiblaCompassCard(
    qiblaBearing: Double,
    cityName: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }

    // Compass sensor azimuth reading in degrees (0..360 from true North)
    var deviceAzimuth by remember { mutableFloatStateOf(0f) }
    var hasCompassSensor by remember { mutableStateOf(true) }

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val rotationSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetometer = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        var lastAccelerometer = FloatArray(3)
        var lastMagnetometer = FloatArray(3)
        var lastAccelerometerSet = false
        var lastMagnetometerSet = false
        val rMatrix = FloatArray(9)
        val orientation = FloatArray(3)

        val sensorListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return

                if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                    SensorManager.getRotationMatrixFromVector(rMatrix, event.values)
                    SensorManager.getOrientation(rMatrix, orientation)
                    var azimuthDeg = Math.toDegrees(orientation[0].toDouble()).toFloat()
                    if (azimuthDeg < 0) azimuthDeg += 360f
                    // Low-pass filter for smooth movement
                    deviceAzimuth = (deviceAzimuth * 0.75f + azimuthDeg * 0.25f)
                } else if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    System.arraycopy(event.values, 0, lastAccelerometer, 0, event.values.size)
                    lastAccelerometerSet = true
                } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                    System.arraycopy(event.values, 0, lastMagnetometer, 0, event.values.size)
                    lastMagnetometerSet = true
                }

                if (rotationSensor == null && lastAccelerometerSet && lastMagnetometerSet) {
                    if (SensorManager.getRotationMatrix(rMatrix, null, lastAccelerometer, lastMagnetometer)) {
                        SensorManager.getOrientation(rMatrix, orientation)
                        var azimuthDeg = Math.toDegrees(orientation[0].toDouble()).toFloat()
                        if (azimuthDeg < 0) azimuthDeg += 360f
                        deviceAzimuth = (deviceAzimuth * 0.75f + azimuthDeg * 0.25f)
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (rotationSensor != null) {
            sensorManager.registerListener(sensorListener, rotationSensor, SensorManager.SENSOR_DELAY_UI)
        } else if (accelerometer != null && magnetometer != null) {
            sensorManager.registerListener(sensorListener, accelerometer, SensorManager.SENSOR_DELAY_UI)
            sensorManager.registerListener(sensorListener, magnetometer, SensorManager.SENSOR_DELAY_UI)
        } else {
            hasCompassSensor = false
        }

        onDispose {
            sensorManager?.unregisterListener(sensorListener)
        }
    }

    // Relative Qibla angle from current phone orientation: (qiblaBearing - deviceAzimuth)
    val targetAngle = ((qiblaBearing - deviceAzimuth + 360.0) % 360.0).toFloat()
    val animatedAngle by animateFloatAsState(
        targetValue = targetAngle,
        animationSpec = spring(stiffness = 300f, dampingRatio = 0.85f),
        label = "qibla_rotation"
    )

    // Check if phone is roughly aligned with Qibla (within +/- 6 degrees)
    val angleDiff = abs((qiblaBearing.toFloat() - deviceAzimuth + 540f) % 360f - 180f)
    val isAligned = angleDiff <= 7f

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = EmeraldPrimary),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("qibla_finder_card")
    ) {
        Column(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth()
        ) {
            // Header: Title, Angle badge, Collapse/Expand toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Filled.Explore,
                                contentDescription = "Kıble Pusulası",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Kıble Bulucu (Pusula)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            if (isAligned && isExpanded) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = Color.White.copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "KÂBE YÖNÜNDESİNİZ",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = "$cityName için Kıble Açısı: ${qiblaBearing.toInt()}°",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.88f)
                        )
                    }
                }

                // Expand / Collapse Action
                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Kapat" else "Aç",
                        tint = Color.White
                    )
                }
            }

            // Expanded Interactive Compass View
            if (isExpanded) {
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.2f), thickness = 0.8.dp)
                Spacer(modifier = Modifier.height(14.dp))

                // Compass Dial
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Outer decorative dial
                    Canvas(modifier = Modifier.size(190.dp)) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val radius = size.minDimension / 2f - 4.dp.toPx()

                        // Outer circle border
                        drawCircle(
                            color = if (isAligned) EmeraldPrimary.copy(alpha = 0.4f) else SoftInfoCardBorder,
                            radius = radius,
                            center = center,
                            style = Stroke(width = 2.dp.toPx())
                        )

                        // Subtle inner background
                        drawCircle(
                            color = if (isAligned) EmeraldContainer.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.4f),
                            radius = radius - 4.dp.toPx(),
                            center = center
                        )

                        // 4 Cardinal ticks (N, S, E, W)
                        for (i in 0 until 36) {
                            val isMajor = i % 9 == 0
                            val angleRad = Math.toRadians((i * 10).toDouble())
                            val tickLen = if (isMajor) 10.dp.toPx() else 5.dp.toPx()
                            val outerX = center.x + (radius - 2.dp.toPx()) * kotlin.math.cos(angleRad).toFloat()
                            val outerY = center.y + (radius - 2.dp.toPx()) * kotlin.math.sin(angleRad).toFloat()
                            val innerX = center.x + (radius - 2.dp.toPx() - tickLen) * kotlin.math.cos(angleRad).toFloat()
                            val innerY = center.y + (radius - 2.dp.toPx() - tickLen) * kotlin.math.sin(angleRad).toFloat()

                            drawLine(
                                color = if (isMajor) EmeraldPrimary else Color.Gray.copy(alpha = 0.4f),
                                start = Offset(outerX, outerY),
                                end = Offset(innerX, innerY),
                                strokeWidth = if (isMajor) 2.dp.toPx() else 1.dp.toPx()
                            )
                        }
                    }

                    // Rotating Kaaba needle
                    Box(
                        modifier = Modifier
                            .size(180.dp)
                            .rotate(animatedAngle),
                        contentAlignment = Alignment.Center
                    ) {
                        // Kaaba icon & pointer on top
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(vertical = 12.dp)
                        ) {
                            // Top: Kaaba icon & green arrow
                            Surface(
                                shape = CircleShape,
                                color = if (isAligned) EmeraldPrimary else Color(0xFF1B5E20),
                                border = BorderStroke(2.dp, Color.White),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Filled.Mosque,
                                        contentDescription = "Kâbe-i Muazzama",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // Pointer Arrow Downwards to center
                            Canvas(modifier = Modifier.size(width = 10.dp, height = 30.dp)) {
                                val path = Path().apply {
                                    moveTo(size.width / 2f, 0f)
                                    lineTo(0f, size.height)
                                    lineTo(size.width, size.height)
                                    close()
                                }
                                drawPath(
                                    path = path,
                                    color = if (isAligned) EmeraldPrimary else Color(0xFF2E7D32)
                                )
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            // Bottom counter-weight (North/South indicator)
                            Surface(
                                shape = CircleShape,
                                color = MissedRed.copy(alpha = 0.7f),
                                modifier = Modifier.size(12.dp)
                            ) {}
                        }

                        // Center Pivot pin
                        Surface(
                            shape = CircleShape,
                            color = Color.White,
                            border = BorderStroke(2.dp, EmeraldPrimary),
                            modifier = Modifier.size(14.dp)
                        ) {}
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Guidance status text & calibration tip
                Surface(
                    color = Color.White.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isAligned) Icons.Filled.CompassCalibration else Icons.Filled.Info,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (!hasCompassSensor) {
                                "Cihazınızda manyetik pusula sensörü bulunamadı. Sabit açı: ${qiblaBearing.toInt()}° (Kuzeyden saat yönünde)."
                            } else if (isAligned) {
                                "Tam hizalandınız! Telefonun baktığı yön Kâbe-i Muazzama doğrultusundadır."
                            } else {
                                "Telefonunuzu düz tutarak yeşil Kâbe simgesi tam yukarı hizalanana kadar çevirin."
                            },
                            fontSize = 11.5.sp,
                            color = Color.White,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }
    }
}
