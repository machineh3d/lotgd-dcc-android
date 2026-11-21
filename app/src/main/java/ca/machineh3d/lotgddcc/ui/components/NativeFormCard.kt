package ca.machineh3d.lotgddcc.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val values = remember(form) {
        mutableStateMapOf<String, String>().apply {
            for (field in form.fields) {
                if (field.type == "checkbox") {
                    if (field.checked) put(field.name, field.value.ifBlank { "1" })
                } else if (field.type != "submit") {
                    put(field.name, field.value)
                }
            }
        }
    }

    val visibleFields = form.fields.filter { it.type != "hidden" && it.type != "submit" }
    val submitButtons = form.fields.filter { it.type == "submit" }

    Surface(
        color = DccColors.Surface,
        border = BorderStroke(1.dp, DccColors.Outline),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(14.dp)
        ) {
            for (field in visibleFields) {
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
                                label = { Text(field.label) },
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
                                text = field.label,
                                color = DccColors.TextPrimary,
                                fontSize = 14.sp
                            )
                        }
                    }

                    "textarea" -> {
                        OutlinedTextField(
                            value = values[field.name] ?: "",
                            onValueChange = { values[field.name] = it },
                            label = { Text(field.label) },
                            placeholder = if (field.placeholder.isNotBlank()) {
                                { Text(field.placeholder) }
                            } else null,
                            minLines = 3,
                            maxLines = 6,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    else -> {
                        OutlinedTextField(
                            value = values[field.name] ?: "",
                            onValueChange = { values[field.name] = it },
                            label = { Text(field.label) },
                            placeholder = if (field.placeholder.isNotBlank()) {
                                { Text(field.placeholder) }
                            } else null,
                            singleLine = true,
                            visualTransformation = if (field.type == "password") {
                                PasswordVisualTransformation()
                            } else {
                                VisualTransformation.None
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(Modifier.height(2.dp))

            if (submitButtons.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for (btn in submitButtons) {
                        Button(
                            enabled = enabled,
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
                    enabled = enabled,
                    onClick = { onSubmit(values.toMap()) },
                    colors = ButtonDefaults.buttonColors(containerColor = DccColors.Crimson),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(form.submitLabel.ifBlank { "Submit" }, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
