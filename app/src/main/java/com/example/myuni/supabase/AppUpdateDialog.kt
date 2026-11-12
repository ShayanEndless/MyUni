package com.example.myuni.supabase

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myuni.AppConfig

@Composable
fun ForceUpdateScreen(
    config: AppConfigRemote,
    isChecking: Boolean = false,
    onRecheckUpdate: () -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? Activity

    // غیرفعال کردن دکمه بازگشت (Back Button) گوشی یا خروج از برنامه
    BackHandler {
        activity?.finish()
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    shape = RoundedCornerShape(AppConfig.dialogCornerRadius),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // آیکون اصلی
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SystemUpdate,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        // عنوان
                        Text(
                            text = "به‌روزرسانی اجباری برنامه",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        // توضیح
                        Text(
                            text = "جهت ادامه استفاده از برنامه، باید نسخه جدید (${config.latestVersionName}) را دریافت و نصب کنید.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        )

                        // تغییرات نسخه جدید (در صورت وجود)
                        if (!config.releaseNotes.isNullOrBlank()) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = AppConfig.dialogCardContainerAlpha),
                                shape = RoundedCornerShape(AppConfig.dialogInnerCornerRadius),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = "تغییرات این نسخه:",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = config.releaseNotes,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // دکمه دانلود و به‌روزرسانی
                        Button(
                            onClick = {
                                AppUpdateHelper.openMarket(context, config.downloadUrl)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(AppConfig.dialogButtonCornerRadius)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "دانلود و به‌روزرسانی",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // دکمه بررسی مجدد
                        OutlinedButton(
                            onClick = onRecheckUpdate,
                            enabled = !isChecking,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            shape = RoundedCornerShape(AppConfig.dialogButtonCornerRadius)
                        ) {
                            if (isChecking) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "بررسی مجدد",
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppUpdateDialog(
    config: AppConfigRemote,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            shape = RoundedCornerShape(AppConfig.dialogCornerRadius),
            tonalElevation = AppConfig.dialogTonalElevation,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppConfig.dialogButtonSpacing)
                ) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "نسخه جدید موجود است! (${config.latestVersionName})",
                        fontWeight = AppConfig.dialogTitleFontWeight,
                        fontSize = AppConfig.dialogTitleFontSize
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(AppConfig.dialogButtonSpacing)
                ) {
                    if (!config.releaseNotes.isNullOrBlank()) {
                        Text(
                            text = "تغییرات نسخه جدید:",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = AppConfig.dialogSubtitleFontSize,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = AppConfig.dialogCardContainerAlpha),
                            shape = RoundedCornerShape(AppConfig.dialogInnerCornerRadius),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = config.releaseNotes ?: "",
                                fontSize = AppConfig.dialogBodyFontSize,
                                modifier = Modifier.padding(10.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Text(
                            text = "نسخه جدید برنامه آماده دانلود می‌باشد.",
                            fontSize = AppConfig.dialogSubtitleFontSize,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        AppUpdateHelper.openMarket(context, config.downloadUrl)
                        onDismissRequest()
                    },
                    shape = RoundedCornerShape(AppConfig.dialogButtonCornerRadius)
                ) {
                    Text("دانلود و به‌روزرسانی", fontSize = AppConfig.dialogButtonFontSize)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissRequest) {
                    Text("بعداً", fontSize = AppConfig.dialogButtonFontSize)
                }
            }
        )
    }
}
