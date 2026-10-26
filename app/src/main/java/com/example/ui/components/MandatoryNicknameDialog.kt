package com.example.ui.components

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.CardBorderColor
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.PureWhite
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.MainViewModel

@Composable
fun MandatoryNicknameDialog(
    viewModel: MainViewModel,
    userEmail: String,
    currentName: String,
    onConfirmed: (String, String) -> Unit
) {
    val context = LocalContext.current
    val childPrefs = remember {
        context.getSharedPreferences("family_child_prefs", Context.MODE_PRIVATE)
    }

    val isDark = isSystemInDarkTheme()
    val surfaceColor = if (isDark) DarkSurface else PureWhite
    val borderColor = if (isDark) Color(0xFF38A18B) else EmeraldPrimary.copy(alpha = 0.5f)
    val textPrimaryColor = if (isDark) DarkTextPrimary else TextPrimary
    val textSecondaryColor = if (isDark) DarkTextSecondary else TextSecondary
    val chipBgColor = if (isDark) DarkSurfaceCard else EmeraldContainer.copy(alpha = 0.6f)
    val chipBorderColor = if (isDark) Color(0xFF2A3D38) else CardBorderColor

    val emailKey = remember(userEmail) { userEmail.trim().lowercase() }
    var nicknameInput by remember {
        val saved = childPrefs.getString("user_nickname_$emailKey", null)
        val initial = if (!saved.isNullOrBlank() && !saved.contains("@") && saved != "Mümin") saved else ""
        mutableStateOf(initial)
    }

    var selectedIcon by remember {
        mutableStateOf(childPrefs.getString("p1_icon", "🧔") ?: "🧔")
    }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    val iconOptions = listOf("🧔", "🧕", "👦", "👧", "👶", "👴", "👵", "🕌", "🤲")
    val roleSuggestions = listOf(
        "Oğlu" to "👦",
        "Kızı" to "👧",
        "Eşi" to "🧕",
        "Annesi" to "🧕",
        "Babası" to "🧔",
        "Kardeşi" to "🧒",
        "Torunu" to "👶",
        "Dede" to "👴",
        "Nine" to "👵"
    )

    Dialog(
        onDismissRequest = {
            // Zorunlu diyalog: Boş geçilemez veya dışarı tıklanarak kapatılamaz
        },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 20.dp),
            shape = RoundedCornerShape(24.dp),
            color = surfaceColor,
            border = BorderStroke(1.5.dp, borderColor),
            shadowElevation = 10.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Başlık Rozeti
                Surface(
                    shape = CircleShape,
                    color = EmeraldPrimary.copy(alpha = if (isDark) 0.3f else 0.15f),
                    modifier = Modifier.size(58.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Badge,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFF34D399) else EmeraldPrimary,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Aile İçi Rolünüzü / Hitabınızı Belirleyin",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimaryColor,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Giriş yapıldı: $userEmail\n\nAile havuzunda ve ibadet tablosunda Aile Reisine olan yakınlığınızın görünmesi gereklidir (Örn: Aile Reisinin Oğlu, Kızı, Eşi gibi).",
                    fontSize = 12.sp,
                    color = textSecondaryColor,
                    textAlign = TextAlign.Center,
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = if (isDark) Color(0xFF2A3D38) else Color.LightGray.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(14.dp))

                // Hızlı Rol Şablonları
                Text(
                    text = "Aile Reisine Olan Yakınlığınızı Seçin (ya da Yazın):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimaryColor,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                roleSuggestions.chunked(3).forEach { rowList ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        rowList.forEach { (role, ic) ->
                            val isSelected = nicknameInput.trim().equals(role, ignoreCase = true)
                            Surface(
                                onClick = {
                                    nicknameInput = role
                                    selectedIcon = ic
                                    errorMessage = null
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) EmeraldPrimary else chipBgColor,
                                border = BorderStroke(1.dp, if (isSelected) EmeraldPrimary else chipBorderColor),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "$ic $role",
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else textPrimaryColor,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 7.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                Spacer(modifier = Modifier.height(14.dp))

                // İkon Seçimi
                Text(
                    text = "Profil İkonunuz:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimaryColor,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    iconOptions.forEach { ic ->
                        val isSel = (selectedIcon == ic)
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(if (isSel) EmeraldPrimary.copy(alpha = 0.25f) else Color.Transparent)
                                .border(
                                    width = if (isSel) 2.dp else 1.dp,
                                    color = if (isSel) (if (isDark) Color(0xFF34D399) else EmeraldPrimary) else (if (isDark) Color(0xFF334B45) else Color.LightGray.copy(alpha = 0.4f)),
                                    shape = CircleShape
                                )
                                .clickable { selectedIcon = ic },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = ic, fontSize = 17.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Takma Ad Metin Kutusu
                OutlinedTextField(
                    value = nicknameInput,
                    onValueChange = {
                        nicknameInput = it
                        errorMessage = null
                    },
                    label = { Text("Takma Adınız / İsminiz") },
                    placeholder = { Text("Örn: Baba, Ahmet, Anne vb.") },
                    singleLine = true,
                    isError = errorMessage != null,
                    leadingIcon = {
                        Icon(Icons.Filled.Person, contentDescription = null, tint = if (isDark) Color(0xFF34D399) else EmeraldPrimary)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textPrimaryColor,
                        unfocusedTextColor = textPrimaryColor,
                        focusedBorderColor = if (isDark) Color(0xFF34D399) else EmeraldPrimary,
                        unfocusedBorderColor = if (isDark) Color(0xFF334B45) else Color.Gray.copy(alpha = 0.5f),
                        focusedLabelColor = if (isDark) Color(0xFF34D399) else EmeraldPrimary,
                        unfocusedLabelColor = textSecondaryColor
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = Color.Red,
                        fontSize = 11.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, start = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Onayla ve Kaydet Butonu
                Button(
                    onClick = {
                        val raw = nicknameInput.trim()
                        if (raw.length < 2) {
                            errorMessage = "Lütfen en az 2 harfli bir takma ad yazınız."
                            return@Button
                        }
                        if (raw.contains("@")) {
                            errorMessage = "E-posta adresi değil, lütfen kendinize özel bir isim/takma ad yazınız."
                            return@Button
                        }
                        val clean = com.example.util.FamilyRoleHelper.normalizeRoleTitle(raw)
                        val finalIcon = if (com.example.util.FamilyRoleHelper.isIconConflictingWithRole(selectedIcon, clean)) {
                            com.example.util.FamilyRoleHelper.getDefaultIconForRole(clean)
                        } else {
                            selectedIcon
                        }

                        // Save nickname to preferences and ViewModel
                        val currentEmailKey = userEmail.trim().lowercase()
                        childPrefs.edit()
                            .putBoolean("has_confirmed_nickname", true)
                            .putBoolean("confirmed_nickname_v3_$currentEmailKey", true)
                            .putString("user_nickname_$currentEmailKey", clean)
                            .putString("p1_name", clean)
                            .putString("p1_icon", finalIcon)
                            .apply()

                        viewModel.updateMyProfileName(clean)
                        viewModel.loginUser(clean, userEmail)
                        onConfirmed(clean, finalIcon)
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Takma Adı Kaydet & Devam Et",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
