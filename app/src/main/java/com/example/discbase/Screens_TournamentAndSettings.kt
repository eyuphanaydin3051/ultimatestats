package com.example.discbase

import android.app.DatePickerDialog
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.example.discbase.ui.theme.*
import kotlinx.coroutines.launch
import java.util.*

// ==========================================
// 1. GİRİŞ VE BAŞLANGIÇ EKRANLARI
// ==========================================

@Composable
fun LoadingScreen(text: String = "Yükleniyor...") {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(16.dp))
            Text(text, fontSize = 18.sp, color = Color.Gray)
        }
    }
}
@Composable
fun SignInScreen(
    signInViewModel: SignInViewModel = viewModel()
) {
    val context = LocalContext.current
    val signInState by signInViewModel.signInState.collectAsState()

    // Google'ın giriş ekranını açmak için bir launcher
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        signInViewModel.handleGoogleSignInResult(result, context)
    }

    // Hata veya başarı mesajlarını Toast olarak göster
    // DİKKAT: 'Success' durumundaki navigasyon buradan kaldırıldı.
    // Navigasyon artık UltimateStatsApp() içindeki ana NavHost tarafından yönetiliyor.
    LaunchedEffect(signInState) {
        when (val state = signInState) {
            is SignInState.Success -> {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                // Navigasyon artık buradan yapılmıyor.
            }

            is SignInState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
            }

            else -> {}
        }
    }

    // Arayüz
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- LOGO DÜZELTMESİ BURADA ---
        // 'res/drawable/ic_launcher_playstore.png' dosyasını kullandığınızı varsayarak
        Image(
            painter = painterResource(id = R.drawable.ic_launcher_playstore),
            contentDescription = "Uygulama Logosu",
            modifier = Modifier.clip(CircleShape).size(150.dp)
        )
        // --- BİTTİ ---

        Spacer(Modifier.height(24.dp))
        Text(
            "DiscBase",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Takım istatistiklerinizi görmek ve paylaşmak için giriş yapın.", // Metin güncellendi
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            color = Color.Gray
        )
        Spacer(Modifier.height(48.dp))

        Button(
            onClick = {
                val client = signInViewModel.getGoogleSignInClient(context)
                googleSignInLauncher.launch(client.signInIntent)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            enabled = signInState !is SignInState.Loading
        ) {
            if (signInState is SignInState.Loading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
            } else {
                Text("Google ile Giriş Yap", fontSize = 16.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamSelectionScreen(
    userTeamsList: List<TeamProfile>,
    onTeamSelected: (String) -> Unit,
    onSignOut: () -> Unit,
    viewModel: MainViewModel = viewModel()// <-- EKLENDİ: İşlemler için gerekli

) {
    // --- STATE TANIMLARI ---
    var showCreateDialog by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) }

    // Dialog Input State'leri
    var newTeamName by remember { mutableStateOf("") }
    var joinTeamId by remember { mutableStateOf("") }

    val context = LocalContext.current

    // --- YENİ TAKIM OLUŞTURMA DIALOGU ---
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Yeni Takım Oluştur") },
            text = {
                Column {
                    Text("Takımınızın adını girin:", fontSize = 14.sp, color = Color.Gray)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newTeamName,
                        onValueChange = { newTeamName = it },
                        label = { Text("Takım Adı") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTeamName.isNotBlank()) {
                            viewModel.createNewTeam(newTeamName)
                            showCreateDialog = false
                            newTeamName = "" // Temizle
                        } else {
                            Toast.makeText(context, "İsim boş olamaz", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) { Text("Oluştur") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("İptal") }
            }
        )
    }

    // --- TAKIMA KATILMA DIALOGU ---
    if (showJoinDialog) {
        AlertDialog(
            onDismissRequest = { showJoinDialog = false },
            title = { Text("Takıma Katıl") },
            text = {
                Column {
                    Text("Davet kodunu girin:", fontSize = 14.sp, color = Color.Gray)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = joinTeamId,
                        onValueChange = { joinTeamId = it },
                        label = { Text("Takım Kodu (ID)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (joinTeamId.isNotBlank()) {
                            viewModel.joinExistingTeam(joinTeamId)
                            showJoinDialog = false
                            joinTeamId = "" // Temizle
                        } else {
                            Toast.makeText(context, "Kod girmelisiniz", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) { Text("Katıl") }
            },
            dismissButton = {
                TextButton(onClick = { showJoinDialog = false }) { Text("İptal") }
            }
        )
    }

    // --- EKRAN TASARIMI ---
    Scaffold(
        containerColor = StitchColor.Background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Takımlarım", fontWeight = FontWeight.Bold) },
                actions = {
                    // Çıkış butonu (Opsiyonel, profil sayfasına taşımıştık ama burada da kalabilir)
                    IconButton(onClick = onSignOut) {
                        Icon(Icons.AutoMirrored.Filled.Logout, null, tint = com.example.discbase.ui.theme.StitchDefense)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ExtendedFloatingActionButton(
                    onClick = { showJoinDialog = true },
                    containerColor = StitchSecondary,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Default.GroupAdd, null) },
                    text = { Text("Katıl") }
                )
                ExtendedFloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = StitchColor.Primary,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Default.Add, null) },
                    text = { Text("Yeni Takım") }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            if (userTeamsList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Groups, null, tint = Color.LightGray, modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("Henüz bir takımınız yok.", color = Color.Gray)
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(userTeamsList) { teamProfile ->
                        // --- TAKIM KARTI ---
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = StitchColor.Surface),
                            elevation = CardDefaults.cardElevation(4.dp),
                            onClick = { onTeamSelected(teamProfile.teamId) }
                        ) {
                            Row(
                                modifier = Modifier.padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Logo Gösterimi
                                if (teamProfile.logoPath != null) {
                                    AsyncImage(
                                        model = getLogoModel(teamProfile.logoPath), contentDescription = null,
                                        modifier = Modifier.size(64.dp).clip(CircleShape).border(2.dp, color = StitchColor.TextPrimary, CircleShape)
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.Groups, null,
                                        modifier = Modifier.size(64.dp).background(StitchBackground, CircleShape).padding(12.dp),
                                        tint = StitchColor.Primary
                                    )
                                }

                                Spacer(Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(teamProfile.teamName, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = StitchColor.TextPrimary)
                                    Text("Kod: ${teamProfile.teamId}", fontSize = 12.sp, color = Color.Gray)
                                }

                                Icon(Icons.Default.ChevronRight, null, tint = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateProfileScreen(
    mainViewModel: MainViewModel = viewModel(),
    onProfileCreated: () -> Unit // Navigasyon için callback
) {
    var displayName by remember { mutableStateOf("") }
    val context = LocalContext.current
    val profileState by mainViewModel.profileState.collectAsState()

    // Eğer viewModel profili EXISTS olarak güncellerse (kayıt başarılıysa),
    // otomatik olarak bir sonraki ekrana git.
    LaunchedEffect(profileState) {
        if (profileState == MainViewModel.UserProfileState.EXISTS) {
            onProfileCreated()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Profil Oluştur") })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Hoş Geldiniz!",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Lütfen uygulamada diğer üyelerin göreceği adınızı ve soyadınızı girin.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = Color.Gray
            )
            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text("Adınız Soyadınız") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
            )
            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    if (displayName.isBlank() || displayName.length < 3) {
                        Toast.makeText(context, "Lütfen geçerli bir ad girin.", Toast.LENGTH_SHORT).show()
                    } else {
                        mainViewModel.saveManualProfile(displayName)
                    }
                },
                // İsim boşsa veya hala yükleniyorsa butonu kilitle
                enabled = displayName.isNotBlank() && profileState != MainViewModel.UserProfileState.LOADING,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                if (profileState == MainViewModel.UserProfileState.LOADING) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Text("Kaydet ve Devam Et", fontSize = 16.sp)
                }
            }
        }
    }
}

// ==========================================
// 2. PROFİL VE AYARLAR
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditScreen(
    navController: NavController,
    onSignOut: () -> Unit,
    onChangeTeam: () -> Unit,
    allUserProfiles: Map<String, UserProfile>,
    allPlayers: List<Player>,
    allTournaments: List<Tournament>
) {
    // --- 1. TEMEL TANIMLAMALAR VE STATE'LER ---
    val context = LocalContext.current
    val mainViewModel: MainViewModel = viewModel() // ViewModel'e buradan erişiyoruz
    val scope = rememberCoroutineScope()

    // ViewModel'den verileri çekiyoruz (Hata alan kısımlar burasıydı)
    val currentProfile by mainViewModel.profile.collectAsState()
    val currentUserRole by mainViewModel.currentUserRole.collectAsState()
    val isAdmin = currentUserRole == "admin"

    // Düzenleme State'leri
    var teamName by remember { mutableStateOf(currentProfile.teamName) }
    var tempLogoUri by remember { mutableStateOf<Uri?>(null) } // Yeni seçilen logo
    var isSaving by remember { mutableStateOf(false) }

    // --- 2. RESİM SEÇME VE KIRPMA MANTIĞI ---

    // Kırpma sonucu buraya düşer
    val cropImageLauncher = rememberLauncherForActivityResult(contract = CropImageContract()) { result ->
        if (result.isSuccessful) {
            tempLogoUri = result.uriContent
        } else {
            val exception = result.error
            Toast.makeText(context, "Kırpma iptal edildi: ${exception?.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Galeri açıcı
    // ProfileEditScreen içindeki photoPickerLauncher kısmı:

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                val cropOptions = CropImageContractOptions(
                    uri,
                    CropImageOptions(
                        imageSourceIncludeGallery = false,
                        imageSourceIncludeCamera = false,
                        cropShape = CropImageView.CropShape.OVAL,
                        fixAspectRatio = true,
                        aspectRatioX = 1,
                        aspectRatioY = 1,
                        outputCompressFormat = Bitmap.CompressFormat.JPEG,
                        outputCompressQuality = 80,
                        // --- BU SATIRLARI EKLEYİN ---
                        activityTitle = "Fotoğrafı Kırp", // Üstte yazacak başlık
                        cropMenuCropButtonTitle = "Tamam", // Onay butonu yazısı (bazen ikon olur)
                        // ----------------------------
                    )
                )
                cropImageLauncher.launch(cropOptions)
            }
        }
    )

    // --- 3. EKRAN TASARIMI ---
    Scaffold(
        containerColor = StitchColor.Background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Takım Ayarları", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    ModernIconButton(
                        icon = Icons.Default.ArrowBack,
                        onClick = { navController.popBackStack() },
                        color = StitchColor.TextPrimary,
                        contentDescription = "Geri"
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- KART 1: TAKIM PROFİLİ (LOGO & İSİM) ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = StitchColor.Surface),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Logo Alanı
                    Box(contentAlignment = Alignment.BottomEnd) {
                        // Geçici logo varsa onu, yoksa kayıtlıyı göster
                        val logoToShow: Any? = tempLogoUri ?: getLogoModel(currentProfile.logoPath)

                        if (logoToShow != null) {
                            AsyncImage(
                                model = logoToShow,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(CircleShape)
                                    .border(2.dp,
                                        com.example.discbase.ui.theme.StitchPrimary, CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                Icons.Default.Groups, null,
                                modifier = Modifier
                                    .size(100.dp)
                                    .background(StitchBackground, CircleShape)
                                    .padding(20.dp),
                                tint = StitchColor.Primary
                            )
                        }

                        // Düzenleme Butonu (Sadece Admin)
                        if (isAdmin) {
                            Surface(
                                onClick = {
                                    // Galeriyi sadece resimler için aç
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                shape = CircleShape,
                                color = com.example.discbase.ui.theme.StitchPrimary,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    null,
                                    tint = Color.White,
                                    modifier = Modifier.padding(6.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Takım Adı
                    OutlinedTextField(
                        value = teamName,
                        onValueChange = { teamName = it },
                        label = { Text("Takım Adı") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        enabled = isAdmin,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = com.example.discbase.ui.theme.StitchPrimary,
                            focusedLabelColor = com.example.discbase.ui.theme.StitchPrimary
                        )
                    )

                    Spacer(Modifier.height(12.dp))

                    // Davet Kodu (Salt Okunur & Kopyalanabilir)
                    OutlinedTextField(
                        value = currentProfile.teamId,
                        onValueChange = {},
                        label = { Text("Davet Kodu (Kopyala)") },
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = {
                            IconButton(onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("Takım Kodu", currentProfile.teamId)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Kopyalandı!", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Default.ContentCopy, null, tint = StitchColor.Primary)
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = StitchColor.Background,
                            unfocusedContainerColor = StitchColor.Background
                        )
                    )
                }
            }

            // --- KART 2: ÜYE YÖNETİMİ (Sadece Admin Görür) ---
            if (isAdmin) {
                val currentUserId = mainViewModel.currentUser.collectAsState().value?.uid ?: ""

                MemberManagementCard(
                    members = currentProfile.members,
                    userProfiles = allUserProfiles,
                    currentUserId = currentUserId,
                    onUpdateRole = { uid, newRole -> mainViewModel.updateMemberRole(uid, newRole) },
                    onRemoveMember = { uid -> mainViewModel.removeMember(uid) }
                )
            }

            // --- KART 3: İŞLEM BUTONLARI ---
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Takım Değiştir
                OutlinedButton(
                    onClick = onChangeTeam,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, com.example.discbase.ui.theme.StitchPrimary),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = com.example.discbase.ui.theme.StitchPrimary)
                ) { Text("Takım Değiştir") }

                // Çıkış Yap
                Button(
                    onClick = onSignOut,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = com.example.discbase.ui.theme.StitchDefense) // Kırmızı
                ) { Text("Çıkış Yap") }
            }

            // --- KAYDET BUTONU (Admin ve değişiklik varsa) ---
            // Hem isim değişmişse hem de yeni logo seçilmişse aktif olur
            val hasChanges = (teamName != currentProfile.teamName) || (tempLogoUri != null)

            if (isAdmin && hasChanges) {
                Button(
                    onClick = {
                        scope.launch {
                            isSaving = true

                            // Logoyu Base64 string'e çevir (Varsa)
                            var base64Logo: String? = null
                            if (tempLogoUri != null) {
                                // IO Thread'de işlemi yap
                                base64Logo = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                    uriToCompressedBase64(context, tempLogoUri!!)
                                }
                            }

                            // ViewModel üzerinden kaydet
                            mainViewModel.saveProfile(teamName, base64Logo) {
                                // Başarılı olursa
                                isSaving = false
                                navController.popBackStack()
                                Toast.makeText(context, "Profil güncellendi", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = !isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StitchColor.Primary)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("DEĞİŞİKLİKLERİ KAYDET", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: MainViewModel,
    allPlayers: List<Player>,      // <-- YENİ PARAMETRE
    tournaments: List<Tournament>  // <-- YENİ PARAMETRE
) {
    val context = LocalContext.current

    // State'leri Dinle
    val currentNameFormat by viewModel.nameFormat.collectAsState()
    val keepScreenOn by viewModel.keepScreenOn.collectAsState()
    val vibrationEnabled by viewModel.vibrationEnabled.collectAsState()
    val isLeftHanded by viewModel.isLeftHanded.collectAsState()
    val currentCaptureMode by viewModel.captureMode.collectAsState()
    val isTimeTrackingEnabled by viewModel.timeTrackingEnabled.collectAsState() // <-- BUNU ÇEKİN
    val isProModeEnabled by viewModel.proModeEnabled.collectAsState()

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            // HATALI OLAN ESKİ SATIR:
            // viewModel.importBackupFromJson(context, uri)

            // DOĞRU OLAN YENİ SATIR (Context parametresini sildik):
            viewModel.importBackupFromJson(uri)
        }
    }

    // Örnek İsim Formatı
    val sampleName = "Eyüphan Aydın"
    val formattedSample = when (currentNameFormat) {
        NameFormat.FULL_NAME -> "Eyüphan Aydın"
        NameFormat.FIRST_NAME_LAST_INITIAL -> "Eyüphan A."
        NameFormat.INITIAL_LAST_NAME -> "E. Aydın"
    }
    // --- YENİ EKLENEN KISIM BAŞLANGICI ---
    // Dosya Kaydetme Penceresini Açan Launcher
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        // Kullanıcı bir yer seçip "Kaydet"e bastığında burası çalışır
        if (uri != null) {
            viewModel.saveBackupToUri(uri, allPlayers, tournaments)
        }
    }
    // --- YENİ EKLENEN KISIM BİTİŞİ ---

    Scaffold(
        containerColor = StitchColor.Background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Ayarlar", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    ModernIconButton(
                        Icons.Default.ArrowBack,
                        { navController.popBackStack() },
                        StitchTextPrimary,
                        "Geri"
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // --- 1. VERİ KAYIT MODU (YENİ BÖLÜM - EN ÜSTE EKLİYORUZ) ---
            SettingsSection(title = "MAÇ KAYIT MODU") {
                // BASİT MOD
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setCaptureMode(CaptureMode.SIMPLE) }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = currentCaptureMode == CaptureMode.SIMPLE,
                        onClick = null,
                        colors = RadioButtonDefaults.colors(selectedColor = StitchColor.Primary)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("Basit Mod", fontWeight = FontWeight.Bold)
                        Text("Sadece Line, Skor, Gol ve Asist takibi.", fontSize = 12.sp, color = Color.Gray)
                    }
                }

                Divider(color = Color.LightGray.copy(0.2f), modifier = Modifier.padding(horizontal = 16.dp))

                // GELİŞMİŞ MOD
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setCaptureMode(CaptureMode.ADVANCED) }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = currentCaptureMode == CaptureMode.ADVANCED,
                        onClick = null,
                        colors = RadioButtonDefaults.colors(selectedColor = StitchColor.Primary)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("Gelişmiş Mod (Önerilen)", fontWeight = FontWeight.Bold)
                        Text("Pas, Hata, Blok ve detaylı istatistikler.", fontSize = 12.sp, color = Color.Gray)
                    }
                }
// --- YENİ: PRO MOD ŞALTERİ (Süre Yönetimi'nin Üstüne) ---
                SettingsSwitchRow(
                    icon = Icons.Default.Map, // Harita İkonu
                    title = "Pro Mod",
                    subtitle = "Saha haritası ve ekstra analiz sekmesini aktifleştirir.",
                    checked = isProModeEnabled,
                    onCheckedChange = { viewModel.setProModeEnabled(it) }
                )

                Divider(color = Color.LightGray.copy(0.2f)) // Araya çizgi

                // --- YENİ EKLENEN SÜRE BUTONU ---
                SettingsSwitchRow(
                    icon = Icons.Default.AccessTime, // Saat İkonu
                    title = "Süre Yönetimi (Pro)",
                    subtitle = "Maç kronometresi, mola takibi ve pull süresi ölçümü.",
                    checked = isTimeTrackingEnabled,
                    onCheckedChange = { viewModel.setTimeTrackingEnabled(it) }
                )
            }

            // --- 1. GENEL AYARLAR ---
            SettingsSection(title = "GENEL") {
                SettingsRow(
                    icon = Icons.Default.Language,
                    title = "Dil / Language",
                    subtitle = "Türkçe (Varsayılan)",
                    onClick = { /* Dil dialogu eklenebilir */ }
                )
                Divider(color = Color.LightGray.copy(0.2f))
                SettingsSwitchRow(
                    icon = Icons.Default.Smartphone,
                    title = "Ekranı Açık Tut",
                    subtitle = "Maç kaydı sırasında ekran kapanmaz.",
                    checked = keepScreenOn,
                    onCheckedChange = { viewModel.setKeepScreenOn(it) }
                )
            }

            // --- 2. GÖRÜNÜM ---
            SettingsSection(title = "GÖRÜNÜM & FORMAT") {
                Text(
                    "Oyuncu İsim Formatı",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                Surface(
                    color = com.example.discbase.ui.theme.StitchPrimary.copy(0.05f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("Önizleme: ", color = Color.Gray, fontSize = 12.sp)
                        Text(
                            formattedSample,
                            fontWeight = FontWeight.Bold,
                            color = com.example.discbase.ui.theme.StitchPrimary,
                            fontSize = 14.sp
                        )
                    }
                }
                NameFormatOption(
                    "İsim + Soyadı Kısaltma",
                    currentNameFormat == NameFormat.FIRST_NAME_LAST_INITIAL
                ) { viewModel.updateNameFormat(NameFormat.FIRST_NAME_LAST_INITIAL) }
                Divider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = Color.LightGray.copy(0.2f)
                )
                NameFormatOption(
                    "Baş Harf + Soyadı",
                    currentNameFormat == NameFormat.INITIAL_LAST_NAME
                ) { viewModel.updateNameFormat(NameFormat.INITIAL_LAST_NAME) }
                Divider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = Color.LightGray.copy(0.2f)
                )
                NameFormatOption(
                    "Tam İsim",
                    currentNameFormat == NameFormat.FULL_NAME
                ) { viewModel.updateNameFormat(NameFormat.FULL_NAME) }
            }
            // --- TEMA AYARLARI (YENİ BÖLÜM) ---
            val currentTheme by viewModel.appTheme.collectAsState()

            SettingsSection(title = "TEMA") {
                Column(Modifier.padding(vertical = 8.dp)) {
                    // Açık Tema Seçeneği
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.updateTheme(AppTheme.LIGHT) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentTheme == AppTheme.LIGHT,
                            onClick = null,
                            colors = RadioButtonDefaults.colors(selectedColor = com.example.discbase.ui.theme.StitchPrimary)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Açık Tema (Varsayılan)")
                    }

                    Divider(
                        color = Color.LightGray.copy(0.2f),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    // Koyu Tema Seçeneği
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.updateTheme(AppTheme.DARK) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentTheme == AppTheme.DARK,
                            onClick = null,
                            colors = RadioButtonDefaults.colors(selectedColor = com.example.discbase.ui.theme.StitchPrimary)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Koyu Tema")
                    }

                    Divider(
                        color = Color.LightGray.copy(0.2f),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    // Sistem Teması Seçeneği
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.updateTheme(AppTheme.SYSTEM) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentTheme == AppTheme.SYSTEM,
                            onClick = null,
                            colors = RadioButtonDefaults.colors(selectedColor = com.example.discbase.ui.theme.StitchPrimary)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Sistem Teması")
                    }
                }
            }
            // --- 3. KULLANIM ---
            SettingsSection(title = "KULLANIM KOLAYLIĞI") {
                SettingsSwitchRow(
                    icon = Icons.Default.Vibration,
                    title = "Titreşim Geri Bildirimi",
                    subtitle = "Butonlara basınca telefon titrer.",
                    checked = vibrationEnabled,
                    onCheckedChange = { viewModel.setVibrationEnabled(it) }
                )
                Divider(color = Color.LightGray.copy(0.2f))

                // --- DÜZELTME: Solak Modu Aktif Edildi ---
                SettingsSwitchRow(
                    icon = Icons.Default.PanTool,
                    title = "Solak Modu",
                    subtitle = "Buton yerleşimini ters çevirir.",
                    checked = isLeftHanded,
                    onCheckedChange = { viewModel.setLeftHanded(it) },
                    enabled = true // ARTIK AKTİF
                )
            }

            // --- 4. VERİ YÖNETİMİ (GÜNCELLENDİ) ---
            SettingsSection(title = "VERİ YÖNETİMİ") {
                // YEDEKLEME BUTONU (GÜNCELLENDİ)
                SettingsActionRow(
                    icon = Icons.Default.Save,
                    text = "Verileri Yedekle (JSON)",
                    onClick = {
                        // Dosya adını ve tarihini oluştur
                        val date = java.text.SimpleDateFormat("yyyyMMdd_HHmm", java.util.Locale.getDefault()).format(java.util.Date())
                        val fileName = "DiscBase_Yedek_$date.json"

                        // Sistem penceresini aç (Dosya adını önerir)
                        exportLauncher.launch(fileName)
                    }
                )
                Divider(color = Color.LightGray.copy(0.2f))

                // GERİ YÜKLEME BUTONU
                SettingsActionRow(
                    icon = Icons.Default.Restore,
                    text = "Yedekten Geri Yükle",
                    onClick = {
                        // Dosya seçiciyi aç (Sadece JSON)
                        importLauncher.launch(arrayOf("application/json"))
                    }
                )
                Divider(color = Color.LightGray.copy(0.2f))

                // SIFIRLAMA
                SettingsActionRow(
                    icon = Icons.Default.DeleteForever,
                    text = "Tüm Verileri Sıfırla",
                    color = com.example.discbase.ui.theme.StitchDefense,
                    onClick = {
                        // Buraya bir AlertDialog ekleyerek mainViewModel.resetAllData() çağırabilirsiniz.
                        Toast.makeText(
                            context,
                            "Verileri silmek için ViewModel fonksiyonu eklenmeli.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }

            // Alt Bilgi
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("DiscBase", fontWeight = FontWeight.Bold, color = Color.Gray)
                Text("Versiyon 1.2.2 • Build 2025", fontSize = 12.sp, color = Color.LightGray)
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
// ==========================================
// 3. TURNUVA EKRANLARI
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TournamentListScreen(
    navController: NavController,
    tournaments: List<Tournament>,
    onAddTournament: () -> Unit,
    isAdmin: Boolean
) {
    Scaffold(
        containerColor = StitchColor.Background,
        floatingActionButton = {
            if (isAdmin) {
                // --- GÜNCELLENEN BUTON ---
                ExtendedFloatingActionButton(
                    onClick = onAddTournament,
                    containerColor = StitchColor.Primary,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("YENİ TURNUVA", fontWeight = FontWeight.Bold) }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            if (tournaments.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.EmojiEvents, null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Henüz bir turnuva oluşturmadınız.",
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 80.dp), // FAB altında kalmasın
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(tournaments) { tournament ->
                        TournamentCard(
                            tournament = tournament,
                            onClick = {
                                navController.navigate("tournament_detail/${tournament.id}")
                            }
                        )
                    }
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TournamentSetupScreen(
    onTournamentSave: (Tournament) -> Unit,
    navController: NavController,
    allPlayers: List<Player>,
    currentTeamName: String,
    tournamentToEdit: Tournament?
) {
    var tournamentName by remember { mutableStateOf(tournamentToEdit?.tournamentName ?: "") }
    var tournamentDate by remember { mutableStateOf(tournamentToEdit?.date ?: "") }
    var selectedPlayerIds by remember { mutableStateOf(tournamentToEdit?.rosterPlayerIds?.toSet() ?: emptySet()) }

    val canSave = tournamentName.isNotBlank() && selectedPlayerIds.size >= 7
    val isEditing = tournamentToEdit != null

    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    // Tarih Seçici
    val datePickerDialog = DatePickerDialog(
        context,
        { _, selectedYear, selectedMonth, selectedDay ->
            tournamentDate = "$selectedDay/${selectedMonth + 1}/$selectedYear"
        },
        calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)
    )

    Scaffold(
        containerColor = StitchColor.Background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (isEditing) "Turnuva Düzenle" else "Yeni Turnuva", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    ModernIconButton(Icons.Default.ArrowBack, { navController.popBackStack() }, StitchTextPrimary, "Geri")
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. KART: GENEL BİLGİLER
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = StitchColor.Surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.EmojiEvents, null, tint = StitchColor.Primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Turnuva Detayları", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = StitchColor.TextPrimary)
                    }

                    OutlinedTextField(
                        value = tournamentName,
                        onValueChange = { tournamentName = it },
                        label = { Text("Turnuva Adı") },
                        placeholder = { Text("Örn: XI. ODTU UFT") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = com.example.discbase.ui.theme.StitchPrimary,
                            focusedLabelColor = com.example.discbase.ui.theme.StitchPrimary
                        )
                    )

                    // Tarih Kutusu (Tıklanabilir)
                    OutlinedTextField(
                        value = tournamentDate,
                        onValueChange = {},
                        label = { Text("Tarih") },
                        placeholder = { Text("Seçmek için tıklayın") },
                        readOnly = true,
                        enabled = false, // Tıklamayı Box yönetecek
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { datePickerDialog.show() },
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = { Icon(Icons.Default.Event, null, tint = StitchColor.Primary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = StitchColor.TextPrimary,
                            disabledBorderColor = Color.Gray,
                            disabledLabelColor = Color.Gray,
                            disabledTrailingIconColor = com.example.discbase.ui.theme.StitchPrimary
                        )
                    )
                }
            }

            // 2. KART: KADRO SEÇİMİ
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = StitchColor.Surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(Modifier.padding(top = 16.dp)) {
                    Text(
                        "Turnuva Kadrosu (${selectedPlayerIds.size})",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = StitchColor.TextPrimary,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )

                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(allPlayers.sortedBy { it.name }) { player ->
                            val isSelected = selectedPlayerIds.contains(player.id)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) com.example.discbase.ui.theme.StitchPrimary.copy(0.1f) else Color.Transparent)
                                    .clickable {
                                        selectedPlayerIds = if (isSelected) selectedPlayerIds - player.id
                                        else selectedPlayerIds + player.id
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                PlayerAvatar(name = player.name, jerseyNumber = player.jerseyNumber, size = 40.dp)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(player.name, fontWeight = FontWeight.Bold, color = StitchColor.TextPrimary)
                                    Text(player.position, fontSize = 12.sp, color = Color.Gray)
                                }
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = null,
                                    colors = CheckboxDefaults.colors(checkedColor = com.example.discbase.ui.theme.StitchPrimary)
                                )
                            }
                        }
                    }
                }
            }

            // KAYDET BUTONU
            Button(
                onClick = {
                    val tournamentData = Tournament(
                        id = tournamentToEdit?.id ?: UUID.randomUUID().toString(),
                        tournamentName = tournamentName,
                        ourTeamName = currentTeamName,
                        date = tournamentDate.ifBlank { "Tarih Girilmedi" },
                        rosterPlayerIds = selectedPlayerIds.toList(),
                        matches = tournamentToEdit?.matches ?: emptyList(),
                        presetLines = tournamentToEdit?.presetLines ?: emptyList()
                    )
                    onTournamentSave(tournamentData)
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = StitchColor.Primary)
            ) {
                Text(if (isEditing) "GÜNCELLE" else "KAYDET", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TournamentDetailScreen(
    navController: NavController,
    tournament: Tournament,
    allPlayers: List<Player>,
    onStartNewMatch: () -> Unit,
    onDeleteTournament: () -> Unit,
    isAdmin: Boolean
) {

    var showDeleteDialog by remember { mutableStateOf(false) }
    val standardMatches = remember(tournament.matches) {
        tournament.matches.filter { it.isProMode == false }
            .sortedByDescending { /* İstersen tarihe göre sırala */ it.id }
    }
    // Turnuva Karnesi
    val wins = standardMatches.count { it.scoreUs > it.scoreThem }
    val losses = standardMatches.count { it.scoreThem > it.scoreUs }
    val recordText = "$wins Galibiyet - $losses Mağlubiyet"

    // Silme Dialogu (Aynı)
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Turnuvayı Sil") },
            text = { Text("Bu turnuva ve tüm maçları silinecek. Emin misiniz?") },
            confirmButton = {
                Button(
                    onClick = { onDeleteTournament(); showDeleteDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Evet, Sil") }
            },
            dismissButton = { Button(onClick = { showDeleteDialog = false }) { Text("İptal") } }
        )
    }

    Scaffold(
        containerColor = StitchColor.Background,
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    ModernIconButton(
                        icon = Icons.Default.ArrowBack,
                        onClick = { navController.popBackStack() },
                        color = StitchColor.TextPrimary,
                        contentDescription = "Geri"
                    )
                },
                actions = {
                    val context = LocalContext.current
                    val vm: MainViewModel = androidx.lifecycle.viewmodel.compose.viewModel()

                    IconButton(onClick = {
                        vm.shareTournamentReport(context, tournament, allPlayers)
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Turnuva Raporu", tint = StitchColor.Primary)
                    }

                    // ---------------------------
                    if (isAdmin) {
                        Row(modifier = Modifier.padding(end = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ModernIconButton(
                                icon = Icons.Default.Edit,
                                onClick = { navController.navigate("tournament_setup?tournamentId=${tournament.id}") },
                                color = com.example.discbase.ui.theme.StitchPrimary,
                                contentDescription = "Düzenle"
                            )
                            ModernIconButton(
                                icon = Icons.Default.Delete,
                                onClick = { showDeleteDialog = true },
                                color = com.example.discbase.ui.theme.StitchDefense,
                                contentDescription = "Sil"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            if (isAdmin) {
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    FloatingActionButton(
                        onClick = { navController.navigate("line_setup/${tournament.id}") },
                        containerColor = StitchSecondary,
                        contentColor = Color.White,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Groups, "Line")
                    }

                    ExtendedFloatingActionButton(
                        onClick = onStartNewMatch,
                        containerColor = StitchColor.Primary,
                        contentColor = Color.White,
                        icon = { Icon(Icons.Default.Add, null) },
                        text = { Text("YENİ MAÇ", fontWeight = FontWeight.Bold) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            // 1. MODERN HEADER KARTI
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = StitchColor.Surface),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Row(
                    modifier = Modifier.padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(com.example.discbase.ui.theme.StitchPrimary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.EmojiEvents,
                            null,
                            tint = StitchColor.Primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(Modifier.width(20.dp))

                    Column {
                        Text(
                            text = tournament.tournamentName,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = StitchColor.TextPrimary
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DateRange, null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                            Spacer(Modifier.width(4.dp))
                            Text(tournament.date, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                        }
                        Spacer(Modifier.height(8.dp))
                        Surface(
                            color = if(wins >= losses) StitchOffense.copy(0.1f) else com.example.discbase.ui.theme.StitchDefense.copy(0.1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = recordText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if(wins >= losses) StitchOffense else com.example.discbase.ui.theme.StitchDefense,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // 2. MAÇ LİSTESİ (SADECE STANDART MAÇLAR)
            Text(
                "Fikstür & Sonuçlar (Standart)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = StitchColor.TextPrimary,
                modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
            )

            // --- DEĞİŞİKLİK: standardMatches KULLANIYORUZ ---
            if (standardMatches.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Sports, null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                        Spacer(Modifier.height(16.dp))
                        Text("Henüz standart maç oynanmadı.", color = Color.Gray)
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(standardMatches) { match ->
                        MatchCard(match, tournament.id, navController)
                    }
                }
            }
        }
    }
}

// ==========================================
// 4. MAÇ DETAY EKRANLARI (READ-ONLY)
// ==========================================

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MatchDetailScreen(
    navController: NavController,
    viewModel: MainViewModel,
    ourTeamName: String,
    opponentName: String,
    pointsArchive: List<PointData>,
    rosterAsStats: List<Player>,
    onBack: () -> Unit,
    tournamentId: String,
    match: Match,
    onDeleteMatch: () -> Unit,
    isAdmin: Boolean,
    onDeleteLastPoint: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedPlayerStats by remember { mutableStateOf<AdvancedPlayerStats?>(null) }
    val tabItems = listOf("Sayı Özeti", "Takım İstatistikleri", "Oyuncu İstatistikleri")
    val pagerState = rememberPagerState { tabItems.size }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    // İstatistik Hesaplama Mantığı (Değişmedi)
    // MatchDetailScreen fonksiyonunun İÇİNDEKİ yerel fonksiyon:
    // MatchDetailScreen içindeki calculateOverallStats fonksiyonunu bununla değiştir:

    fun calculateOverallStats(archive: List<PointData>): List<AdvancedPlayerStats> {
        val overallStatsMap = mutableMapOf<String, AdvancedPlayerStats>()
        // Başlangıç değerleri
        for (player in rosterAsStats) {
            overallStatsMap[player.id] = AdvancedPlayerStats(
                basicStats = PlayerStats(playerId = player.id, name = player.name),
                plusMinus = 0.0,
                oPointsPlayed = 0,
                dPointsPlayed = 0
            )
        }

        for (pointData in archive) {
            for (playerStat in pointData.stats) {
                val playerId = playerStat.playerId
                val currentOverall = overallStatsMap[playerId] ?: continue

                // İstatistikleri Topla
                val updatedBasicStats = currentOverall.basicStats.copy(
                    pointsPlayed = currentOverall.basicStats.pointsPlayed + 1,
                    successfulPass = currentOverall.basicStats.successfulPass + playerStat.successfulPass,
                    assist = currentOverall.basicStats.assist + playerStat.assist,
                    throwaway = currentOverall.basicStats.throwaway + playerStat.throwaway,
                    catchStat = currentOverall.basicStats.catchStat + playerStat.catchStat,
                    drop = currentOverall.basicStats.drop + playerStat.drop,
                    goal = currentOverall.basicStats.goal + playerStat.goal,
                    pullAttempts = currentOverall.basicStats.pullAttempts + playerStat.pullAttempts,
                    successfulPulls = currentOverall.basicStats.successfulPulls + playerStat.successfulPulls,
                    block = currentOverall.basicStats.block + playerStat.block,
                    passDistribution = (currentOverall.basicStats.passDistribution.toList() + playerStat.passDistribution.toList())
                        .groupBy({ it.first }, { it.second })
                        .mapValues { (_, values) -> values.sum() }
                )

                // --- GÜNCELLENEN FORMÜL (Blok = 1.5 Puan) ---
                val efficiencyScore = (updatedBasicStats.goal + updatedBasicStats.assist).toDouble() +
                        (updatedBasicStats.block * 1.5) -
                        (updatedBasicStats.throwaway + updatedBasicStats.drop).toDouble() +
                        (updatedBasicStats.successfulPass * 0.05)

                var newOPoints = currentOverall.oPointsPlayed
                var newDPoints = currentOverall.dPointsPlayed
                if (pointData.startMode == PointStartMode.OFFENSE) { newOPoints++ }
                else if (pointData.startMode == PointStartMode.DEFENSE) { newDPoints++ }

                overallStatsMap[playerId] = currentOverall.copy(
                    basicStats = updatedBasicStats,
                    plusMinus = efficiencyScore,
                    oPointsPlayed = newOPoints,
                    dPointsPlayed = newDPoints
                )
            }
        }
        return overallStatsMap.values.toList().sortedBy { it.basicStats.name }
    }

    val overallStats = calculateOverallStats(pointsArchive)
    val teamStats = calculateTeamStatsForMatch(pointsArchive, overallStats)

    // --- SİLME DİALOGU (Değişmedi) ---
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Maçı Sil") },
            text = { Text("Bu maçı ve içindeki tüm istatistikleri kalıcı olarak silmek istediğinizden emin misiniz? Bu işlem geri alınamaz.") },
            confirmButton = {
                Button(
                    onClick = { onDeleteMatch(); showDeleteDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Evet, Sil") }
            },
            dismissButton = { Button(onClick = { showDeleteDialog = false }) { Text("İptal") } }
        )
    }

    // --- OYUNCU DETAY PENCERESİ (BURASI GÜNCELLENDİ) ---
    // MatchDetailScreen içinde bu bloğu bul ve değiştir:
    if (selectedPlayerStats != null) {
        val advancedStats = selectedPlayerStats!!
        val stats = advancedStats.basicStats
        val playerInfo = rosterAsStats.find { it.id == stats.playerId }
        val isHandler = playerInfo?.position == "Handler" || playerInfo?.position == "Hybrid"
        var showEfficiencyInfo by remember { mutableStateOf(false) }

        if (showEfficiencyInfo) {
            // MainActivity'nin en altındaki global fonksiyonu çağır
            EfficiencyDescriptionDialog(onDismiss = { showEfficiencyInfo = false })
        }
        // --- YENİ: Bilgi Dialogunu burada çağırıyoruz ---

        AlertDialog(
            onDismissRequest = { selectedPlayerStats = null },
            title = {
                // --- BAŞLIK DÜZENLEMESİ (İsim + Info Butonu) ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stats.name, fontWeight = FontWeight.Bold)
                        Text(
                            "${playerInfo?.position ?: "Oyuncu"} | ${playerInfo?.gender ?: ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                    // Info Butonu
                    IconButton(onClick = { showEfficiencyInfo = true }) {
                        Icon(Icons.Default.Info, contentDescription = "Bilgi", tint = com.example.discbase.ui.theme.StitchPrimary)
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. GENEL ÖZET (+/- ve Sayılar)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Verimlilik (+/-)", style = MaterialTheme.typography.labelMedium, color = Color.Gray)

                            // --- HATANIN DÜZELDİĞİ YER ---
                            // String.format yerine Kotlin'in format fonksiyonunu kullandık.
                            val formattedScore = "%.1f".format(advancedStats.plusMinus)
                            val displayText = if (advancedStats.plusMinus > 0) "+$formattedScore" else formattedScore

                            Text(
                                text = displayText,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (advancedStats.plusMinus > 0) Color(0xFF03DAC5)
                                else if (advancedStats.plusMinus < 0) MaterialTheme.colorScheme.error
                                else Color.Gray
                            )
                            // ----------------------------------------
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Oynanan Sayı", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                            Text("${stats.pointsPlayed}", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Text("O: ${advancedStats.oPointsPlayed} | D: ${advancedStats.dPointsPlayed}", fontSize = 12.sp, color = Color.Gray)
                        }
                    }

                    Divider()

                    // --- HESAPLAMALAR ---
                    val totalPassesCompleted = stats.successfulPass + stats.assist
                    val totalPassesAttempted = totalPassesCompleted + stats.throwaway
                    val passSuccessRate = calculateSafePercentage(totalPassesCompleted, totalPassesAttempted)

                    val totalSuccesfulCatches = stats.catchStat + stats.goal
                    val totalCatchesAttempted = totalSuccesfulCatches + stats.drop
                    val catchRate = calculateSafePercentage(totalSuccesfulCatches, totalCatchesAttempted)

                    // --- UI BİLEŞENLERİ (Değişmedi) ---
                    val PassingSection = @Composable {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
                            Column(Modifier.padding(12.dp)) {
                                Text("Pas (Throwing)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.height(8.dp))
                                PerformanceStatRow("Başarı", passSuccessRate.text, passSuccessRate.ratio, passSuccessRate.progress)
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Asist: ${stats.assist}")
                                    Text("Throwaway: ${stats.throwaway}", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    val ReceivingSection = @Composable {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
                            Column(Modifier.padding(12.dp)) {
                                Text("Yakalama (Receiving)", fontWeight = FontWeight.Bold, color = Color(0xFF009688))
                                Spacer(Modifier.height(8.dp))
                                PerformanceStatRow("Başarı", catchRate.text, catchRate.ratio, catchRate.progress, progressColor = Color(0xFF009688))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Gol: ${stats.goal}")
                                    Text("Drop: ${stats.drop}", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    if (isHandler) { PassingSection(); ReceivingSection() } else { ReceivingSection(); PassingSection() }

                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))) {
                        Column(Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Savunma", fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Blok (D):", fontWeight = FontWeight.Bold)
                                Text("${stats.block}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                            }
                            Divider(Modifier.padding(vertical = 4.dp))
                            Text("Pull: ${stats.successfulPulls} / ${stats.pullAttempts}", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
            },
            confirmButton = { Button(onClick = { selectedPlayerStats = null }) { Text("Kapat") } }
        )
    }

    // --- ANA EKRAN YAPISI (Değişmedi) ---
    Scaffold(
        containerColor = StitchColor.Background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$ourTeamName vs $opponentName", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("${match.scoreUs} - ${match.scoreThem}", fontSize = 14.sp, color = if(match.scoreUs > match.scoreThem) StitchOffense else if(match.scoreThem > match.scoreUs) com.example.discbase.ui.theme.StitchDefense else Color.Gray, fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    ModernIconButton(Icons.Default.ArrowBack, onBack, StitchTextPrimary, "Geri")
                },
                actions = {
                    val context = LocalContext.current
                    // Butona basınca PDF oluşturma fonksiyonunu çağıracağız
                    // NOT: overallStats değişkeni hesaplandıktan sonra kullanılmalı.
                    // Bu yüzden bu butonu TopAppBar'ın içine değil de, hesaplamaların yapıldığı yere yakın koymamız lazım
                    // VEYA overallStats'ı remember ile tutup kullanabiliriz.

                    // overallStats, MatchDetailScreen'in içinde hesaplanıyor.
                    // Fonksiyonun başında `val overallStats = calculateOverallStats(pointsArchive)` var.
                    // Bu değişkene erişebiliyoruz.

                    IconButton(
                        onClick = {viewModel.shareMatchReport(context, match, "Maç Raporu", overallStats)
                        }
                    ) {
                        // Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF Rapor")
                        // PictureAsPdf material iconlarda varsayılan olmayabilir, Share kullanabiliriz:
                        Icon(Icons.Default.Share, contentDescription = "Raporu Paylaş", tint = StitchColor.Primary)
                    }
                    if (isAdmin) {
                        ModernIconButton(Icons.Default.Edit, { navController.navigate("match_playback/$tournamentId/${match.opponentName}?matchId=${match.id}") },
                            com.example.discbase.ui.theme.StitchPrimary, "Düzenle")
                        Spacer(Modifier.width(8.dp))
                        ModernIconButton(Icons.Default.Delete, { showDeleteDialog = true },
                            com.example.discbase.ui.theme.StitchDefense, "Sil")
                        Spacer(Modifier.width(8.dp))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            // TAB BAR (Sekmeler)
            TabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = StitchColor.Surface,
                contentColor = com.example.discbase.ui.theme.StitchPrimary,
                indicator = { tabPositions ->
                    androidx.compose.material3.TabRowDefaults.Indicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                        color = com.example.discbase.ui.theme.StitchPrimary,
                        height = 3.dp
                    )
                }
            ) {
                tabItems.forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        text = {
                            Text(
                                title,
                                fontWeight = if(pagerState.currentPage == index) FontWeight.Bold else FontWeight.Normal,
                                color = if(pagerState.currentPage == index) com.example.discbase.ui.theme.StitchPrimary else Color.Gray
                            )
                        }
                    )
                }
            }

            // İÇERİK
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize().weight(1f)) { pageIndex ->
                when (pageIndex) {
                    0 -> PointSummaryTab(navController, pointsArchive, tournamentId, match.id, isAdmin, onDeleteLastPoint)
                    // --- GÜNCELLENEN KISIM ---
                    1 -> TeamStatsTab(
                        teamStats = teamStats,
                        allPlayersStats = overallStats,
                        allPlayers = rosterAsStats,
                        // Verileri 'match' ve 'pointsArchive' değişkenlerinden geçiriyoruz
                        matchDurationSeconds = match.matchDurationSeconds,
                        pointsArchive = pointsArchive
                    )
                    // -------------------------
                    2 -> PlayerStatsTab(overallStats, onPlayerClick = { selectedPlayerStats = it })
                }
            }
        }
    }
}
@Composable
private fun PointSummaryTab(
    navController: NavController,
    pointsArchive: List<PointData>,
    tournamentId: String,
    matchId: String,
    isAdmin: Boolean,
    onDeleteLastPoint: () -> Unit
) {
    if (pointsArchive.isEmpty()) {
        Text(
            "Bu maç için kaydedilmiş bir sayı yok.",
            fontStyle = FontStyle.Italic,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .wrapContentHeight(Alignment.CenterVertically)
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            itemsIndexed(pointsArchive) { index, pointData ->
                // val isLastPoint artık burada GEREKLİ DEĞİL
                PointSummaryCard(
                    pointIndex = index,
                    pointData = pointData,
                    // Admin ise düzenlemeye izin ver, değilse null gönder (veya false)
                    // Aşağıda PointSummaryCard'ı da güncelleyeceğiz.
                    onViewClick = {
                        navController.navigate("point_detail_summary/$tournamentId/$matchId/$index")
                    },
                    onEditClick = {
                        navController.navigate("edit_point/$tournamentId/$matchId/$index")
                    },
                    showEditButton = isAdmin // <-- YENİ PARAMETRE OLARAK BUNU EKLEYECEĞİZ
                )
            }

            // --- YENİ BUTON VE LOGİĞİ BURAYA EKLİYORUZ ---
            if (isAdmin) {
                item {
                    // Silme dialog state'i ve Alert'i buraya taşıdık
                    var showDeleteDialog by remember { mutableStateOf(false) }
                    if (showDeleteDialog) {
                        AlertDialog(
                            onDismissRequest = { showDeleteDialog = false },
                            title = { Text("Son Sayıyı Sil") },
                            // Mesajı güncelledik (artık pointIndex yok)
                            text = { Text("Son sayıyı (${pointsArchive.size}. sayı) silmek istediğinizden emin misiniz? Bu işlem geri alınamaz.") },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        onDeleteLastPoint()
                                        showDeleteDialog = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                ) { Text("Evet, Sil") }
                            },
                            dismissButton = {
                                Button(onClick = { showDeleteDialog = false }) { Text("İptal") }
                            }
                        )
                    }

                    // Tam genişlikli yeni buton
                    Button(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp), // Kart ile arasına boşluk
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Son Sayıyı Sil")
                    }
                }
            }
            // --- YENİ BLOK BİTTİ ---
        }
    }
}
@Composable
fun TeamStatsTab(
    teamStats: AdvancedTeamStats,
    allPlayersStats: List<AdvancedPlayerStats>, // Bu parametreyi eklemeniz gerekecek (MatchDetailScreen'den geçirin)
    allPlayers: List<Player>, // Bunu da geçirin
    matchDurationSeconds: Long,
    pointsArchive: List<PointData>
) {
    // Temel oranlar
    val holdRate = calculateSafePercentage(teamStats.offensiveHolds, teamStats.totalOffensePoints)
    val breakRate = calculateSafePercentage(teamStats.breakPointsScored, teamStats.totalDefensePoints)
    val passRate = calculateSafePercentage(teamStats.totalPassesCompleted, teamStats.totalPassesAttempted)
    val conversionRate = calculateSafePercentage(teamStats.totalGoals, teamStats.totalPossessions)
    val blockConversionRate = calculateSafePercentage(teamStats.blocksConvertedToGoals, teamStats.totalBlockPoints)
    val totalTurnovers = teamStats.totalThrowaways + teamStats.totalDrops
    val teamAvgTempo = if (teamStats.totalPassesAttempted > 0)
        String.format("%.2f sn", teamStats.totalTempoSeconds.toDouble() / teamStats.totalPassesAttempted)
    else "0.00 sn"
    val avgTurnoverPerPoint = if (teamStats.totalPointsPlayed > 0)
        String.format("%.2f", totalTurnovers.toDouble() / teamStats.totalPointsPlayed) else "0.0"
    val cleanHoldRate = calculateSafePercentage(teamStats.cleanHolds, teamStats.totalOffensePoints)
        // --- YENİ ---
    val teamAvgPullTime = if (teamStats.totalPulls > 0)
        String.format("%.2f sn", teamStats.totalPullTimeSeconds.toDouble() / teamStats.totalPulls)
    else "0.00 sn"
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. ZAMAN ANALİZİ (YENİ - EN ÜSTE EKLİYORUZ)
        item {
            TimeAnalysisCard(
                matchDurationSeconds = matchDurationSeconds,
                pointsArchive = pointsArchive
            )
        }
        // 1. Performans
        item { PerformanceCard(holdRate, breakRate, passRate) }

        // 2. Verimlilik
        item { PossessionCard(conversionRate, blockConversionRate,cleanHoldRate) }

        // 3. Detaylı İstatistikler
        item {
            DetailedStatsCard(
                totalPasses = teamStats.totalPassesAttempted.toString(),
                totalCompleted = teamStats.totalPassesCompleted.toString(),
                totalTurnovers = totalTurnovers.toString(),
                avgTurnoverPoint = avgTurnoverPerPoint,
                avgTurnoverMatch = totalTurnovers.toString(),
                totalPointsPlayed = teamStats.totalPointsPlayed.toString(),
                matchesPlayed = "1",
                teamAvgTempo = teamAvgTempo,
                teamAvgPullTime = teamAvgPullTime
            )
        }

        // 4. YENİ: MAÇ PAS AĞI (Maçın En Çok Pas Yapan Oyuncusu İçin)
        item {
            Text("Pas Bağlantıları (Top 5)", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = StitchColor.TextPrimary)
        }

        // En çok pas yapan 5 oyuncuyu listele
        val topPassers = allPlayersStats
            .sortedByDescending { it.basicStats.successfulPass + it.basicStats.assist }
            .take(5)
            .filter { (it.basicStats.successfulPass + it.basicStats.assist) > 0 }

        if (topPassers.isEmpty()) {
            item { Text("Henüz pas verisi yok.", color = Color.Gray) }
        } else {
            items(topPassers) { playerStat ->
                ExpandablePassNetworkCard(
                    playerStat = playerStat.basicStats,
                    allPlayers = allPlayers
                )
            }
        }
    }
}
@Composable
fun PlayerStatsTab(
    overallStats: List<AdvancedPlayerStats>,
    onPlayerClick: (AdvancedPlayerStats) -> Unit
) {
    val playersWhoPlayed = overallStats.filter { it.basicStats.pointsPlayed > 0 }

    if (playersWhoPlayed.isEmpty()) {
        Text(
            "Bu maçta oynayan oyuncu istatistiği bulunamadı.",
            fontStyle = FontStyle.Italic,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .wrapContentHeight(Alignment.CenterVertically)
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(playersWhoPlayed) { playerWithStats ->
                PlayerStatCard(
                    playerWithStats = playerWithStats,
                    onClick = { onPlayerClick(playerWithStats) }
                )
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PointDetailScreen(
    navController: NavController,
    pointIndex: Int?,
    statsForPoint: List<PlayerStats>?,
    pointDuration: Long = 0L // <-- YENİ PARAMETRE EKLENDİ
) {
    if (statsForPoint == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Veri bulunamadı.", color = Color.Gray)
        }
        return
    }

    val playersInPoint = statsForPoint.filter { it.pointsPlayed > 0 }

    // Varsayılan olarak en çok aksiyonu olan oyuncuyu seç
    var selectedPlayerStat by remember {
        mutableStateOf(playersInPoint.maxByOrNull { it.successfulPass + it.assist }
            ?: playersInPoint.firstOrNull())
    }

    Scaffold(
        containerColor = StitchColor.Background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        if (pointIndex != null) "Sayı ${pointIndex + 1} Detayı" else "Detay",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    ModernIconButton(
                        Icons.Default.ArrowBack,
                        { navController.popBackStack() },
                        StitchTextPrimary,
                        "Geri"
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {

            // 1. OYUNCU SEÇİCİ
            Text(
                "Pas Ağını Görmek İçin Oyuncu Seçin:",
                fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(playersInPoint) { p ->
                    val isSelected = selectedPlayerStat?.playerId == p.playerId
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { selectedPlayerStat = p }
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(if (isSelected) 56.dp else 48.dp)
                                .border(
                                    if (isSelected) 2.dp else 0.dp,
                                    if (isSelected) com.example.discbase.ui.theme.StitchPrimary else Color.Transparent,
                                    CircleShape
                                )
                                .padding(4.dp)
                        ) {
                            PlayerAvatar(
                                name = p.name,
                                size = if (isSelected) 48.dp else 40.dp,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            p.name.split(" ").first(),
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) com.example.discbase.ui.theme.StitchPrimary else Color.Gray
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    val totalPasses = playersInPoint.sumOf { it.successfulPass + it.assist }
                    val totalTurns = playersInPoint.sumOf { it.throwaway + it.drop }
                    val totalBlocks = playersInPoint.sumOf { it.block }

                    // --- ÖZET KARTI (SÜRE EKLENDİ) ---
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = StitchColor.Surface),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            // Başlık ve Süre Yan Yana
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Sayı Özeti", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = StitchColor.TextPrimary)

                                // SÜRE GÖSTERİMİ
                                Surface(
                                    color = Color(0xFFE3F2FD),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.AccessTime, null, modifier = Modifier.size(14.dp), tint = Color(0xFF1565C0))
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            formatSecondsToTime(pointDuration),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1565C0)
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("$totalPasses", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = StitchOffense)
                                    Text("Pas", fontSize = 11.sp, color = Color.Gray)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("$totalTurns", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = StitchDefense)
                                    Text("Hata", fontSize = 11.sp, color = Color.Gray)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("$totalBlocks", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = StitchColor.Primary)
                                    Text("Blok", fontSize = 11.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
                // ------------------------------
                // SEÇİLİ OYUNCUNUN PAS AĞI
                if (selectedPlayerStat != null) {
                    val totalAction =
                        selectedPlayerStat!!.successfulPass + selectedPlayerStat!!.assist
                    item {
                        // --- ÖNEMLİ DÜZELTME: 'key' bloğu grafiğin zorla yenilenmesini sağlar ---
                        androidx.compose.runtime.key(selectedPlayerStat!!.playerId) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = StitchColor.Surface),
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(2.dp)
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    Text(
                                        "${selectedPlayerStat!!.name} Pas Trafiği",
                                        fontWeight = FontWeight.Bold,
                                        color = StitchColor.TextPrimary,
                                        fontSize = 16.sp
                                    )

                                    if (totalAction == 0) {
                                        Text(
                                            "Bu sayıda pas verisi yok.",
                                            fontSize = 12.sp,
                                            color = Color.Gray,
                                            modifier = Modifier.padding(top = 8.dp)
                                        )
                                    } else {
                                        Spacer(Modifier.height(16.dp))
                                        val dummyAllPlayers = playersInPoint.map {
                                            Player(
                                                id = it.playerId,
                                                name = it.name
                                            )
                                        }

                                        PassNetworkChordDiagram(
                                            mainPlayerName = selectedPlayerStat!!.name,
                                            passDistribution = selectedPlayerStat!!.passDistribution,
                                            allPlayers = dummyAllPlayers
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Text(
                        "Tüm Oyuncu İstatistikleri",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = StitchColor.TextPrimary
                    )
                }

                items(playersInPoint) { playerStat ->
                    PointPlayerStatCard(playerStat = playerStat)
                }
            }
        }
    }
}
// ==========================================
// 5. AYARLAR YARDIMCI COMPOSABLE'LAR
// ==========================================
@Composable
fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column {
        Text(
            title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
        )
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = StitchColor.Surface),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column { content() }
        }
    }
}

@Composable
fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Color.Gray)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            if (subtitle != null) {
                Text(subtitle, fontSize = 12.sp, color = Color.Gray)
            }
        }
        Icon(Icons.Default.ChevronRight, null, tint = Color.LightGray)
    }
}

@Composable
fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Color.Gray)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            if (subtitle != null) {
                Text(subtitle, fontSize = 12.sp, color = Color.Gray)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = if (enabled) onCheckedChange else null,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = com.example.discbase.ui.theme.StitchPrimary
            )
        )
    }
}

@Composable
fun NameFormatOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(selectedColor = com.example.discbase.ui.theme.StitchPrimary)
        )
        Spacer(Modifier.width(8.dp))
        Text(label, fontSize = 14.sp)
    }
}
@Composable
fun SettingsActionRow(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    color: Color = StitchColor.TextPrimary // Varsayılan renk, parametre gelmezse bunu kullanır
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = color
        )
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberManagementCard(
    members: Map<String, String>,
    userProfiles: Map<String, UserProfile>, // <-- YENİ PARAMETREYİ KABUL EDİN
    currentUserId: String,
    onUpdateRole: (uid: String, newRole: String) -> Unit,
    onRemoveMember: (uid: String) -> Unit
) {
    // Üyeleri rollere göre grupla (önce pending, sonra member, sonra admin)
    val sortedMembers = members.toList().sortedWith(
        compareBy {
            when (it.second) {
                "pending" -> 0
                "member" -> 1
                "admin" -> 2
                else -> 3
            }
        }
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Üye Yönetimi",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))

            if (sortedMembers.isEmpty()) {
                Text("Takımda üye yok.")
            }

            sortedMembers.forEach { (uid, role) ->
                var showRoleMenu by remember { mutableStateOf(false) }

                // Kendi satırımızı (admin) gösterme
                if (uid != currentUserId) {

                    // --- GÜNCELLENMİŞ KOD BLOĞU ---
                    // uid'ye karşılık gelen profili 'userProfiles' haritasından bul
                    val userProfile = userProfiles[uid]
                    // Profil bulunamazsa veya adı yoksa varsayılan metinler kullan
                    val displayName = userProfile?.displayName ?: "Bilinmeyen Kullanıcı"
                    val displayEmail =
                        userProfile?.email ?: uid // E-posta yoksa en azından UID'yi göster

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            // ESKİ: Text(uid, style = MaterialTheme.typography.bodySmall, color = Color.Gray)

                            // YENİ: İsim ve e-posta/uid göster
                            Text(
                                text = displayName,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = displayEmail,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )

                            // Rolü göster (stili biraz daha az vurgulu yaptık)
                            Text(
                                text = when (role) {
                                    "pending" -> "ONAY BEKLİYOR"
                                    "member" -> "Üye"
                                    "admin" -> "Kaptan"
                                    else -> role
                                },
                                style = MaterialTheme.typography.bodyMedium, // Stil değişti
                                fontWeight = FontWeight.Normal, // Stil değişti
                                color = if (role == "pending") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        // --- GÜNCELLEME BİTTİ ---
                        // Eylem Menüsü
                        Box {
                            IconButton(onClick = { showRoleMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Rolü Yönet")
                            }

                            androidx.compose.material3.DropdownMenu(
                                expanded = showRoleMenu,
                                onDismissRequest = { showRoleMenu = false }
                            ) {
                                if (role == "pending") {
                                    androidx.compose.material3.DropdownMenuItem(
                                        text = { Text("Onayla (Üye Yap)") },
                                        onClick = {
                                            onUpdateRole(uid, "member")
                                            showRoleMenu = false
                                        }
                                    )
                                }
                                if (role == "member") {
                                    androidx.compose.material3.DropdownMenuItem(
                                        text = { Text("Kaptan Yap") },
                                        onClick = {
                                            onUpdateRole(uid, "admin")
                                            showRoleMenu = false
                                        }
                                    )
                                }
                                if (role == "admin") {
                                    androidx.compose.material3.DropdownMenuItem(
                                        text = { Text("Yetkisini Al (Üye Yap)") },
                                        onClick = {
                                            onUpdateRole(uid, "member")
                                            showRoleMenu = false
                                        }
                                    )
                                }
                                androidx.compose.material3.DropdownMenuItem(
                                    text = {
                                        Text(
                                            "Takımdan At / Reddet",
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    onClick = {
                                        onRemoveMember(uid)
                                        showRoleMenu = false
                                    }
                                )
                            }
                        }
                    }
                    Divider()
                }
            }
        }
    }
}

// --- 6. ANA İSKELET (ALT MENÜLÜ) ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(
    topLevelNavController: NavController,
    tournaments: List<Tournament>,
    teamProfile: TeamProfile,
    allPlayers: List<Player>,
    isAdmin: Boolean,
    activeTeamId: String?,
    mainViewModel: MainViewModel
) {
    val bottomBarNavController = rememberNavController()
    val navBackStackEntry by bottomBarNavController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route
    val isProModeEnabled by mainViewModel.proModeEnabled.collectAsState()
    // Hangi sayfalarda Ana Üst Başlık (Global TopBar) GİZLENSİN?
    // Home ve Trainings sayfalarının kendi özel başlıkları var, o yüzden burada gizliyoruz.
    var isFullScreen by remember { mutableStateOf(false) }

    val dynamicBottomNavItems = remember(isProModeEnabled) {
        val items = mutableListOf(
            Screen.Home,
            Screen.Tournaments,
            Screen.Roster,
            Screen.Trainings
        )
        if (isProModeEnabled) {
            items.add(Screen.ProMode) // Sadece açıkken ekle
        }
        items
    }
    val isProModeActive = currentRoute == Screen.ProMode.route

    // Hangi sayfalarda Ana Üst Başlık (Global TopBar) GİZLENSİN?
    val hideGlobalTopBar = currentRoute == Screen.Home.route ||
            currentRoute == Screen.Trainings.route ||
            isFullScreen // <-- Eğer tam ekransa gizle
    Scaffold(
        containerColor = StitchColor.Background, // Tüm sayfaların arka planı
        topBar = {
            // Sadece Kadro ve Turnuvalar sayfasında bu başlık görünür
            if (!hideGlobalTopBar) {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = when (currentRoute) {
                                Screen.Roster.route -> "Kadro"
                                Screen.Tournaments.route -> "Turnuvalar"
                                else -> teamProfile.teamName
                            },
                            fontWeight = FontWeight.Bold,
                            color = StitchColor.TextPrimary
                        )
                    },
                    actions = {
                        // --- GÜNCELLENMİŞ PROFİL BUTONU (BADGE İLE) ---
                        // ViewModel'den bildirim durumunu dinle
                        val hasNotification by mainViewModel.hasPendingRequests.collectAsState()

                        // Profil İkonu ve Kırmızı Nokta Kutusu
                        androidx.compose.material3.BadgedBox(
                            badge = {
                                if (hasNotification) {
                                    androidx.compose.material3.Badge(
                                        containerColor = com.example.discbase.ui.theme.StitchDefense, // Kırmızı renk
                                        contentColor = Color.White
                                    ) {
                                        // İstersen sayı da yazdırabilirsin ama şimdilik sadece nokta yeterli
                                        // Text("!")
                                    }
                                }
                            },
                            modifier = Modifier.padding(end = 8.dp) // Sağdan biraz boşluk
                        ) {
                            ModernIconButton(
                                icon = Icons.Default.Person,
                                onClick = { topLevelNavController.navigate("profile_edit") },
                                color = com.example.discbase.ui.theme.StitchPrimary,
                                contentDescription = "Profil"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = StitchColor.Background // Arka planla bütünleşik
                    )
                )
            }
        },
        bottomBar = {
        if (!isFullScreen) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(16.dp, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
                color = Color.White,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                NavigationBar(
                    containerColor = StitchColor.Surface,
                    tonalElevation = 0.dp,
                    windowInsets = WindowInsets(0)
                ) {
                    dynamicBottomNavItems.forEach { screen ->
                        val isSelected = currentDestination?.route == screen.route
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title, modifier = Modifier.size(24.dp)) },
                            label = { Text(screen.title, fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
                            selected = isSelected,
                            onClick = {
                                bottomBarNavController.navigate(screen.route) {
                                    popUpTo(bottomBarNavController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = StitchPrimary,
                                selectedTextColor = StitchPrimary,
                                indicatorColor = StitchPrimary.copy(alpha = 0.1f),
                                unselectedIconColor = Color.Gray,
                                unselectedTextColor = Color.Gray
                            )
                        )
                    }
                }
            }
        }
}
) { innerPadding ->
    NavHost(
        navController = bottomBarNavController,
        startDestination = Screen.Home.route,
        modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()
    ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    navController = topLevelNavController,
                    teamProfile = teamProfile,
                    tournaments = tournaments
                )
            }

            composable(Screen.Tournaments.route) {
                TournamentListScreen(
                    navController = topLevelNavController,
                    tournaments = tournaments,
                    onAddTournament = { topLevelNavController.navigate("tournament_setup") },
                    isAdmin = isAdmin
                )
            }

            composable(Screen.Trainings.route) {
                val trainings by mainViewModel.trainings.collectAsState()
                TrainingScreen(
                    navController = topLevelNavController,
                    trainings = trainings,
                    allPlayers = allPlayers,
                    isAdmin = isAdmin,
                    onSaveTraining = { mainViewModel.saveTraining(it) },
                    onDeleteTraining = { mainViewModel.deleteTraining(it.id) }
                )
            }

            composable(Screen.Roster.route) {
                RosterScreen(
                    navController = topLevelNavController,
                    rosterPlayers = allPlayers,
                    onAddPlayerClick = { topLevelNavController.navigate("player_add") },
                    isAdmin = isAdmin
                )
            }

            composable("jersey_grid") {
                JerseyGridScreen(
                    navController = topLevelNavController,
                    allPlayers = allPlayers
                )
            }
            composable(Screen.ProMode.route) {
                val tournamentsList by mainViewModel.tournaments.collectAsState()

            // BURAYA CALLBACK VERİYORUZ:
                ProModeHost(
                mainViewModel = mainViewModel,
                tournaments = tournamentsList,
                allPlayers = allPlayers,
                onFullScreenToggle = { fullScreen -> isFullScreen = fullScreen } // <-- BU YENİ
                )
            }
        }
    }
}
@Composable
fun TimeAnalysisCard(
    matchDurationSeconds: Long, // Net Oyun Süresi
    pointsArchive: List<PointData>
) {
    // --- HESAPLAMALAR ---
    // 1. Toplam Duraksama Süresi
    val allStoppages = pointsArchive.flatMap { it.stoppages }
    val totalStoppageSeconds = allStoppages.sumOf { it.durationSeconds }

    // 2. Toplam Etkinlik Süresi (Duvar Saati) = Net + Duraksama
    val totalWallClockSeconds = matchDurationSeconds + totalStoppageSeconds

    // 3. Oranlar
    val activeRatio = if (totalWallClockSeconds > 0) matchDurationSeconds.toFloat() / totalWallClockSeconds else 0f

    // 4. Pull İstatistikleri (Sadece 0'dan büyük olanlar)
    val pullTimes = pointsArchive.map { it.pullDurationSeconds }.filter { it > 0 }
    val avgPullTime = if (pullTimes.isNotEmpty()) pullTimes.average() else 0.0

    // 5. Duraksama Türleri Sayısı
    val timeoutCount = allStoppages.count { it.type == StoppageType.TIMEOUT }
    val callCount = allStoppages.count { it.type == StoppageType.CALL }
    val injuryCount = allStoppages.count { it.type == StoppageType.INJURY }

    // Eğer hiç süre verisi yoksa kartı gizle (Eski maçlar için)
    if (totalWallClockSeconds == 0L) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = StitchColor.Surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Başlık
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccessTime, null, tint = StitchColor.Primary)
                Spacer(Modifier.width(8.dp))
                Text("Zaman Analizi", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = StitchColor.TextPrimary)
            }
            Divider(Modifier.padding(vertical = 12.dp).alpha(0.1f))

            // 1. GRAFİK BAR (Net vs Duraksama)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Net Oyun: ${formatSecondsToTime(matchDurationSeconds)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = StitchOffense)
                Text("Duraksama: ${formatSecondsToTime(totalStoppageSeconds)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = StitchDefense)
            }
            Spacer(Modifier.height(8.dp))

            // Görsel Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(50))
                    .background(StitchDefense) // Arka plan (Duraksama Rengi)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(activeRatio)
                        .fillMaxHeight()
                        .background(StitchOffense) // Ön plan (Net Süre Rengi)
                )
            }

            Spacer(Modifier.height(16.dp))

            // 2. PULL SÜRESİ
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Sports, null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                Spacer(Modifier.width(8.dp))
                Text("Ortalama Pull Süresi (Hangtime): ", fontSize = 13.sp, color = StitchColor.TextPrimary)
                Text(
                    String.format("%.2f sn", avgPullTime),
                    fontWeight = FontWeight.Bold,
                    color = StitchColor.Primary
                )
            }

            Spacer(Modifier.height(12.dp))

            // 3. DURAKSAMA DETAYLARI
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StoppageCountBadge("Timeout", timeoutCount, Color(0xFFFFA000)) // Amber
                StoppageCountBadge("Call/Faul", callCount, Color(0xFF5D4037)) // Kahve
                StoppageCountBadge("Sakatlık", injuryCount, StitchDefense) // Kırmızı
            }
        }
    }
}

@Composable
fun StoppageCountBadge(label: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(count.toString(), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = color)
        Text(label, fontSize = 10.sp, color = Color.Gray)
    }
}