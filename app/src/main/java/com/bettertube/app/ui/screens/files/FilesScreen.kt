package com.bettertube.app.ui.screens.files

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import com.bettertube.app.domain.model.DownloadTask
import com.bettertube.app.domain.model.MediaType
import com.bettertube.app.domain.model.VaultItem
import com.bettertube.app.domain.model.VaultState
import com.bettertube.app.R
import com.bettertube.app.ui.theme.BgSecondary
import com.bettertube.app.ui.theme.BrandPrimary
import com.bettertube.app.ui.theme.SurfaceDark
import com.bettertube.app.ui.theme.TextSecondary
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FilesScreen(
    modifier: Modifier = Modifier,
    viewModel: FilesViewModel = hiltViewModel()
) {
    val vaultState by viewModel.vaultState.collectAsStateWithLifecycle()
    val filteredItems by viewModel.filteredItems.collectAsStateWithLifecycle()
    val totalVaultItems by viewModel.vaultItems.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val pinInput by viewModel.pinInput.collectAsStateWithLifecycle()
    val confirmPinInput by viewModel.confirmPinInput.collectAsStateWithLifecycle()
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val showChangePinDialog by viewModel.showChangePinDialog.collectAsStateWithLifecycle()
    val showImportDialog by viewModel.showImportDialog.collectAsStateWithLifecycle()
    val completedDownloads by viewModel.completedDownloads.collectAsStateWithLifecycle()

    val currentPinInput by viewModel.currentPinInput.collectAsStateWithLifecycle()
    val newPinInput by viewModel.newPinInput.collectAsStateWithLifecycle()
    val confirmNewPinInput by viewModel.confirmNewPinInput.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val snackbarHostState = remember { SnackbarHostState() }

    var itemToDelete by remember { mutableStateOf<VaultItem?>(null) }
    var itemToRestore by remember { mutableStateOf<VaultItem?>(null) }

    LaunchedEffect(Unit) {
        viewModel.snackbarEvent.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize().testTag("files_vault_screen")
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (vaultState) {
                VaultState.UNINITIALIZED -> {
                    VaultSetupView(
                        pinInput = pinInput,
                        confirmPinInput = confirmPinInput,
                        isBiometricSupported = viewModel.isBiometricSupported,
                        isBiometricEnabled = isBiometricEnabled,
                        isLoading = isLoading,
                        onPinChange = viewModel::onPinInputChange,
                        onConfirmPinChange = viewModel::onConfirmPinInputChange,
                        onToggleBiometrics = viewModel::toggleBiometric,
                        onSubmit = { viewModel.initializeVault(isBiometricEnabled) }
                    )
                }

                VaultState.LOCKED -> {
                    VaultLockedView(
                        pinInput = pinInput,
                        isBiometricSupported = viewModel.isBiometricSupported,
                        isBiometricEnabled = isBiometricEnabled,
                        isLoading = isLoading,
                        onPinChange = viewModel::onPinInputChange,
                        onUnlockPin = viewModel::unlockWithPin,
                        onUnlockBiometrics = {
                            activity?.let { viewModel.unlockWithBiometrics(it) }
                        }
                    )
                }

                VaultState.UNLOCKED -> {
                    VaultUnlockedView(
                        items = filteredItems,
                        totalCount = totalVaultItems.size,
                        selectedFilter = selectedFilter,
                        searchQuery = searchQuery,
                        isLoading = isLoading,
                        onFilterSelected = viewModel::onFilterSelected,
                        onSearchChange = viewModel::onSearchQueryChange,
                        onLockClick = viewModel::lockVault,
                        onChangePinClick = { viewModel.setShowChangePinDialog(true) },
                        onImportClick = { viewModel.setShowImportDialog(true) },
                        onRestoreItem = { itemToRestore = it },
                        onDeleteItem = { itemToDelete = it }
                    )
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = BrandPrimary,
                        modifier = Modifier.testTag("vault_loading_indicator")
                    )
                }
            }
        }
    }

    // Change PIN Dialog
    if (showChangePinDialog) {
        ChangePinDialog(
            currentPin = currentPinInput,
            newPin = newPinInput,
            confirmNewPin = confirmNewPinInput,
            onCurrentPinChange = viewModel::onCurrentPinInputChange,
            onNewPinChange = viewModel::onNewPinInputChange,
            onConfirmNewPinChange = viewModel::onConfirmNewPinInputChange,
            onDismiss = { viewModel.setShowChangePinDialog(false) },
            onConfirm = viewModel::changePin
        )
    }

    // Import from Downloads Dialog
    if (showImportDialog) {
        ImportFromDownloadsDialog(
            downloads = completedDownloads,
            onDismiss = { viewModel.setShowImportDialog(false) },
            onImport = { task ->
                viewModel.importDownloadToVault(task)
                viewModel.setShowImportDialog(false)
            }
        )
    }

    // Restore Confirm Dialog
    itemToRestore?.let { item ->
        AlertDialog(
            onDismissRequest = { itemToRestore = null },
            title = { Text(stringResource(R.string.restore_file_title)) },
            text = { Text("Are you sure you want to decrypt \"${item.fileName}\" and restore it to your Downloads directory?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.removeFromVault(item)
                        itemToRestore = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
                ) {
                    Text(stringResource(R.string.restore_item), color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToRestore = null }) {
                    Text("Cancel")
                }
            },
            modifier = Modifier.testTag("vault_restore_dialog")
        )
    }

    // Delete Confirm Dialog
    itemToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text(stringResource(R.string.permanently_delete)) },
            text = { Text("This will permanently delete \"${item.fileName}\" from your encrypted vault. This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePermanently(item)
                        itemToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.delete_item), color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Cancel")
                }
            },
            modifier = Modifier.testTag("vault_delete_dialog")
        )
    }
}

@Composable
private fun VaultSetupView(
    pinInput: String,
    confirmPinInput: String,
    isBiometricSupported: Boolean,
    isBiometricEnabled: Boolean,
    isLoading: Boolean,
    onPinChange: (String) -> Unit,
    onConfirmPinChange: (String) -> Unit,
    onToggleBiometrics: (Boolean) -> Unit,
    onSubmit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(BrandPrimary.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = BrandPrimary,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Set Up Secure Vault",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Create a 4–8 digit PIN to encrypt and protect your downloaded videos and audio with hardware-backed AES-256-GCM encryption.",
            fontSize = 14.sp,
            color = TextSecondary,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = pinInput,
            onValueChange = onPinChange,
            label = { Text(stringResource(R.string.vault_setup_pin_label)) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Next),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("vault_setup_pin"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BrandPrimary,
                cursorColor = BrandPrimary
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = confirmPinInput,
            onValueChange = onConfirmPinChange,
            label = { Text(stringResource(R.string.vault_setup_confirm_label)) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onSubmit() }),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("vault_setup_confirm_pin"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BrandPrimary,
                cursorColor = BrandPrimary
            )
        )

        if (isBiometricSupported) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Enable Biometric Unlock",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                Switch(
                    checked = isBiometricEnabled,
                    onCheckedChange = onToggleBiometrics,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = BrandPrimary
                    ),
                    modifier = Modifier.testTag("vault_setup_biometric_switch")
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onSubmit,
            enabled = pinInput.length >= 4 && confirmPinInput.length >= 4 && !isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("vault_setup_submit")
        ) {
            Text(stringResource(R.string.vault_setup_create), color = Color.Black, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun VaultLockedView(
    pinInput: String,
    isBiometricSupported: Boolean,
    isBiometricEnabled: Boolean,
    isLoading: Boolean,
    onPinChange: (String) -> Unit,
    onUnlockPin: () -> Unit,
    onUnlockBiometrics: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(BrandPrimary.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = BrandPrimary,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Vault Locked",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Enter your secure PIN to access protected files.",
            fontSize = 14.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = pinInput,
            onValueChange = onPinChange,
            label = { Text(stringResource(R.string.vault_locked_pin_label)) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onUnlockPin() }),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("vault_pin_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BrandPrimary,
                cursorColor = BrandPrimary
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onUnlockPin,
            enabled = pinInput.isNotBlank() && !isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("vault_unlock_button")
        ) {
            Text(stringResource(R.string.vault_locked_unlock), color = Color.Black, fontWeight = FontWeight.SemiBold)
        }

        if (isBiometricSupported && isBiometricEnabled) {
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onUnlockBiometrics,
                colors = ButtonDefaults.outlinedButtonColors(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("vault_biometric_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = null,
                    tint = BrandPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Unlock with Biometrics", color = MaterialTheme.colorScheme.onBackground)
            }
        }
    }
}

@Composable
private fun VaultUnlockedView(
    items: List<VaultItem>,
    totalCount: Int,
    selectedFilter: String,
    searchQuery: String,
    isLoading: Boolean,
    onFilterSelected: (String) -> Unit,
    onSearchChange: (String) -> Unit,
    onLockClick: () -> Unit,
    onChangePinClick: () -> Unit,
    onImportClick: () -> Unit,
    onRestoreItem: (VaultItem) -> Unit,
    onDeleteItem: (VaultItem) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val inactiveChipBg = if (isDark) SurfaceDark else BgSecondary
    val filters = listOf("All", "Videos", "Audio")

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("vault_items_list"),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            // Top Bar
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Secure Vault",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(BrandPrimary.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "$totalCount items",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = BrandPrimary
                                )
                            }
                        }
                        Text(
                            text = "Hardware encrypted with AES-256",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }

                    Row {
                        IconButton(
                            onClick = onChangePinClick,
                            modifier = Modifier.testTag("vault_change_pin_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = "Change PIN",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        IconButton(
                            onClick = onLockClick,
                            modifier = Modifier.testTag("vault_lock_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Lock Vault",
                                tint = BrandPrimary
                            )
                        }
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text("Search encrypted files…", color = TextSecondary) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = TextSecondary
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .testTag("vault_search_bar"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandPrimary,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = inactiveChipBg,
                        unfocusedContainerColor = inactiveChipBg
                    )
                )
            }

            // Filter Chips
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filters.forEach { filter ->
                        val isSelected = selectedFilter == filter
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) BrandPrimary else inactiveChipBg)
                                .clickable { onFilterSelected(filter) }
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .testTag("vault_filter_$filter")
                        ) {
                            Text(
                                text = filter,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Vault Items
            if (items.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 48.dp, start = 24.dp, end = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = TextSecondary.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "No matching encrypted files" else "Your Vault is Empty",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap the + button below to import and encrypt downloaded videos or songs into the vault.",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            } else {
                items(items, key = { it.id }) { item ->
                    VaultItemCard(
                        item = item,
                        onRestore = { onRestoreItem(item) },
                        onDelete = { onDeleteItem(item) }
                    )
                }
            }
        }

        // Floating Action Button to Import
        FloatingActionButton(
            onClick = onImportClick,
            containerColor = BrandPrimary,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .size(56.dp)
                .testTag("vault_import_fab")
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Import from Downloads",
                tint = Color.Black
            )
        }
    }
}

@Composable
private fun VaultItemCard(
    item: VaultItem,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val cardBg = if (isDark) SurfaceDark else BgSecondary
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    val formattedDate = remember(item.addedAtMillis) { dateFormat.format(Date(item.addedAtMillis)) }
    val formattedSize = remember(item.sizeBytes) { formatFileSize(item.sizeBytes) }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("vault_item_${item.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(BrandPrimary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (item.mediaType == MediaType.AUDIO) Icons.Default.Audiotrack else Icons.Default.Videocam,
                    contentDescription = null,
                    tint = BrandPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.fileName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(BrandPrimary.copy(alpha = 0.15f))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "AES-256",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = formattedSize,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "• $formattedDate",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }

            // Actions
            IconButton(
                onClick = onRestore,
                modifier = Modifier.testTag("vault_item_restore_${item.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Restore,
                    contentDescription = "Restore",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("vault_item_delete_${item.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun ChangePinDialog(
    currentPin: String,
    newPin: String,
    confirmNewPin: String,
    onCurrentPinChange: (String) -> Unit,
    onNewPinChange: (String) -> Unit,
    onConfirmNewPinChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.change_pin_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = currentPin,
                    onValueChange = onCurrentPinChange,
                    label = { Text(stringResource(R.string.current_pin_label)) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("change_pin_current")
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = newPin,
                    onValueChange = onNewPinChange,
                    label = { Text(stringResource(R.string.new_pin_label)) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("change_pin_new")
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = confirmNewPin,
                    onValueChange = onConfirmNewPinChange,
                    label = { Text(stringResource(R.string.confirm_new_pin_label)) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("change_pin_confirm")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = currentPin.isNotBlank() && newPin.length >= 4 && confirmNewPin.length >= 4,
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                modifier = Modifier.testTag("change_pin_submit")
            ) {
                Text(stringResource(R.string.update_pin), color = Color.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        modifier = Modifier.testTag("vault_change_pin_dialog")
    )
}

@Composable
private fun ImportFromDownloadsDialog(
    downloads: List<DownloadTask>,
    onDismiss: () -> Unit,
    onImport: (DownloadTask) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import from Downloads") },
        text = {
            if (downloads.isEmpty()) {
                Text("No completed downloads found to import into the vault.")
            } else {
                LazyColumn(modifier = Modifier.height(260.dp)) {
                    items(downloads, key = { it.id }) { task ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onImport(task) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (task.mediaType == MediaType.AUDIO) Icons.Default.Audiotrack else Icons.Default.Videocam,
                                contentDescription = null,
                                tint = BrandPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = task.title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (task.mediaType == MediaType.AUDIO) "Audio" else "Video",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        modifier = Modifier.testTag("vault_import_dialog")
    )
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    val df = DecimalFormat("#,##0.#")
    return "${df.format(bytes / Math.pow(1024.0, digitGroups.toDouble()))} ${units[digitGroups]}"
}
