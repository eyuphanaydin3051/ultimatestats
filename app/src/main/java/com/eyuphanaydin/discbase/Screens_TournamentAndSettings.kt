package com.eyuphanaydin.discbase

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
import com.eyuphanaydin.discbase.ui.theme.*
import kotlinx.coroutines.launch
import java.util.*
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.res.stringResource
import androidx.core.os.LocaleListCompat
import com.eyuphanaydin.discbase.R // R sınıfının senin paket isminle olduğundan emin ol
import com.eyuphanaydin.discbase.R.string.language_option

// ==========================================
// 1. GİRİŞ VE BAŞLANGIÇ EKRANLARI
// ==========================================

@Composable
fun LoadingScreen(text: String = stringResource(R.string.loading)) {
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

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        signInViewModel.handleGoogleSignInResult(result, context)
    }

    LaunchedEffect(signInState) {
        when (val state = signInState) {
            is SignInState.Success -> {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
            }
            is SignInState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
            }
            else -> {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_launcher_playstore),
            contentDescription = "App Logo",
            modifier = Modifier.clip(CircleShape).size(150.dp)
        )

        Spacer(Modifier.height(24.dp))
        Text(
            stringResource(R.string.app_name),
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            stringResource(R.string.signin_welcome_desc),
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
                Text(stringResource(R.string.signin_google_btn), fontSize = 16.sp)
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
    viewModel: MainViewModel = viewModel()
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) }

    var newTeamName by remember { mutableStateOf("") }
    var joinTeamId by remember { mutableStateOf("") }

    val context = LocalContext.current

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text(stringResource(R.string.team_create_dialog)) },
            text = {
                Column {
                    Text(stringResource(R.string.team_name_label), fontSize = 14.sp, color = Color.Gray)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newTeamName,
                        onValueChange = { newTeamName = it },
                        label = { Text(stringResource(R.string.team_name_label)) },
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
                            newTeamName = ""
                        } else {
                            Toast.makeText(context, context.getString(R.string.msg_name_required), Toast.LENGTH_SHORT).show()
                        }
                    }
                ) { Text(stringResource(R.string.btn_create)) }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text(stringResource(R.string.btn_cancel)) }
            }
        )
    }

    if (showJoinDialog) {
        AlertDialog(
            onDismissRequest = { showJoinDialog = false },
            title = { Text(stringResource(R.string.team_join_dialog)) },
            text = {
                Column {
                    Text(stringResource(R.string.team_code_label), fontSize = 14.sp, color = Color.Gray)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = joinTeamId,
                        onValueChange = { joinTeamId = it },
                        label = { Text(stringResource(R.string.team_code_label)) },
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
                            joinTeamId = ""
                        } else {
                            Toast.makeText(context, context.getString(R.string.msg_code_required), Toast.LENGTH_SHORT).show()
                        }
                    }
                ) { Text(stringResource(R.string.btn_join)) }
            },
            dismissButton = {
                TextButton(onClick = { showJoinDialog = false }) { Text(stringResource(R.string.btn_cancel)) }
            }
        )
    }

    Scaffold(
        containerColor = StitchColor.Background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.team_my_teams), fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onSignOut) {
                        Icon(Icons.AutoMirrored.Filled.Logout, null, tint = com.eyuphanaydin.discbase.ui.theme.StitchDefense)
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
                    text = { Text(stringResource(R.string.btn_join)) }
                )
                ExtendedFloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = StitchColor.Primary,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Default.Add, null) },
                    text = { Text(stringResource(R.string.btn_create_new_team)) }
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
                        Text(stringResource(R.string.team_empty), color = Color.Gray)
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(userTeamsList) { teamProfile ->
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
                                    Text("${stringResource(R.string.label_invite_code)} ${teamProfile.teamId}", fontSize = 12.sp, color = Color.Gray)
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
    onProfileCreated: () -> Unit
) {
    var displayName by remember { mutableStateOf("") }
    val context = LocalContext.current
    val profileState by mainViewModel.profileState.collectAsState()

    LaunchedEffect(profileState) {
        if (profileState == MainViewModel.UserProfileState.EXISTS) {
            onProfileCreated()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.profile_create_title)) })
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
                stringResource(R.string.signin_welcome_title),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.profile_create_desc),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = Color.Gray
            )
            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text(stringResource(R.string.profile_name_hint)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
            )
            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    if (displayName.isBlank() || displayName.length < 3) {
                        Toast.makeText(context, context.getString(R.string.error_name_required), Toast.LENGTH_SHORT).show()
                    } else {
                        mainViewModel.saveManualProfile(displayName)
                    }
                },
                enabled = displayName.isNotBlank() && profileState != MainViewModel.UserProfileState.LOADING,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                if (profileState == MainViewModel.UserProfileState.LOADING) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Text(stringResource(R.string.btn_save_continue), fontSize = 16.sp)
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
    val context = LocalContext.current
    val mainViewModel: MainViewModel = viewModel()
    val scope = rememberCoroutineScope()

    val currentProfile by mainViewModel.profile.collectAsState()
    val currentUserRole by mainViewModel.currentUserRole.collectAsState()
    val isAdmin = currentUserRole == "admin"

    var teamName by remember { mutableStateOf(currentProfile.teamName) }
    var tempLogoUri by remember { mutableStateOf<Uri?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    val cropImageLauncher = rememberLauncherForActivityResult(contract = CropImageContract()) { result ->
        if (result.isSuccessful) {
            tempLogoUri = result.uriContent
        } else {
            val exception = result.error
            Toast.makeText(context, "${context.getString(R.string.btn_cancel)}: ${exception?.message}", Toast.LENGTH_SHORT).show()
        }
    }

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
                        activityTitle = context.getString(R.string.profile_photo_crop),
                        cropMenuCropButtonTitle = context.getString(R.string.btn_ok),
                    )
                )
                cropImageLauncher.launch(cropOptions)
            }
        }
    )

    Scaffold(
        containerColor = StitchColor.Background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.team_settings_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    ModernIconButton(
                        icon = Icons.Default.ArrowBack,
                        onClick = { navController.popBackStack() },
                        color = StitchColor.TextPrimary,
                        contentDescription = stringResource(R.string.desc_back)
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
                    Box(contentAlignment = Alignment.BottomEnd) {
                        val logoToShow: Any? = tempLogoUri ?: getLogoModel(currentProfile.logoPath)

                        if (logoToShow != null) {
                            AsyncImage(
                                model = logoToShow,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, com.eyuphanaydin.discbase.ui.theme.StitchPrimary, CircleShape),
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

                        if (isAdmin) {
                            Surface(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                shape = CircleShape,
                                color = com.eyuphanaydin.discbase.ui.theme.StitchPrimary,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Edit, null, tint = Color.White, modifier = Modifier.padding(6.dp))
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = teamName,
                        onValueChange = { teamName = it },
                        label = { Text(stringResource(R.string.team_name_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        enabled = isAdmin,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = com.eyuphanaydin.discbase.ui.theme.StitchPrimary,
                            focusedLabelColor = com.eyuphanaydin.discbase.ui.theme.StitchPrimary
                        )
                    )

                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = currentProfile.teamId,
                        onValueChange = {},
                        label = { Text(stringResource(R.string.team_invite_code)) },
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = {
                            IconButton(onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("Team Code", currentProfile.teamId)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, context.getString(R.string.msg_copied), Toast.LENGTH_SHORT).show()
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

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onChangeTeam,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, com.eyuphanaydin.discbase.ui.theme.StitchPrimary),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = com.eyuphanaydin.discbase.ui.theme.StitchPrimary)
                ) { Text(stringResource(R.string.btn_switch_team)) }

                Button(
                    onClick = onSignOut,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = com.eyuphanaydin.discbase.ui.theme.StitchDefense)
                ) { Text(stringResource(R.string.settings_logout)) }
            }

            val hasChanges = (teamName != currentProfile.teamName) || (tempLogoUri != null)

            if (isAdmin && hasChanges) {
                Button(
                    onClick = {
                        scope.launch {
                            isSaving = true
                            var base64Logo: String? = null
                            if (tempLogoUri != null) {
                                base64Logo = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                    uriToCompressedBase64(context, tempLogoUri!!)
                                }
                            }
                            mainViewModel.saveProfile(teamName, base64Logo) {
                                isSaving = false
                                navController.popBackStack()
                                Toast.makeText(context, context.getString(R.string.msg_profile_updated), Toast.LENGTH_SHORT).show()
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
                        Text(stringResource(R.string.btn_save_changes), fontWeight = FontWeight.Bold)
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
    allPlayers: List<Player>,
    tournaments: List<Tournament>
) {
    val context = LocalContext.current
    var showLanguageDialog by remember { mutableStateOf(false) }
    val currentNameFormat by viewModel.nameFormat.collectAsState()
    val keepScreenOn by viewModel.keepScreenOn.collectAsState()
    val vibrationEnabled by viewModel.vibrationEnabled.collectAsState()
    val isLeftHanded by viewModel.isLeftHanded.collectAsState()
    val currentCaptureMode by viewModel.captureMode.collectAsState()
    val isTimeTrackingEnabled by viewModel.timeTrackingEnabled.collectAsState()
    val isProModeEnabled by viewModel.proModeEnabled.collectAsState()

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importBackupFromJson(uri)
        }
    }

    val sampleName = "Eyüphan Aydın"
    val formattedSample = when (currentNameFormat) {
        NameFormat.FULL_NAME -> "Eyüphan Aydın"
        NameFormat.FIRST_NAME_LAST_INITIAL -> "Eyüphan A."
        NameFormat.INITIAL_LAST_NAME -> "E. Aydın"
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.saveBackupToUri(uri, allPlayers, tournaments)
        }
    }

    Scaffold(
        containerColor = StitchColor.Background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.settings_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    ModernIconButton(
                        Icons.Default.ArrowBack,
                        { navController.popBackStack() },
                        StitchTextPrimary,
                        stringResource(R.string.desc_back)
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
            SettingsSection(title = stringResource(R.string.settings_section_mode)) {
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
                        Text(stringResource(R.string.settings_simple_mode), fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.settings_simple_desc), fontSize = 12.sp, color = Color.Gray)
                    }
                }

                Divider(color = Color.LightGray.copy(0.2f), modifier = Modifier.padding(horizontal = 16.dp))

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
                        Text(stringResource(R.string.settings_adv_mode), fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.settings_adv_desc), fontSize = 12.sp, color = Color.Gray)
                    }
                }

                SettingsSwitchRow(
                    icon = Icons.Default.Map,
                    title = stringResource(R.string.settings_pro_mode),
                    subtitle = stringResource(R.string.settings_pro_desc),
                    checked = isProModeEnabled,
                    onCheckedChange = { viewModel.setProModeEnabled(it) }
                )

                Divider(color = Color.LightGray.copy(0.2f))

                SettingsSwitchRow(
                    icon = Icons.Default.AccessTime,
                    title = stringResource(R.string.settings_time_mgmt),
                    subtitle = stringResource(R.string.settings_time_desc),
                    checked = isTimeTrackingEnabled,
                    onCheckedChange = { viewModel.setTimeTrackingEnabled(it) }
                )
            }

            SettingsSection(title = stringResource(R.string.settings_section_general)) {
                SettingsRow(
                    icon = Icons.Default.Language,
                    title = stringResource(id = language_option),
                    subtitle = "Türkçe / English",
                    onClick = { showLanguageDialog = true }
                )
                Divider(color = Color.LightGray.copy(0.2f))
                SettingsSwitchRow(
                    icon = Icons.Default.Smartphone,
                    title = stringResource(R.string.settings_keep_screen),
                    subtitle = stringResource(R.string.settings_keep_screen_desc),
                    checked = keepScreenOn,
                    onCheckedChange = { viewModel.setKeepScreenOn(it) }
                )
            }

            SettingsSection(title = stringResource(R.string.settings_section_appearance)) {
                Text(
                    stringResource(R.string.settings_name_format),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                Surface(
                    color = com.eyuphanaydin.discbase.ui.theme.StitchPrimary.copy(0.05f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(stringResource(R.string.settings_preview) + " ", color = Color.Gray, fontSize = 12.sp)
                        Text(
                            formattedSample,
                            fontWeight = FontWeight.Bold,
                            color = com.eyuphanaydin.discbase.ui.theme.StitchPrimary,
                            fontSize = 14.sp
                        )
                    }
                }
                NameFormatOption(
                    stringResource(R.string.format_first_last_initial),
                    currentNameFormat == NameFormat.FIRST_NAME_LAST_INITIAL
                ) { viewModel.updateNameFormat(NameFormat.FIRST_NAME_LAST_INITIAL) }
                Divider(modifier = Modifier.padding(horizontal = 16.dp), color = Color.LightGray.copy(0.2f))
                NameFormatOption(
                    stringResource(R.string.format_initial_last),
                    currentNameFormat == NameFormat.INITIAL_LAST_NAME
                ) { viewModel.updateNameFormat(NameFormat.INITIAL_LAST_NAME) }
                Divider(modifier = Modifier.padding(horizontal = 16.dp), color = Color.LightGray.copy(0.2f))
                NameFormatOption(
                    stringResource(R.string.format_full),
                    currentNameFormat == NameFormat.FULL_NAME
                ) { viewModel.updateNameFormat(NameFormat.FULL_NAME) }
            }

            val currentTheme by viewModel.appTheme.collectAsState()

            SettingsSection(title = stringResource(R.string.settings_section_theme)) {
                Column(Modifier.padding(vertical = 8.dp)) {
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
                            colors = RadioButtonDefaults.colors(selectedColor = com.eyuphanaydin.discbase.ui.theme.StitchPrimary)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.theme_light))
                    }

                    Divider(color = Color.LightGray.copy(0.2f), modifier = Modifier.padding(horizontal = 16.dp))

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
                            colors = RadioButtonDefaults.colors(selectedColor = com.eyuphanaydin.discbase.ui.theme.StitchPrimary)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.theme_dark))
                    }

                    Divider(color = Color.LightGray.copy(0.2f), modifier = Modifier.padding(horizontal = 16.dp))

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
                            colors = RadioButtonDefaults.colors(selectedColor = com.eyuphanaydin.discbase.ui.theme.StitchPrimary)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.theme_system))
                    }
                }
            }

            SettingsSection(title = stringResource(R.string.settings_section_usage)) {
                SettingsSwitchRow(
                    icon = Icons.Default.Vibration,
                    title = stringResource(R.string.settings_vibration),
                    subtitle = stringResource(R.string.settings_vibration_desc),
                    checked = vibrationEnabled,
                    onCheckedChange = { viewModel.setVibrationEnabled(it) }
                )
                Divider(color = Color.LightGray.copy(0.2f))

                SettingsSwitchRow(
                    icon = Icons.Default.PanTool,
                    title = stringResource(R.string.settings_left_handed),
                    subtitle = stringResource(R.string.settings_left_handed_desc),
                    checked = isLeftHanded,
                    onCheckedChange = { viewModel.setLeftHanded(it) },
                    enabled = true
                )
            }

            SettingsSection(title = stringResource(R.string.settings_section_data)) {
                SettingsActionRow(
                    icon = Icons.Default.Save,
                    text = stringResource(R.string.settings_backup),
                    onClick = {
                        val date = java.text.SimpleDateFormat("yyyyMMdd_HHmm", java.util.Locale.getDefault()).format(java.util.Date())
                        val fileName = "DiscBase_Backup_$date.json"
                        exportLauncher.launch(fileName)
                    }
                )
                Divider(color = Color.LightGray.copy(0.2f))

                SettingsActionRow(
                    icon = Icons.Default.Restore,
                    text = stringResource(R.string.settings_restore),
                    onClick = {
                        importLauncher.launch(arrayOf("application/json"))
                    }
                )
                Divider(color = Color.LightGray.copy(0.2f))

                SettingsActionRow(
                    icon = Icons.Default.DeleteForever,
                    text = stringResource(R.string.settings_reset),
                    color = com.eyuphanaydin.discbase.ui.theme.StitchDefense,
                    onClick = {
                        Toast.makeText(context, "Feature to delete data should be implemented in ViewModel", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold, color = Color.Gray)
                Text(stringResource(R.string.settings_footer_version, "1.2.2", "2025"), fontSize = 12.sp, color = Color.LightGray)
            }
            Spacer(Modifier.height(32.dp))
        }
    }
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = {
                Text(text = stringResource(R.string.dialog_language_title))
            },
            text = {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                changeAppLanguage("tr")
                                showLanguageDialog = false
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "🇹🇷  Türkçe", style = MaterialTheme.typography.bodyLarge)
                    }

                    Divider()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                changeAppLanguage("en")
                                showLanguageDialog = false
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "🇺🇸  English", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
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
                ExtendedFloatingActionButton(
                    onClick = onAddTournament,
                    containerColor = StitchColor.Primary,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.tour_create_btn), fontWeight = FontWeight.Bold) }
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
                            stringResource(R.string.tour_empty),
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 80.dp),
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
                title = { Text(if (isEditing) stringResource(R.string.tour_setup_edit) else stringResource(R.string.tour_setup_new), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    ModernIconButton(Icons.Default.ArrowBack, { navController.popBackStack() }, StitchTextPrimary, stringResource(R.string.desc_back))
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
                        Text(stringResource(R.string.tour_details_header), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = StitchColor.TextPrimary)
                    }

                    OutlinedTextField(
                        value = tournamentName,
                        onValueChange = { tournamentName = it },
                        label = { Text(stringResource(R.string.tour_name_hint)) },
                        placeholder = { Text("Ex: XI. ODTU UFT") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = com.eyuphanaydin.discbase.ui.theme.StitchPrimary,
                            focusedLabelColor = com.eyuphanaydin.discbase.ui.theme.StitchPrimary
                        )
                    )

                    OutlinedTextField(
                        value = tournamentDate,
                        onValueChange = {},
                        label = { Text(stringResource(R.string.label_date)) },
                        placeholder = { Text("Select Date") },
                        readOnly = true,
                        enabled = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { datePickerDialog.show() },
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = { Icon(Icons.Default.Event, null, tint = StitchColor.Primary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = StitchColor.TextPrimary,
                            disabledBorderColor = Color.Gray,
                            disabledLabelColor = Color.Gray,
                            disabledTrailingIconColor = com.eyuphanaydin.discbase.ui.theme.StitchPrimary
                        )
                    )
                }
            }

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
                        stringResource(R.string.tour_roster_header, selectedPlayerIds.size),
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
                                    .background(if (isSelected) com.eyuphanaydin.discbase.ui.theme.StitchPrimary.copy(0.1f) else Color.Transparent)
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
                                    colors = CheckboxDefaults.colors(checkedColor = com.eyuphanaydin.discbase.ui.theme.StitchPrimary)
                                )
                            }
                        }
                    }
                }
            }

            Button(
                onClick = {
                    val tournamentData = Tournament(
                        id = tournamentToEdit?.id ?: UUID.randomUUID().toString(),
                        tournamentName = tournamentName,
                        ourTeamName = currentTeamName,
                        date = tournamentDate.ifBlank { "Unknown Date" },
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
                Text(if (isEditing) stringResource(R.string.btn_update) else stringResource(R.string.btn_create_caps), fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
            .sortedByDescending { it.id }
    }
    val wins = standardMatches.count { it.scoreUs > it.scoreThem }
    val losses = standardMatches.count { it.scoreThem > it.scoreUs }
    val recordText = "$wins ${stringResource(R.string.pdf_wins)} - $losses ${stringResource(R.string.pdf_losses)}"

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.match_delete_title)) },
            text = { Text(stringResource(R.string.match_delete_msg)) },
            confirmButton = {
                Button(
                    onClick = { onDeleteTournament(); showDeleteDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text(stringResource(R.string.btn_delete_confirm)) }
            },
            dismissButton = { Button(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.btn_cancel)) } }
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
                        contentDescription = stringResource(R.string.desc_back)
                    )
                },
                actions = {
                    val context = LocalContext.current
                    val vm: MainViewModel = androidx.lifecycle.viewmodel.compose.viewModel()

                    IconButton(onClick = {
                        vm.shareTournamentReport(context, tournament, allPlayers)
                    }) {
                        Icon(Icons.Default.Share, contentDescription = stringResource(R.string.tour_get_report), tint = StitchColor.Primary)
                    }

                    if (isAdmin) {
                        Row(modifier = Modifier.padding(end = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ModernIconButton(
                                icon = Icons.Default.Edit,
                                onClick = { navController.navigate("tournament_setup?tournamentId=${tournament.id}") },
                                color = com.eyuphanaydin.discbase.ui.theme.StitchPrimary,
                                contentDescription = stringResource(R.string.btn_edit)
                            )
                            ModernIconButton(
                                icon = Icons.Default.Delete,
                                onClick = { showDeleteDialog = true },
                                color = com.eyuphanaydin.discbase.ui.theme.StitchDefense,
                                contentDescription = stringResource(R.string.btn_delete)
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
                        text = { Text(stringResource(R.string.match_new_title).uppercase(), fontWeight = FontWeight.Bold) }
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
                            .background(com.eyuphanaydin.discbase.ui.theme.StitchPrimary.copy(alpha = 0.1f)),
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
                            color = if(wins >= losses) StitchOffense.copy(0.1f) else com.eyuphanaydin.discbase.ui.theme.StitchDefense.copy(0.1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = recordText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if(wins >= losses) StitchOffense else com.eyuphanaydin.discbase.ui.theme.StitchDefense,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Text(
                stringResource(R.string.pdf_match_results),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = StitchColor.TextPrimary,
                modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
            )

            if (standardMatches.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Sports, null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                        Spacer(Modifier.height(16.dp))
                        Text(stringResource(R.string.matches_empty), color = Color.Gray)
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

    // TAB BAŞLIKLARI (XML'den)
    val tabItems = listOf(
        stringResource(R.string.match_tab_summary), // "Sayı Özeti"
        stringResource(R.string.match_tab_team),    // "Takım İstatistikleri"
        stringResource(R.string.match_tab_player)   // "Oyuncu İstatistikleri"
    )
    val pagerState = rememberPagerState { tabItems.size }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // İstatistik Hesaplama (Değişmedi, sadece context korundu)
    fun calculateOverallStats(archive: List<PointData>): List<AdvancedPlayerStats> {
        val overallStatsMap = mutableMapOf<String, AdvancedPlayerStats>()
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

    // SİLME DİALOGU
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.match_delete_title)) },
            text = { Text(stringResource(R.string.match_delete_msg)) },
            confirmButton = {
                Button(
                    onClick = { onDeleteMatch(); showDeleteDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text(stringResource(R.string.btn_delete_confirm)) }
            },
            dismissButton = { Button(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.btn_cancel)) } }
        )
    }

    // OYUNCU DETAY PENCERESİ (GÜNCELLENDİ)
    if (selectedPlayerStats != null) {
        val advancedStats = selectedPlayerStats!!
        val stats = advancedStats.basicStats
        val playerInfo = rosterAsStats.find { it.id == stats.playerId }
        val isHandler = playerInfo?.position == "Handler" || playerInfo?.position == "Hybrid"
        var showEfficiencyInfo by remember { mutableStateOf(false) }

        if (showEfficiencyInfo) {
            EfficiencyDescriptionDialog(onDismiss = { showEfficiencyInfo = false })
        }

        AlertDialog(
            onDismissRequest = { selectedPlayerStats = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stats.name, fontWeight = FontWeight.Bold)
                        Text(
                            "${playerInfo?.position ?: stringResource(R.string.unknown)} | ${playerInfo?.gender ?: ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                    IconButton(onClick = { showEfficiencyInfo = true }) {
                        Icon(Icons.Default.Info, contentDescription = "Info", tint = com.eyuphanaydin.discbase.ui.theme.StitchPrimary)
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // GENEL ÖZET
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            // "Verimlilik (+/-)" -> XML
                            Text(stringResource(R.string.match_stat_efficiency), style = MaterialTheme.typography.labelMedium, color = Color.Gray)

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
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            // "Oynanan Sayı" -> XML
                            Text(stringResource(R.string.match_stat_played), style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                            Text("${stats.pointsPlayed}", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Text("O: ${advancedStats.oPointsPlayed} | D: ${advancedStats.dPointsPlayed}", fontSize = 12.sp, color = Color.Gray)
                        }
                    }

                    Divider()

                    val totalPassesCompleted = stats.successfulPass + stats.assist
                    val totalPassesAttempted = totalPassesCompleted + stats.throwaway
                    val passSuccessRate = calculateSafePercentage(totalPassesCompleted, totalPassesAttempted)

                    val totalSuccesfulCatches = stats.catchStat + stats.goal
                    val totalCatchesAttempted = totalSuccesfulCatches + stats.drop
                    val catchRate = calculateSafePercentage(totalSuccesfulCatches, totalCatchesAttempted)

                    // ATICILIK (THROWING) KARTI
                    val PassingSection = @Composable {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
                            Column(Modifier.padding(12.dp)) {
                                // "Atış (Throwing)" -> XML
                                Text(stringResource(R.string.match_stat_throwing), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.height(8.dp))
                                // "Başarı" -> XML
                                PerformanceStatRow(stringResource(R.string.home_pass_success), passSuccessRate.text, passSuccessRate.ratio, passSuccessRate.progress)
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("${stringResource(R.string.action_assist)}: ${stats.assist}")
                                    Text("${stringResource(R.string.action_turnover)}: ${stats.throwaway}", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // ALICILIK (RECEIVING) KARTI
                    val ReceivingSection = @Composable {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
                            Column(Modifier.padding(12.dp)) {
                                // "Tutuş (Receiving)" -> XML
                                Text(stringResource(R.string.match_stat_receiving), fontWeight = FontWeight.Bold, color = Color(0xFF009688))
                                Spacer(Modifier.height(8.dp))
                                // "Başarı" -> XML
                                PerformanceStatRow(stringResource(R.string.home_pass_success), catchRate.text, catchRate.ratio, catchRate.progress, progressColor = Color(0xFF009688))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("${stringResource(R.string.action_goal)}: ${stats.goal}")
                                    Text("${stringResource(R.string.action_drop)}: ${stats.drop}", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    if (isHandler) { PassingSection(); ReceivingSection() } else { ReceivingSection(); PassingSection() }

                    // SAVUNMA KARTI
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))) {
                        Column(Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                // "Savunma" -> XML
                                Text(stringResource(R.string.match_stat_defense), fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${stringResource(R.string.action_block)} (D):", fontWeight = FontWeight.Bold)
                                Text("${stats.block}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                            }
                            Divider(Modifier.padding(vertical = 4.dp))
                            Text("Pull: ${stats.successfulPulls} / ${stats.pullAttempts}", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
            },
            confirmButton = { Button(onClick = { selectedPlayerStats = null }) { Text(stringResource(R.string.btn_ok)) } }
        )
    }

    // ANA EKRAN YAPISI
    Scaffold(
        containerColor = StitchColor.Background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$ourTeamName vs $opponentName", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("${match.scoreUs} - ${match.scoreThem}", fontSize = 14.sp, color = if(match.scoreUs > match.scoreThem) StitchOffense else if(match.scoreThem > match.scoreUs) com.eyuphanaydin.discbase.ui.theme.StitchDefense else Color.Gray, fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    ModernIconButton(Icons.Default.ArrowBack, onBack, StitchTextPrimary, stringResource(R.string.desc_back))
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.shareMatchReport(context, match, context.getString(R.string.pdf_match_report_title), overallStats) }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = stringResource(R.string.match_share_report), tint = StitchColor.Primary)
                    }
                    if (isAdmin) {
                        ModernIconButton(Icons.Default.Edit, { navController.navigate("match_playback/$tournamentId/${match.opponentName}?matchId=${match.id}") },
                            com.eyuphanaydin.discbase.ui.theme.StitchPrimary, stringResource(R.string.btn_edit))
                        Spacer(Modifier.width(8.dp))
                        ModernIconButton(Icons.Default.Delete, { showDeleteDialog = true },
                            com.eyuphanaydin.discbase.ui.theme.StitchDefense, stringResource(R.string.btn_delete))
                        Spacer(Modifier.width(8.dp))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            TabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = StitchColor.Surface,
                contentColor = com.eyuphanaydin.discbase.ui.theme.StitchPrimary,
                indicator = { tabPositions ->
                    androidx.compose.material3.TabRowDefaults.Indicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                        color = com.eyuphanaydin.discbase.ui.theme.StitchPrimary,
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
                                color = if(pagerState.currentPage == index) com.eyuphanaydin.discbase.ui.theme.StitchPrimary else Color.Gray
                            )
                        }
                    )
                }
            }

            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize().weight(1f)) { pageIndex ->
                when (pageIndex) {
                    0 -> PointSummaryTab(navController, pointsArchive, tournamentId, match.id, isAdmin, onDeleteLastPoint)
                    1 -> TeamStatsTab(
                        teamStats = teamStats,
                        allPlayersStats = overallStats,
                        allPlayers = rosterAsStats,
                        matchDurationSeconds = match.matchDurationSeconds,
                        pointsArchive = pointsArchive
                    )
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
            stringResource(R.string.match_no_points),
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
                PointSummaryCard(
                    pointIndex = index,
                    pointData = pointData,
                    onViewClick = {
                        navController.navigate("point_detail_summary/$tournamentId/$matchId/$index")
                    },
                    onEditClick = {
                        navController.navigate("edit_point/$tournamentId/$matchId/$index")
                    },
                    showEditButton = isAdmin
                )
            }

            if (isAdmin) {
                item {
                    var showDeleteDialog by remember { mutableStateOf(false) }
                    if (showDeleteDialog) {
                        AlertDialog(
                            onDismissRequest = { showDeleteDialog = false },
                            title = { Text(stringResource(R.string.match_delete_last_title)) },
                            text = { Text(stringResource(R.string.match_delete_last_msg, pointsArchive.size)) },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        onDeleteLastPoint()
                                        showDeleteDialog = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                ) { Text(stringResource(R.string.btn_delete_confirm)) }
                            },
                            dismissButton = {
                                Button(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.btn_cancel)) }
                            }
                        )
                    }

                    Button(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.btn_delete_last_point))
                    }
                }
            }
        }
    }
}
@Composable
fun TeamStatsTab(
    teamStats: AdvancedTeamStats,
    allPlayersStats: List<AdvancedPlayerStats>,
    allPlayers: List<Player>,
    matchDurationSeconds: Long,
    pointsArchive: List<PointData>
) {
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
    val teamAvgPullTime = if (teamStats.totalPulls > 0)
        String.format("%.2f sn", teamStats.totalPullTimeSeconds.toDouble() / teamStats.totalPulls)
    else "0.00 sn"

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            TimeAnalysisCard(
                matchDurationSeconds = matchDurationSeconds,
                pointsArchive = pointsArchive
            )
        }
        item { PerformanceCard(holdRate, breakRate, passRate) }
        item { PossessionCard(conversionRate, blockConversionRate,cleanHoldRate) }
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

        item {
            // DEĞİŞİKLİK: "Pas Bağlantıları (Top 5)" -> XML
            Text(stringResource(R.string.stats_pass_network_header), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = StitchColor.TextPrimary)
        }

        val topPassers = allPlayersStats
            .sortedByDescending { it.basicStats.successfulPass + it.basicStats.assist }
            .take(5)
            .filter { (it.basicStats.successfulPass + it.basicStats.assist) > 0 }

        if (topPassers.isEmpty()) {
            item { Text(stringResource(R.string.stats_no_pass_data), color = Color.Gray) }
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
    pointDuration: Long = 0L
) {
    if (statsForPoint == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.no_data), color = Color.Gray)
        }
        return
    }

    val playersInPoint = statsForPoint.filter { it.pointsPlayed > 0 }

    var selectedPlayerStat by remember {
        mutableStateOf(playersInPoint.maxByOrNull { it.successfulPass + it.assist }
            ?: playersInPoint.firstOrNull())
    }

    Scaffold(
        containerColor = StitchColor.Background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    // "Sayı X Detayı" -> XML
                    val title = if (pointIndex != null) stringResource(R.string.point_detail_title, pointIndex + 1) else "Detay"
                    Text(title, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    ModernIconButton(
                        Icons.Default.ArrowBack,
                        { navController.popBackStack() },
                        StitchTextPrimary,
                        stringResource(R.string.desc_back)
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {

            // "Pas Ağını Görmek İçin Oyuncu Seçin" -> XML
            Text(
                stringResource(R.string.point_detail_select_player),
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
                                    if (isSelected) com.eyuphanaydin.discbase.ui.theme.StitchPrimary else Color.Transparent,
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
                            color = if (isSelected) com.eyuphanaydin.discbase.ui.theme.StitchPrimary else Color.Gray
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

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = StitchColor.Surface),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // "Sayı Özeti" -> XML
                                Text(stringResource(R.string.point_summary_title), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = StitchColor.TextPrimary)

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
                                    Text("Pas", fontSize = 11.sp, color = Color.Gray) // XML'de 'Pas' yoksa hardcoded kalabilir veya action_pass eklenmeli
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("$totalTurns", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = StitchDefense)
                                    Text(stringResource(R.string.action_turnover), fontSize = 11.sp, color = Color.Gray)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("$totalBlocks", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = StitchColor.Primary)
                                    Text(stringResource(R.string.action_block), fontSize = 11.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }

                if (selectedPlayerStat != null) {
                    val totalAction =
                        selectedPlayerStat!!.successfulPass + selectedPlayerStat!!.assist
                    item {
                        androidx.compose.runtime.key(selectedPlayerStat!!.playerId) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = StitchColor.Surface),
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(2.dp)
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    // "X Pas Trafiği" -> XML
                                    Text(
                                        stringResource(R.string.point_pass_traffic, selectedPlayerStat!!.name),
                                        fontWeight = FontWeight.Bold,
                                        color = StitchColor.TextPrimary,
                                        fontSize = 16.sp
                                    )

                                    if (totalAction == 0) {
                                        Text(
                                            stringResource(R.string.point_no_pass_data),
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
                    // "Tüm Oyuncu İstatistikleri" -> XML
                    Text(
                        stringResource(R.string.point_all_players),
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
                checkedTrackColor = com.eyuphanaydin.discbase.ui.theme.StitchPrimary
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
            colors = RadioButtonDefaults.colors(selectedColor = com.eyuphanaydin.discbase.ui.theme.StitchPrimary)
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
    userProfiles: Map<String, UserProfile>,
    currentUserId: String,
    onUpdateRole: (uid: String, newRole: String) -> Unit,
    onRemoveMember: (uid: String) -> Unit
) {
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
                stringResource(R.string.member_mgmt_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))

            if (sortedMembers.isEmpty()) {
                Text(stringResource(R.string.roster_empty))
            }

            sortedMembers.forEach { (uid, role) ->
                var showRoleMenu by remember { mutableStateOf(false) }

                if (uid != currentUserId) {
                    val userProfile = userProfiles[uid]
                    val displayName = userProfile?.displayName ?: stringResource(R.string.unknown)
                    val displayEmail = userProfile?.email ?: uid

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
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
                            Text(
                                text = when (role) {
                                    "pending" -> stringResource(R.string.role_pending)
                                    "member" -> stringResource(R.string.role_member)
                                    "admin" -> stringResource(R.string.role_captain)
                                    else -> role
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Normal,
                                color = if (role == "pending") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Box {
                            IconButton(onClick = { showRoleMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Manage Role")
                            }

                            androidx.compose.material3.DropdownMenu(
                                expanded = showRoleMenu,
                                onDismissRequest = { showRoleMenu = false }
                            ) {
                                if (role == "pending") {
                                    androidx.compose.material3.DropdownMenuItem(
                                        text = { Text(stringResource(R.string.menu_approve)) },
                                        onClick = {
                                            onUpdateRole(uid, "member")
                                            showRoleMenu = false
                                        }
                                    )
                                }
                                if (role == "member") {
                                    androidx.compose.material3.DropdownMenuItem(
                                        text = { Text(stringResource(R.string.menu_make_captain)) },
                                        onClick = {
                                            onUpdateRole(uid, "admin")
                                            showRoleMenu = false
                                        }
                                    )
                                }
                                if (role == "admin") {
                                    androidx.compose.material3.DropdownMenuItem(
                                        text = { Text(stringResource(R.string.menu_revoke)) },
                                        onClick = {
                                            onUpdateRole(uid, "member")
                                            showRoleMenu = false
                                        }
                                    )
                                }
                                androidx.compose.material3.DropdownMenuItem(
                                    text = {
                                        Text(
                                            stringResource(R.string.menu_kick),
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

    var isFullScreen by remember { mutableStateOf(false) }

    val dynamicBottomNavItems = remember(isProModeEnabled) {
        val items = mutableListOf(
            Screen.Home,
            Screen.Tournaments,
            Screen.Roster,
            Screen.Trainings
        )
        if (isProModeEnabled) {
            items.add(Screen.ProMode)
        }
        items
    }

    val hideGlobalTopBar = currentRoute == Screen.Home.route ||
            currentRoute == Screen.Trainings.route ||
            isFullScreen

    Scaffold(
        containerColor = StitchColor.Background,
        topBar = {
            if (!hideGlobalTopBar) {
                CenterAlignedTopAppBar(
                    title = {
                        // BAŞLIK GÜNCELLEMESİ
                        Text(
                            text = when (currentRoute) {
                                Screen.Roster.route -> stringResource(R.string.nav_roster)
                                Screen.Tournaments.route -> stringResource(R.string.tour_list_title)
                                else -> teamProfile.teamName
                            },
                            fontWeight = FontWeight.Bold,
                            color = StitchColor.TextPrimary
                        )
                    },
                    actions = {
                        val hasNotification by mainViewModel.hasPendingRequests.collectAsState()

                        androidx.compose.material3.BadgedBox(
                            badge = {
                                if (hasNotification) {
                                    androidx.compose.material3.Badge(
                                        containerColor = com.eyuphanaydin.discbase.ui.theme.StitchDefense,
                                        contentColor = Color.White
                                    )
                                }
                            },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            ModernIconButton(
                                icon = Icons.Default.Person,
                                onClick = { topLevelNavController.navigate("profile_edit") },
                                color = com.eyuphanaydin.discbase.ui.theme.StitchPrimary,
                                contentDescription = stringResource(R.string.desc_profile)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = StitchColor.Background
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

                            // ALT MENÜ ETİKETLERİ GÜNCELLEMESİ
                            val labelText = when(screen.route) {
                                Screen.Home.route -> stringResource(R.string.nav_home)
                                Screen.Tournaments.route -> stringResource(R.string.nav_tournaments)
                                Screen.Roster.route -> stringResource(R.string.nav_roster)
                                Screen.Trainings.route -> stringResource(R.string.nav_trainings)
                                Screen.ProMode.route -> stringResource(R.string.nav_pro_mode)
                                else -> screen.title
                            }

                            NavigationBarItem(
                                icon = { Icon(screen.icon, contentDescription = labelText, modifier = Modifier.size(24.dp)) },
                                label = { Text(labelText, fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
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

                ProModeHost(
                    mainViewModel = mainViewModel,
                    tournaments = tournamentsList,
                    allPlayers = allPlayers,
                    onFullScreenToggle = { fullScreen -> isFullScreen = fullScreen }
                )
            }
        }
    }
}
@Composable
fun TimeAnalysisCard(
    matchDurationSeconds: Long,
    pointsArchive: List<PointData>
) {
    val allStoppages = pointsArchive.flatMap { it.stoppages }
    val totalStoppageSeconds = allStoppages.sumOf { it.durationSeconds }
    val totalWallClockSeconds = matchDurationSeconds + totalStoppageSeconds
    val activeRatio = if (totalWallClockSeconds > 0) matchDurationSeconds.toFloat() / totalWallClockSeconds else 0f
    val pullTimes = pointsArchive.map { it.pullDurationSeconds }.filter { it > 0 }
    val avgPullTime = if (pullTimes.isNotEmpty()) pullTimes.average() else 0.0
    val timeoutCount = allStoppages.count { it.type == StoppageType.TIMEOUT }
    val callCount = allStoppages.count { it.type == StoppageType.CALL }
    val injuryCount = allStoppages.count { it.type == StoppageType.INJURY }

    if (totalWallClockSeconds == 0L) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = StitchColor.Surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccessTime, null, tint = StitchColor.Primary)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.time_analysis_title), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = StitchColor.TextPrimary)
            }
            Divider(Modifier.padding(vertical = 12.dp).alpha(0.1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("${stringResource(R.string.time_net_game)}: ${formatSecondsToTime(matchDurationSeconds)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = StitchOffense)
                Text("${stringResource(R.string.time_stoppage)}: ${formatSecondsToTime(totalStoppageSeconds)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = StitchDefense)
            }
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(50))
                    .background(StitchDefense)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(activeRatio)
                        .fillMaxHeight()
                        .background(StitchOffense)
                )
            }

            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Sports, null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.time_avg_pull) + " ", fontSize = 13.sp, color = StitchColor.TextPrimary)
                Text(
                    String.format("%.2f sn", avgPullTime),
                    fontWeight = FontWeight.Bold,
                    color = StitchColor.Primary
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StoppageCountBadge(stringResource(R.string.stoppage_timeout), timeoutCount, Color(0xFFFFA000))
                StoppageCountBadge(stringResource(R.string.stoppage_call), callCount, Color(0xFF5D4037))
                StoppageCountBadge(stringResource(R.string.stoppage_injury), injuryCount, StitchDefense)
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
@Composable
fun LanguageSelector() {
    // Mevcut dili anlamak için
    val currentLocale = AppCompatDelegate.getApplicationLocales().toLanguageTags()
    val isEnglish = currentLocale.contains("en")

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            // Hata almamak için stringResource import edildiğinden emin ol
            text = stringResource(id = language_option),
            // Material 3'te h6 yoktur, titleLarge veya titleMedium kullanılır
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row {
            // TÜRKÇE BUTONU
            Button(
                onClick = {
                    // Utils.kt içindeki fonksiyonu çağırıyoruz
                    changeAppLanguage("tr")
                },
                colors = ButtonDefaults.buttonColors(
                    // DİKKAT: Material 3'te backgroundColor YERİNE containerColor kullanılır
                    containerColor = if (!isEnglish) Color.Gray else Color.LightGray
                ),
                modifier = Modifier.weight(1f).padding(end = 8.dp)
            ) {
                Text("Türkçe", color = Color.White)
            }

            // İNGİLİZCE BUTONU
            Button(
                onClick = {
                    changeAppLanguage("en")
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isEnglish) Color.Gray else Color.LightGray
                ),
                modifier = Modifier.weight(1f).padding(start = 8.dp)
            ) {
                Text("English", color = Color.White)
            }
        }
    }
}