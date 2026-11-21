package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.UiState
import com.example.ui.theme.BrutalButton
import com.example.ui.theme.BrutalCard
import com.example.ui.theme.BrutalChip
import com.example.ui.theme.BrutalTagBadge
import com.example.ui.theme.LocalBrutalPalette
import com.example.ui.theme.ThemeMode
import com.example.ui.viewmodel.MonthlyReportSummary
import com.example.util.buildMonthlyReportShareText
import com.example.util.formatMonthLabel
import com.google.firebase.auth.FirebaseUser

@Composable
fun ProfileSettingsScreen(
    currentUser: FirebaseUser?,
    themeMode: ThemeMode,
    reportState: UiState<MonthlyReportSummary>,
    onThemeModeChange: (ThemeMode) -> Unit,
    onSignOut: () -> Unit,
) {
    val palette = LocalBrutalPalette.current
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Text(
                    text = "PROFIL & PENGATURAN",
                    style = MaterialTheme.typography.headlineSmall,
                    color = palette.textPrimary,
                )
            }

            // 1. Google Account Card
            item {
                BrutalCard(
                    containerColor = palette.surface,
                    shadowOffset = 5.dp,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        val photoUrl = currentUser?.photoUrl?.toString()
                        val initials = (currentUser?.displayName ?: currentUser?.email ?: "K")
                            .trim()
                            .take(2)
                            .uppercase()

                        if (!photoUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = photoUrl,
                                contentDescription = "Foto profil Google",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(2.5.dp, palette.border, RoundedCornerShape(10.dp)),
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(palette.primaryAccent)
                                    .border(2.5.dp, palette.border, RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = initials,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = palette.onPrimaryAccent,
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentUser?.displayName?.takeIf { it.isNotBlank() } ?: "Pengguna KasKu",
                                style = MaterialTheme.typography.titleLarge,
                                color = palette.textPrimary,
                            )
                            Text(
                                text = currentUser?.email ?: "Akun Google terhubung",
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.textSecondary,
                            )
                            Spacer(Modifier.height(6.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                BrutalTagBadge(
                                    text = "SINKRONISASI AKTIF",
                                    containerColor = palette.income,
                                    contentColor = palette.onIncome,
                                )
                            }
                        }
                    }
                }
            }

            // 2. Dark Mode & Appearance Selector
            item {
                BrutalCard(
                    containerColor = palette.surface,
                    shadowOffset = 4.dp,
                ) {
                    Text(
                        text = "TEMA TAMPILAN (NEO-BRUTALISM)",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textPrimary,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Aktifkan Mode Gelap agar lebih nyaman di mata saat malam atau kondisi minim cahaya.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary,
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ThemeMode.entries.forEach { mode ->
                            val icon = when (mode) {
                                ThemeMode.SYSTEM -> Icons.Rounded.BrightnessAuto
                                ThemeMode.LIGHT -> Icons.Rounded.LightMode
                                ThemeMode.DARK -> Icons.Rounded.DarkMode
                            }
                            BrutalChip(
                                label = when (mode) {
                                    ThemeMode.SYSTEM -> "Sistem"
                                    ThemeMode.LIGHT -> "Terang"
                                    ThemeMode.DARK -> "Gelap"
                                },
                                selected = themeMode == mode,
                                onClick = { onThemeModeChange(mode) },
                                selectedColor = palette.primaryAccent,
                                selectedTextColor = palette.onPrimaryAccent,
                                leadingIcon = {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("theme_mode_${mode.name}"),
                            )
                        }
                    }
                }
            }

            // 3. Export Monthly Data
            item {
                val report = (reportState as? UiState.Success)?.data
                BrutalCard(
                    containerColor = palette.surface,
                    shadowOffset = 4.dp,
                ) {
                    Text(
                        text = "EKSPOR DATA BULANAN",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textPrimary,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Bagikan atau simpan rekap transaksi & kategori bulan ini dalam format ringkasan + CSV.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary,
                    )
                    Spacer(Modifier.height(12.dp))
                    BrutalButton(
                        onClick = {
                            if (report != null) {
                                val text = buildMonthlyReportShareText(
                                    monthKey = report.monthKey,
                                    totalExpense = report.totalExpense,
                                    totalIncome = report.totalIncome,
                                    categoryTotals = report.categoryBreakdown.map { it.category to it.amount },
                                    transactions = report.monthTransactions,
                                )
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "Rekap Keuangan KasKu ${formatMonthLabel(report.monthKey)}")
                                    putExtra(Intent.EXTRA_TEXT, text)
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Ekspor Laporan KasKu"))
                            }
                        },
                        enabled = report != null,
                        containerColor = palette.secondaryAccent,
                        contentColor = palette.onSecondaryAccent,
                        shadowOffset = 3.dp,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Rounded.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("BAGIKAN REKAP & CSV", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            // 4. Security & Sync Info
            item {
                BrutalCard(
                    containerColor = palette.surfaceVariant,
                    shadowOffset = 3.dp,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(Icons.Rounded.Security, contentDescription = null, tint = palette.textPrimary)
                        Column {
                            Text(
                                text = "Keamanan & Sinkronisasi Lintas Perangkat",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = palette.textPrimary,
                            )
                            Text(
                                text = "Seluruh catatan transaksi, anggaran, dan template rutin disimpan di Firestore secara privat atas UID Google kamu.",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary,
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(Icons.Rounded.CloudDone, contentDescription = null, tint = palette.income, modifier = Modifier.size(16.dp))
                        Text(
                            text = "UID: ${currentUser?.uid?.take(12) ?: "-"}…",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary,
                        )
                    }
                }
            }

            // 5. Sign Out
            item {
                BrutalButton(
                    onClick = onSignOut,
                    containerColor = palette.expense,
                    contentColor = palette.onExpense,
                    shadowOffset = 4.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sign_out_button"),
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("KELUAR DARI AKUN GOOGLE", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
