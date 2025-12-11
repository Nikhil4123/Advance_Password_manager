package com.masterpushers.pass_assist.ui

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.masterpushers.pass_assist.data.PasswordEntity
import com.masterpushers.pass_assist.ui.theme.ButtonBlack
import com.masterpushers.pass_assist.ui.theme.CardLight
import com.masterpushers.pass_assist.ui.theme.DangerRed
import com.masterpushers.pass_assist.utils.PasswordGenerator
import com.masterpushers.pass_assist.utils.PasswordGeneratorOptions
import com.masterpushers.pass_assist.utils.PasswordStrengthCalculator
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: PasswordViewModel = viewModel(
        factory = PasswordViewModel.Factory(LocalContext.current.applicationContext as Application)
    )
) {
    val passwords by viewModel.allPasswords.observeAsState(emptyList())
    val decryptedPasswords by viewModel.decryptedPasswords.observeAsState(emptyMap())
    val addSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val detailSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showAddSheet by remember { mutableStateOf(false) }
    var showDetailSheet by remember { mutableStateOf(false) }
    var selectedPassword by remember { mutableStateOf<PasswordEntity?>(null) }
    var editingPassword by remember { mutableStateOf<PasswordEntity?>(null) }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Password Manager",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingPassword = null
                    showAddSheet = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.size(72.dp)
            ) {
                Icon(
                    Icons.Default.Add, 
                    contentDescription = "Add Password",
                    modifier = Modifier.size(32.dp)
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            if (passwords.isEmpty()) {
                EmptyState()
            } else {
                PasswordList(
                    passwords = passwords,
                    decryptedPasswords = decryptedPasswords,
                    onPasswordClick = { password ->
                        selectedPassword = password
                        showDetailSheet = true
                    },
                    onDecryptPassword = { id, encryptedPassword ->
                        val decrypted = viewModel.decryptPassword(encryptedPassword)
                        viewModel.updateDecryptedPassword(id, decrypted)
                    }
                )
            }
        }
    }

    if (showAddSheet) {
        AddAccountModal(
            sheetState = addSheetState,
            onDismissRequest = { showAddSheet = false },
            passwordToEdit = editingPassword,
            onSave = { accountType, username, password ->
                coroutineScope.launch {
                    if (editingPassword == null) {
                        viewModel.insert(accountType, username, password)
                    } else {
                        viewModel.update(editingPassword!!.id, accountType, username, password)
                    }
                    showAddSheet = false
                    editingPassword = null
                }
            }
        )
    }

    if (showDetailSheet && selectedPassword != null) {
        val decrypted = decryptedPasswords[selectedPassword!!.id]
        AccountDetailsModal(
            sheetState = detailSheetState,
            password = selectedPassword!!,
            decryptedPassword = decrypted,
            onDismissRequest = {
                showDetailSheet = false
                selectedPassword = null
            },
            onRevealPassword = { entity ->
                val value = viewModel.decryptPassword(entity.password)
                viewModel.updateDecryptedPassword(entity.id, value)
            },
            onDelete = { entity ->
                viewModel.delete(entity.id)
                showDetailSheet = false
                selectedPassword = null
            },
            onEdit = { entity ->
                editingPassword = entity
                showDetailSheet = false
                coroutineScope.launch {
                    addSheetState.hide()
                    detailSheetState.hide()
                }.invokeOnCompletion {
                    showAddSheet = true
                }
            }
        )
    }
}

@Composable
fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "Lock",
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "No passwords saved yet",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Tap the + button to add your first password",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun PasswordList(
    passwords: List<PasswordEntity>,
    @Suppress("UNUSED_PARAMETER") decryptedPasswords: Map<Long, String>,
    onPasswordClick: (PasswordEntity) -> Unit,
    @Suppress("UNUSED_PARAMETER") onDecryptPassword: (Long, String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 20.dp)
    ) {
        items(passwords) { password ->
            PasswordItemCard(
                password = password,
                maskedPassword = "*******",
                onClick = { onPasswordClick(password) },
                onDecryptPassword = { onDecryptPassword(password.id, password.password) }
            )
        }
    }
}

@Composable
fun PasswordItemCard(
    password: PasswordEntity,
    maskedPassword: String,
    onClick: () -> Unit,
    @Suppress("UNUSED_PARAMETER") onDecryptPassword: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = CardLight
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = password.accountType,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = maskedPassword,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Open",
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountModal(
    sheetState: androidx.compose.material3.SheetState,
    onDismissRequest: () -> Unit,
    passwordToEdit: PasswordEntity?,
    onSave: (String, String, String) -> Unit
) {
    var accountType by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showGeneratorSheet by remember { mutableStateOf(false) }
    val generatorSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(passwordToEdit) {
        if (passwordToEdit != null) {
            accountType = passwordToEdit.accountType
            username = passwordToEdit.username
            errorMessage = null
        } else {
            accountType = ""
            username = ""
            password = ""
            errorMessage = null
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        dragHandle = { Spacer(modifier = Modifier.height(12.dp)) },
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp, vertical = 16.dp)
        ) {
            Text(
                text = if (passwordToEdit == null) "Add New Account" else "Edit Account",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(bottom = 24.dp)
            )

            AuthTextField(
                value = accountType,
                onValueChange = { accountType = it },
                placeholder = "Account Name"
            )

            Spacer(modifier = Modifier.height(16.dp))

            AuthTextField(
                value = username,
                onValueChange = { username = it },
                placeholder = "Username/ Email"
            )

            Spacer(modifier = Modifier.height(16.dp))

            AuthTextField(
                value = password,
                onValueChange = { password = it },
                placeholder = "Password",
                isPassword = true
            )
            
            // Password Strength Meter
            if (password.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                PasswordStrengthMeter(password = password)
            }
            
            // Generate Password Button
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = { showGeneratorSheet = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Generate Password",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Generate Strong Password",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            errorMessage?.let {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = {
                    errorMessage = validateInputs(accountType, username, password, password)
                    if (errorMessage == null) {
                        onSave(accountType, username, password)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(30.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = ButtonBlack,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = if (passwordToEdit == null) "Add New Account" else "Update Account",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
    
    // Password Generator Bottom Sheet
    if (showGeneratorSheet) {
        PasswordGeneratorSheet(
            sheetState = generatorSheetState,
            onDismissRequest = { 
                coroutineScope.launch {
                    generatorSheetState.hide()
                }.invokeOnCompletion {
                    showGeneratorSheet = false
                }
            },
            onPasswordGenerated = { generatedPassword ->
                password = generatedPassword
                coroutineScope.launch {
                    generatorSheetState.hide()
                }.invokeOnCompletion {
                    showGeneratorSheet = false
                }
            }
        )
    }
}

@Composable
private fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isPassword: Boolean = false
) {
    var isPasswordVisible by remember { mutableStateOf(false) }
    androidx.compose.material3.OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        placeholder = { Text(placeholder, style = MaterialTheme.typography.bodyLarge) },
        textStyle = MaterialTheme.typography.bodyLarge,
        shape = RoundedCornerShape(16.dp),
        singleLine = true,
        visualTransformation = if (isPassword && !isPasswordVisible) androidx.compose.ui.text.input.PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        trailingIcon = {
            if (isPassword) {
                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                    Icon(
                        imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (isPasswordVisible) "Hide password" else "Show password",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailsModal(
    sheetState: androidx.compose.material3.SheetState,
    password: PasswordEntity,
    decryptedPassword: String?,
    onDismissRequest: () -> Unit,
    onRevealPassword: (PasswordEntity) -> Unit,
    onDelete: (PasswordEntity) -> Unit,
    onEdit: (PasswordEntity) -> Unit
) {
    var isPasswordVisible by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        dragHandle = { Spacer(modifier = Modifier.height(12.dp)) },
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp, vertical = 16.dp)
        ) {
            Text(
                text = "Account Details",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.padding(bottom = 24.dp)
            )

            DetailItem(label = "Account Type", value = password.accountType)
            Spacer(modifier = Modifier.height(16.dp))
            DetailItem(label = "Username/ Email", value = password.username)
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Password",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (decryptedPassword != null) {
                        Text(
                            text = if (isPasswordVisible) decryptedPassword else "••••••••",
                            style = MaterialTheme.typography.titleMedium
                        )
                    } else {
                        Text(
                            text = "Hidden",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                IconButton(
                    onClick = {
                        if (decryptedPassword == null) {
                            onRevealPassword(password)
                        }
                        isPasswordVisible = !isPasswordVisible
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = if (isPasswordVisible && decryptedPassword != null) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Toggle password visibility",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = { onEdit(password) },
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = ButtonBlack,
                        contentColor = Color.White
                    )
                ) {
                    Text("Edit", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold))
                }
                Button(
                    onClick = { onDelete(password) },
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = DangerRed,
                        contentColor = Color.White
                    )
                ) {
                    Text("Delete", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            TextButton(
                onClick = onDismissRequest,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Close", 
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun DetailItem(label: String, value: String) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium
        )
    }
}

/**
 * Password Strength Meter Component
 */
@Composable
fun PasswordStrengthMeter(password: String) {
    val strength = remember(password) {
        PasswordStrengthCalculator.calculateStrength(password)
    }
    
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Password Strength:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Text(
                text = strength.label,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = strength.color
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        LinearProgressIndicator(
            progress = { strength.progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = strength.color,
            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
        )
        
        if (strength.feedback.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            strength.feedback.forEach { suggestion ->
                Text(
                    text = "• $suggestion",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

/**
 * Password Generator Bottom Sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordGeneratorSheet(
    sheetState: androidx.compose.material3.SheetState,
    onDismissRequest: () -> Unit,
    onPasswordGenerated: (String) -> Unit
) {
    var passwordLength by remember { mutableStateOf(16f) }
    var includeLowercase by remember { mutableStateOf(true) }
    var includeUppercase by remember { mutableStateOf(true) }
    var includeDigits by remember { mutableStateOf(true) }
    var includeSpecialChars by remember { mutableStateOf(true) }
    var excludeAmbiguous by remember { mutableStateOf(true) }
    var generatedPassword by remember { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current
    
    // Generate initial password
    LaunchedEffect(Unit) {
        generatedPassword = PasswordGenerator.generate(
            PasswordGeneratorOptions(
                length = passwordLength.roundToInt(),
                includeLowercase = includeLowercase,
                includeUppercase = includeUppercase,
                includeDigits = includeDigits,
                includeSpecialChars = includeSpecialChars,
                excludeAmbiguous = excludeAmbiguous
            )
        )
    }
    
    // Regenerate when options change
    LaunchedEffect(passwordLength, includeLowercase, includeUppercase, includeDigits, includeSpecialChars, excludeAmbiguous) {
        if (includeLowercase || includeUppercase || includeDigits || includeSpecialChars) {
            generatedPassword = PasswordGenerator.generate(
                PasswordGeneratorOptions(
                    length = passwordLength.roundToInt(),
                    includeLowercase = includeLowercase,
                    includeUppercase = includeUppercase,
                    includeDigits = includeDigits,
                    includeSpecialChars = includeSpecialChars,
                    excludeAmbiguous = excludeAmbiguous
                )
            )
        }
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        dragHandle = { Spacer(modifier = Modifier.height(12.dp)) },
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp, vertical = 16.dp)
        ) {
            Text(
                text = "Generate Password",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(bottom = 24.dp)
            )
            
            // Generated Password Display
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardLight),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = generatedPassword,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(generatedPassword))
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    IconButton(
                        onClick = {
                            generatedPassword = PasswordGenerator.generate(
                                PasswordGeneratorOptions(
                                    length = passwordLength.roundToInt(),
                                    includeLowercase = includeLowercase,
                                    includeUppercase = includeUppercase,
                                    includeDigits = includeDigits,
                                    includeSpecialChars = includeSpecialChars,
                                    excludeAmbiguous = excludeAmbiguous
                                )
                            )
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Regenerate",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Password Length Slider
            Text(
                text = "Length: ${passwordLength.roundToInt()}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Slider(
                value = passwordLength,
                onValueChange = { passwordLength = it },
                valueRange = 8f..32f,
                steps = 23,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Character Type Options
            Text(
                text = "Include:",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            CheckboxOption(
                checked = includeLowercase,
                onCheckedChange = { includeLowercase = it },
                label = "Lowercase (a-z)"
            )
            CheckboxOption(
                checked = includeUppercase,
                onCheckedChange = { includeUppercase = it },
                label = "Uppercase (A-Z)"
            )
            CheckboxOption(
                checked = includeDigits,
                onCheckedChange = { includeDigits = it },
                label = "Numbers (0-9)"
            )
            CheckboxOption(
                checked = includeSpecialChars,
                onCheckedChange = { includeSpecialChars = it },
                label = "Special Characters (!@#$...)"
            )
            CheckboxOption(
                checked = excludeAmbiguous,
                onCheckedChange = { excludeAmbiguous = it },
                label = "Exclude Ambiguous (0, O, 1, l, I)"
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Use Password Button
            Button(
                onClick = { onPasswordGenerated(generatedPassword) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(30.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ButtonBlack,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "Use This Password",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun CheckboxOption(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

/**
 * Validates the input fields for adding/editing a password
 */
fun validateInputs(
    accountType: String,
    username: String,
    password: String,
    confirmPassword: String
): String? {
    if (accountType.isBlank()) {
        return "Account type is required"
    }
    
    if (username.isBlank()) {
        return "Username/Email is required"
    }
    
    if (password.isBlank()) {
        return "Password is required"
    }
    
    if (password != confirmPassword) {
        return "Passwords do not match"
    }
    
    if (password.length < 6) {
        return "Password must be at least 6 characters"
    }
    
    return null
}