package at.florianschuster.hydro.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import at.florianschuster.hydro.AppAction
import at.florianschuster.hydro.AppState
import at.florianschuster.hydro.model.Cup
import at.florianschuster.hydro.model.Milliliters
import at.florianschuster.hydro.model.icon
import at.florianschuster.hydro.ui.base.HydrationCarousel
import at.florianschuster.hydro.model.LiquidUnit

@Composable
fun CupCarouselSelection(
    modifier: Modifier = Modifier,
    state: AppState,
    dispatch: (AppAction) -> Unit
) {
    Column(modifier = modifier) {
        var showCanOnlySelectThreeAlert by remember { mutableStateOf(false) }
        Text(
            modifier = Modifier.padding(horizontal = 24.dp),
            text = "Cups",
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            modifier = Modifier.padding(horizontal = 24.dp),
            text = "Cups are displayed on your Main screen and in your Reminder notification",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        val allCupsAsMilliliters = remember(state.allCups) {
            state.allCups.map(Cup::milliliters)
        }
        val selectedCupsAsMilliliters = remember(state.selectedCups) {
            state.selectedCups.map(Cup::milliliters)
        }
        HydrationCarousel(
            contentPadding = PaddingValues(horizontal = 36.dp),
            milliliterItems = allCupsAsMilliliters,
            liquidUnit = state.liquidUnit,
            selected = selectedCupsAsMilliliters,
            onClick = { index, _ ->
                val cup = state.allCups[index]
                if (cup in state.selectedCups) {
                    dispatch(AppAction.SetSelectedCups(state.selectedCups - cup))
                } else if (state.selectedCups.count() >= 3) {
                    showCanOnlySelectThreeAlert = true
                } else {
                    dispatch(AppAction.SetSelectedCups(state.selectedCups + cup))
                }
            },
            trailingContent = {
                var showCustomDialog by remember { mutableStateOf(false) }

                OutlinedCard(
                    modifier = Modifier.clickable { showCustomDialog = true },
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = CardDefaults.outlinedCardBorder(false)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            modifier = Modifier.size(28.dp),
                            painter = Milliliters(500).icon(),
                            contentDescription = null
                        )
                        Text("Custom…", style = MaterialTheme.typography.labelLarge)
                    }
                }

                if (showCustomDialog) {
                    CustomSizeDialog(
                        liquidUnit = state.liquidUnit,
                        onConfirm = { ml ->
                            showCustomDialog = false
                            val bounded = ml.coerceIn(50, 5000)
                            if (state.selectedCups.any { it.milliliters == Milliliters(bounded) }) return@CustomSizeDialog
                            if (state.selectedCups.size >= 3) {
                                showCanOnlySelectThreeAlert = true
                            } else {
                                dispatch(
                                    AppAction.SetSelectedCups(
                                        state.selectedCups + Cup(milliliters = Milliliters(bounded))
                                    )
                                )
                            }
                        },
                        onDismiss = { showCustomDialog = false }
                    )
                }
            }
        )
        if (showCanOnlySelectThreeAlert) {
            AlertDialog(
                title = { Text(text = "Sorry") },
                text = { Text(text = "You can only select 3 different Cups.") },
                onDismissRequest = { showCanOnlySelectThreeAlert = false },
                confirmButton = {
                    Button(
                        onClick = { showCanOnlySelectThreeAlert = false },
                        content = { Text(text = "Ok") }
                    )
                }
            )
        }
    }
}
@Composable
private fun CustomSizeDialog(
    liquidUnit: LiquidUnit,
    onConfirm: (Int /* ml */) -> Unit,
    onDismiss: () -> Unit,
) {
    val (unitLabel, toMlFactor, minMl) = when (liquidUnit) {
        LiquidUnit.Milliliter   -> Triple("ml", 1.0, 50)
        LiquidUnit.USFluidOunce -> Triple("fl oz (US)", 29.5735, 1)
        LiquidUnit.UKFluidOunce -> Triple("fl oz (UK)", 28.4131, 1)
    }
    val maxMl = 5000
    val minDisplay = (minMl / toMlFactor).coerceAtLeast(1.0).toInt()
    val maxDisplay = (maxMl / toMlFactor).toInt()

    var text by remember { mutableStateOf("") }
    val sanitized = remember(text) {
        text.replace(',', '.')
            .filterIndexed { i, c -> c.isDigit() || (c == '.' && !text.take(i).contains('.')) }
    }
    val entered = sanitized.toDoubleOrNull()
    val ml = entered?.let { (it * toMlFactor).toInt() }
    val valid = ml != null && ml in minMl..maxMl

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Custom size") },
        text = {
            Column {
                androidx.compose.material3.OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Amount") },
                    placeholder = { Text(if (liquidUnit == LiquidUnit.Milliliter) "500" else "16") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    suffix = { Text(unitLabel) },
                    isError = !valid && text.isNotEmpty()
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    if (valid && ml != null) "≈ $ml ml"
                    else "Enter a value between $minDisplay–$maxDisplay $unitLabel",
                    color = if (valid || text.isEmpty())
                        MaterialTheme.colorScheme.onSurfaceVariant
                    else
                        MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            Button(enabled = valid, onClick = { onConfirm(ml!!) }) { Text("OK") }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}