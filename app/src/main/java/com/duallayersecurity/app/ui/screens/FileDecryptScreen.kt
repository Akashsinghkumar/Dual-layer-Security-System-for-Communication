package com.duallayersecurity.app.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.duallayersecurity.app.ui.viewmodels.CryptoStegoViewModel
import com.duallayersecurity.app.utils.FileUtils
import com.duallayersecurity.app.utils.ImageUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileDecryptScreen(navController: NavController, viewModel: CryptoStegoViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var stegoImage by remember { mutableStateOf<Bitmap?>(null) }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var extractedFileName by remember { mutableStateOf<String?>(null) }
    var extractedFileSize by remember { mutableStateOf(0) }
    var fileSaved by remember { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsState()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            stegoImage = ImageUtils.loadBitmapFromUri(context, it)
            extractedFileName = null
            fileSaved = false
        }
    }

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is CryptoStegoViewModel.UiState.FileExtractSuccess -> {
                extractedFileName = state.extractedFile.fileName
                extractedFileSize = state.extractedFile.fileBytes.size
                snackbarHostState.showSnackbar("✓ File '${state.extractedFile.fileName}' extracted successfully!")
            }
            is CryptoStegoViewModel.UiState.Error -> {
                snackbarHostState.showSnackbar("✗ Error: ${state.message}")
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, null, modifier = Modifier.size(20.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Extract Hidden File", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF0F172A), Color(0xFF1E293B))))
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Step 1: Select Stego Image
            StepCard(stepNumber = 1, title = "Select Image with Hidden File", icon = Icons.Default.Add) {
                FilledTonalButton(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color(0xFF3B82F6).copy(alpha = 0.1f),
                        contentColor = Color(0xFF3B82F6)
                    )
                ) {
                    Icon(Icons.Default.Add, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Choose Image from Gallery", fontWeight = FontWeight.Bold)
                }

                AnimatedVisibility(visible = stegoImage != null, enter = fadeIn() + expandVertically()) {
                    stegoImage?.let { bitmap ->
                        Column {
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                                    .border(1.dp, Color(0xFF3B82F6).copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                            ) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(), contentDescription = "Stego Image",
                                    modifier = Modifier.fillMaxWidth().height(180.dp),
                                    contentScale = ContentScale.Crop
                                )
                                Surface(
                                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF1E293B).copy(alpha = 0.85f)
                                ) {
                                    Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp), tint = Color(0xFF10B981))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("${bitmap.width}×${bitmap.height}", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Step 2: Password
            AnimatedVisibility(visible = stegoImage != null, enter = fadeIn() + expandVertically()) {
                StepCard(stepNumber = 2, title = "Enter Decryption Password", icon = Icons.Default.Lock) {
                    OutlinedTextField(
                        value = password, onValueChange = { password = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Enter the password to decrypt...", color = Color(0xFF64748B)) },
                        leadingIcon = { Icon(Icons.Default.Lock, null, tint = Color(0xFF8B5CF6)) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Default.List else Icons.Default.Lock,
                                    null, tint = Color(0xFF94A3B8)
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        shape = RoundedCornerShape(12.dp), singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF8B5CF6), unfocusedBorderColor = Color(0xFF475569),
                            focusedTextColor = Color.White, unfocusedTextColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Step 3: Extract Button
            AnimatedVisibility(
                visible = stegoImage != null && password.isNotBlank(),
                enter = fadeIn() + slideInVertically()
            ) {
                StepCard(stepNumber = 3, title = "Extract Hidden File", icon = Icons.Default.Lock) {
                    Button(
                        onClick = {
                            stegoImage?.let { img ->
                                if (password.isNotBlank()) {
                                    viewModel.extractFile(img, password)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = uiState !is CryptoStegoViewModel.UiState.Loading,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
                    ) {
                        if (uiState is CryptoStegoViewModel.UiState.Loading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Extracting & Decrypting...", color = Color.White)
                        } else {
                            Icon(Icons.Default.Lock, null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Extract & Decrypt File", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Step 4: Result
            AnimatedVisibility(visible = extractedFileName != null, enter = fadeIn() + expandVertically()) {
                val currentState = uiState
                if (currentState is CryptoStegoViewModel.UiState.FileExtractSuccess) {
                    Column {
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth().border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp)),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
                            elevation = CardDefaults.cardElevation(0.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFF10B981).copy(alpha = 0.1f),
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Check, null, tint = Color(0xFF10B981), modifier = Modifier.size(28.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text("File Recovered", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text("Decryption successful", style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                Divider(color = Color.White.copy(alpha = 0.08f))
                                Spacer(modifier = Modifier.height(16.dp))

                                // File info card
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF8B5CF6).copy(alpha = 0.08f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.2f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Info, null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(36.dp))
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                currentState.extractedFile.fileName,
                                                color = Color.White, fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                            Text(
                                                FileUtils.formatFileSize(currentState.extractedFile.fileBytes.size.toLong()),
                                                color = Color(0xFF94A3B8),
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Save to Downloads
                                FilledTonalButton(
                                    onClick = {
                                        if (!fileSaved) {
                                            val savedUri = FileUtils.saveFileToDownloads(
                                                context,
                                                currentState.extractedFile.fileName,
                                                currentState.extractedFile.fileBytes
                                            )
                                            if (savedUri != null) {
                                                fileSaved = true
                                                scope.launch {
                                                    snackbarHostState.showSnackbar("✓ File saved to Downloads!")
                                                }
                                            } else {
                                                scope.launch {
                                                    snackbarHostState.showSnackbar("✗ Failed to save file")
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = if (fileSaved) Color(0xFF10B981).copy(alpha = 0.1f) else Color(0xFF3B82F6).copy(alpha = 0.1f),
                                        contentColor = if (fileSaved) Color(0xFF10B981) else Color(0xFF3B82F6)
                                    )
                                ) {
                                    Icon(
                                        if (fileSaved) Icons.Default.Check else Icons.Default.ArrowForward,
                                        null
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        if (fileSaved) "Saved to Downloads ✓" else "Save File to Downloads",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
