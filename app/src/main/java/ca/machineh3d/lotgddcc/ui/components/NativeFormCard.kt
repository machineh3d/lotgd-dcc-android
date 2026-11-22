package ca.machineh3d.lotgddcc.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ca.machineh3d.lotgddcc.model.FormField
import ca.machineh3d.lotgddcc.model.GameForm
import ca.machineh3d.lotgddcc.ui.theme.DccColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NativeFormCard(
    form: GameForm,
    enabled: Boolean,
    onSubmit: (Map<String, String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isRegistrationForm = remember(form) {
        form.action.contains("create.php", ignoreCase = true) ||
            (form.fields.any { it.name.equals("pass1", ignoreCase = true) } &&
                form.fields.any { it.name.equals("pass2", ignoreCase = true) })
    }

    val values = remember(form) {
        mutableStateMapOf<String, String>().apply {
            val seenRadios = mutableSetOf<String>()
            for (field in form.fields) {
                when (field.type) {
                    "checkbox" -> {
                        if (field.checked) put(field.name, field.value.ifBlank { "1" })
                    }
                    "radio" -> {
                        if (field.checked || !seenRadios.contains(field.name)) {
                            put(field.name, field.value)
                            seenRadios.add(field.name)
                        }
                    }
                    "submit" -> Unit
                    else -> put(field.name, field.value)
                }
            }
        }
    }

    // Track password visibility toggles per field name
    val passwordVisible = remember(form) { mutableStateMapOf<String, Boolean>() }

    val visibleFields = remember(form) {
        form.fields.filter { it.type != "hidden" && it.type != "submit" }
    }
    val submitButtons = remember(form) {
        form.fields.filter { it.type == "submit" }
    }

    // Group radio fields with the same `name` so they render as one clean selector
    val groupedRadioFields = remember(visibleFields) {
        visibleFields
            .filter { it.type == "radio" }
            .groupBy { it.name }
    }

    val pass1 = values["pass1"].orEmpty()
    val pass2 = values["pass2"].orEmpty()
    val passwordMismatch = isRegistrationForm && pass2.isNotEmpty() && pass1 != pass2

    Surface(
        color = DccColors.Surface,
        border = BorderStroke(
            1.dp,
            if (isRegistrationForm) DccColors.Cyan.copy(alpha = 0.45f) else DccColors.Outline
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            if (isRegistrationForm) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "NEW CRAWLER REGISTRATION",
                        color = DccColors.Cyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                    )
                    Text(
                        text = "Enter your Syndicate dossier credentials below to enter Floor 1.",
                        color = DccColors.TextSecondary,
                        fontSize = 12.sp,
                    )
                }
            }

            val renderedRadioNames = mutableSetOf<String>()
            for (field in visibleFields) {
                if (field.type == "radio") {
                    if (!renderedRadioNames.add(field.name)) continue
                    val radioGroup = groupedRadioFields[field.name].orEmpty()
                    RadioGroupField(
                        fieldName = field.name,
                        options = radioGroup,
                        selectedValue = values[field.name] ?: radioGroup.firstOrNull()?.value.orEmpty(),
                        onSelect = { values[field.name] = it },
                    )
                    continue
                }

                val friendlyLabel = resolveFriendlyLabel(field, isRegistrationForm)
                val friendlyPlaceholder = resolveFriendlyPlaceholder(field, isRegistrationForm)
                val supportingHint = resolveSupportingHint(field, isRegistrationForm, passwordMismatch)

                when (field.type) {
                    "select" -> {
                        var expanded by remember(field.name) { mutableStateOf(false) }
                        val currentVal = values[field.name] ?: field.value
                        val currentLabel = field.options.firstOrNull { it.value == currentVal }?.label
                            ?: currentVal

                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded }
                        ) {
                            OutlinedTextField(
                                value = currentLabel,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(friendlyLabel) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                for (opt in field.options) {
                                    DropdownMenuItem(
                                        text = { Text(opt.label) },
                                        onClick = {
                                            values[field.name] = opt.value
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    "checkbox" -> {
                        val checked = values.containsKey(field.name)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { isChecked ->
                                    if (isChecked) {
                                        values[field.name] = field.value.ifBlank { "1" }
                                    } else {
                                        values.remove(field.name)
                                    }
                                }
                            )
                            Text(
                                text = friendlyLabel,
                                color = DccColors.TextPrimary,
                                fontSize = 14.sp
                            )
                        }
                    }

                    "textarea" -> {
                        OutlinedTextField(
                            value = values[field.name] ?: "",
                            onValueChange = { values[field.name] = it },
                            label = { Text(friendlyLabel) },
                            placeholder = if (friendlyPlaceholder.isNotBlank()) {
                                { Text(friendlyPlaceholder) }
                            } else null,
                            minLines = 3,
                            maxLines = 6,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    else -> {
                        val isPassword = field.type == "password" ||
                            field.name.equals("pass1", ignoreCase = true) ||
                            field.name.equals("pass2", ignoreCase = true)
                        val isShown = passwordVisible[field.name] == true
                        val isError = field.name.equals("pass2", ignoreCase = true) && passwordMismatch

                        OutlinedTextField(
                            value = values[field.name] ?: "",
                            onValueChange = { values[field.name] = it },
                            label = { Text(friendlyLabel) },
                            placeholder = if (friendlyPlaceholder.isNotBlank()) {
                                { Text(friendlyPlaceholder) }
                            } else null,
                            supportingText = if (!supportingHint.isNullOrBlank()) {
                                {
                                    Text(
                                        text = supportingHint,
                                        color = if (isError) DccColors.Crimson else DccColors.TextSecondary,
                                        fontSize = 11.sp,
                                    )
                                }
                            } else null,
                            isError = isError,
                            singleLine = true,
                            visualTransformation = if (isPassword && !isShown) {
                                PasswordVisualTransformation()
                            } else {
                                VisualTransformation.None
                            },
                            trailingIcon = if (isPassword) {
                                {
                                    IconButton(onClick = { passwordVisible[field.name] = !isShown }) {
                                        Icon(
                                            imageVector = if (isShown) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                            contentDescription = if (isShown) "Hide password" else "Show password",
                                            tint = DccColors.TextSecondary,
                                        )
                                    }
                                }
                            } else null,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(Modifier.height(2.dp))

            val canSubmit = enabled && !passwordMismatch
            if (submitButtons.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for (btn in submitButtons) {
                        Button(
                            enabled = canSubmit,
                            onClick = {
                                val payload = values.toMutableMap()
                                payload[btn.name] = btn.value
                                onSubmit(payload)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DccColors.Crimson),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(btn.label.ifBlank { form.submitLabel }, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                Button(
                    enabled = canSubmit,
                    onClick = { onSubmit(values.toMap()) },
                    colors = ButtonDefaults.buttonColors(containerColor = DccColors.Crimson),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val defaultSubmit = if (isRegistrationForm) "Create Crawler" else "Submit"
                    Text(form.submitLabel.ifBlank { defaultSubmit }, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun RadioGroupField(
    fieldName: String,
    options: List<FormField>,
    selectedValue: String,
    onSelect: (String) -> Unit,
) {
    val groupTitle = when {
        fieldName.equals("sex", ignoreCase = true) -> "Gender"
        else -> fieldName.replaceFirstChar { it.uppercase() }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = groupTitle,
            color = DccColors.TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )

        if (options.size <= 3) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                for (opt in options) {
                    val isSelected = selectedValue == opt.value
                    val optionLabel = resolveRadioOptionLabel(fieldName, opt)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) DccColors.Cyan.copy(alpha = 0.16f) else DccColors.SurfaceVariant
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) DccColors.Cyan else DccColors.Outline,
                                shape = RoundedCornerShape(10.dp),
                            )
                            .clickable { onSelect(opt.value) }
                            .padding(vertical = 10.dp, horizontal = 8.dp)
                    ) {
                        Text(
                            text = optionLabel,
                            color = if (isSelected) DccColors.Cyan else DccColors.TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        )
                    }
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                for (opt in options) {
                    val isSelected = selectedValue == opt.value
                    val optionLabel = resolveRadioOptionLabel(fieldName, opt)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onSelect(opt.value) }
                            .padding(vertical = 2.dp)
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { onSelect(opt.value) },
                            colors = RadioButtonDefaults.colors(selectedColor = DccColors.Cyan)
                        )
                        Text(
                            text = optionLabel,
                            color = DccColors.TextPrimary,
                            fontSize = 14.sp,
                        )
                    }
                }
            }
        }
    }
}

private fun resolveRadioOptionLabel(fieldName: String, option: FormField): String {
    if (fieldName.equals("sex", ignoreCase = true)) {
        return when (option.value.trim()) {
            "0" -> "Male"
            "1" -> "Female"
            else -> option.label.ifBlank { option.value }
        }
    }
    return option.label
        .takeIf { it.isNotBlank() && !it.equals(fieldName, ignoreCase = true) }
        ?: option.value.ifBlank { "Option" }
}

private fun resolveFriendlyLabel(field: FormField, isRegistrationForm: Boolean): String {
    if (isRegistrationForm) {
        return when (field.name.lowercase()) {
            "name" -> "Crawler Name"
            "pass1" -> "Password"
            "pass2" -> "Confirm Password"
            "email" -> "Email Address"
            else -> field.label.ifBlank { field.name }
        }
    }
    return field.label.ifBlank { field.name }
}

private fun resolveFriendlyPlaceholder(field: FormField, isRegistrationForm: Boolean): String {
    if (field.placeholder.isNotBlank()) return field.placeholder
    if (isRegistrationForm) {
        return when (field.name.lowercase()) {
            "name" -> "How will you be known to this world?"
            "email" -> "you@example.com"
            else -> ""
        }
    }
    return ""
}

private fun resolveSupportingHint(
    field: FormField,
    isRegistrationForm: Boolean,
    passwordMismatch: Boolean,
): String? {
    if (!isRegistrationForm) return null
    return when (field.name.lowercase()) {
        "pass2" -> if (passwordMismatch) "Passwords do not match" else null
        "email" -> "Required — a verification link will be emailed before you can log in."
        else -> null
    }
}
