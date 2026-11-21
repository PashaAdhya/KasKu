package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.auth.attemptAutoSignIn
import com.example.data.auth.onGoogleSignInClicked
import com.example.ui.theme.BrutalButton
import com.example.ui.theme.BrutalCard
import com.example.ui.theme.BrutalIconButton
import com.example.ui.theme.BrutalTagBadge
import com.example.ui.theme.LocalBrutalPalette
import com.example.ui.theme.ThemeMode

@Composable
fun AuthScreen(
    themeMode: ThemeMode,
    onToggleTheme: () -> Unit,
    onAuthSuccess: () -> Unit,
) {
    val palette = LocalBrutalPalette.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var isLoading by remember { mutableStateOf(false) }
    var isCheckingAutoLogin by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {
                isCheckingAutoLogin = false
                onAuthSuccess()
            },
            onUnauthenticated = {
                isCheckingAutoLogin = false
            },
            scope = scope,
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Top bar with Brand pill & theme switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    BrutalTagBadge(
                        text = "KASKU",
                        containerColor = palette.primaryAccent,
                        contentColor = palette.onPrimaryAccent,
                    )
                    Text(
                        text = stringResource(R.string.tagline),
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary,
                    )
                }
                BrutalIconButton(
                    onClick = onToggleTheme,
                    modifier = Modifier.testTag("auth_theme_toggle"),
                ) {
                    Icon(
                        imageVector = if (palette.isDark) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                        contentDescription = "Ganti mode terang atau gelap",
                    )
                }
            }

            // Hero Neo-Brutalist Artwork Card
            BrutalCard(
                containerColor = palette.surface,
                shadowOffset = 6.dp,
            ) {
                Image(
                    painter = painterResource(id = R.drawable.kasku_welcome_hero_1795098361180),
                    contentDescription = "Ilustrasi brankas keuangan neo-brutalism KasKu",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(2.dp, palette.border, RoundedCornerShape(8.dp)),
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Catat Uang Harian.\nTanpa Ribet.",
                    style = MaterialTheme.typography.displaySmall,
                    color = palette.textPrimary,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Kelola pemasukan, pengeluaran harian, anggaran kategori, hingga laporan bulanan lengkap dengan sinkronisasi Google lintas perangkat.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textSecondary,
                )
            }

            // Feature highlights
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                FeatureMiniCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.Bolt,
                    title = "Catat Kilat",
                    subtitle = "1 ketuk langsung masuk",
                    bgColor = palette.primaryAccent,
                    fgColor = palette.onPrimaryAccent,
                )
                FeatureMiniCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.BarChart,
                    title = "Laporan",
                    subtitle = "Grafik & kategori",
                    bgColor = palette.secondaryAccent,
                    fgColor = palette.onSecondaryAccent,
                )
                FeatureMiniCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.CloudSync,
                    title = "Sinkron",
                    subtitle = "Aman di Cloud",
                    bgColor = palette.income,
                    fgColor = palette.onIncome,
                )
            }

            if (errorMessage != null) {
                BrutalCard(
                    containerColor = palette.expense,
                    shadowOffset = 3.dp,
                ) {
                    Text(
                        text = errorMessage ?: "",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = palette.onExpense,
                    )
                }
            }

            Spacer(Modifier.weight(1f, fill = false))

            BrutalButton(
                onClick = {
                    errorMessage = null
                    isLoading = true
                    onGoogleSignInClicked(
                        context = context,
                        credentialManager = credentialManager,
                        onAuthSuccess = {
                            isLoading = false
                            onAuthSuccess()
                        },
                        onAuthError = { msg ->
                            isLoading = false
                            errorMessage = msg
                        },
                        scope = scope,
                        onAuthCancelled = {
                            isLoading = false
                        },
                    )
                },
                enabled = !isLoading && !isCheckingAutoLogin,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("google_sign_in_button"),
                containerColor = palette.primaryAccent,
                contentColor = palette.onPrimaryAccent,
                shadowOffset = 5.dp,
            ) {
                if (isLoading || isCheckingAutoLogin) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = palette.onPrimaryAccent,
                        strokeWidth = 2.5.dp,
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.signing_in),
                        style = MaterialTheme.typography.labelLarge,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.sign_in_google),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }

            Text(
                text = "Data terenkripsi & tersimpan pribadi di akun Google milikmu (${themeMode.label}).",
                style = MaterialTheme.typography.bodySmall,
                color = palette.textSecondary,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}

@Composable
private fun FeatureMiniCard(
    modifier: Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    bgColor: androidx.compose.ui.graphics.Color,
    fgColor: androidx.compose.ui.graphics.Color,
) {
    BrutalCard(
        modifier = modifier,
        containerColor = bgColor,
        shadowOffset = 3.dp,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = fgColor,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = fgColor,
            maxLines = 1,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = fgColor.copy(alpha = 0.85f),
            maxLines = 2,
        )
    }
}
