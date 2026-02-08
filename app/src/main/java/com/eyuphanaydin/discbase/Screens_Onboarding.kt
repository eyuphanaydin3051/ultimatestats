package com.eyuphanaydin.discbase

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eyuphanaydin.discbase.ui.theme.StitchColor
import com.eyuphanaydin.discbase.ui.theme.StitchPrimary
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onFinishOnboarding: () -> Unit
) {
    // Sayfa içeriklerini R.string üzerinden tanımlıyoruz
    val pages = listOf(
        OnboardingPageData(
            titleRes = R.string.onboard_title_1,
            descRes = R.string.onboard_desc_1,
            icon = Icons.Default.Groups
        ),
        OnboardingPageData(
            titleRes = R.string.onboard_title_2,
            descRes = R.string.onboard_desc_2,
            icon = Icons.Default.EmojiEvents
        ),
        OnboardingPageData(
            titleRes = R.string.onboard_title_3,
            descRes = R.string.onboard_desc_3,
            icon = Icons.Default.BarChart
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = StitchColor.Background,
        bottomBar = {
            // Alt Kontrol Alanı (Noktalar ve Buton)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .navigationBarsPadding(), // Alt bardan kaçınmak için
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sayfa Noktaları (Indicators)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(pages.size) { iteration ->
                        val color = if (pagerState.currentPage == iteration) StitchPrimary else Color.LightGray.copy(alpha = 0.5f)
                        val size = if (pagerState.currentPage == iteration) 24.dp else 10.dp // Aktif olan uzun olsun

                        Box(
                            modifier = Modifier
                                .height(10.dp)
                                .width(size)
                                .clip(RoundedCornerShape(50))
                                .background(color)
                        )
                    }
                }

                // İleri / Başla Butonu
                Button(
                    onClick = {
                        scope.launch {
                            if (pagerState.currentPage < pages.size - 1) {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            } else {
                                onFinishOnboarding()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StitchPrimary),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    if (pagerState.currentPage < pages.size - 1) {
                        Text(stringResource(R.string.onboard_btn_next))
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(18.dp))
                    } else {
                        Text(stringResource(R.string.onboard_btn_start), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { pageIndex ->
                OnboardingPageContent(page = pages[pageIndex])
            }
        }
    }
}

@Composable
fun OnboardingPageContent(page: OnboardingPageData) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // İkon Alanı (Modern Daire İçinde)
        Box(
            modifier = Modifier
                .size(240.dp)
                .clip(CircleShape)
                .background(StitchPrimary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .clip(CircleShape)
                    .background(StitchPrimary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = page.icon,
                    contentDescription = null,
                    modifier = Modifier.size(100.dp),
                    tint = StitchPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(56.dp))

        // Başlık
        Text(
            text = stringResource(page.titleRes),
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = StitchColor.TextPrimary,
            textAlign = TextAlign.Center,
            lineHeight = 40.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Açıklama
        Text(
            text = stringResource(page.descRes),
            fontSize = 18.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            lineHeight = 28.sp
        )
    }
}

// Veri Sınıfı
data class OnboardingPageData(
    val titleRes: Int,
    val descRes: Int,
    val icon: ImageVector
)
@androidx.compose.ui.tooling.preview.Preview(showBackground = true, locale = "tr")
@Composable
fun OnboardingPreview() {
    // Burada ekranın sahte bir versiyonunu oluşturuyoruz
    OnboardingScreen(onFinishOnboarding = {})
}