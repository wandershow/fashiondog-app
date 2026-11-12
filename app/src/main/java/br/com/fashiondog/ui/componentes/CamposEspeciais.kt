package br.com.fashiondog.ui.componentes

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import br.com.fashiondog.domain.Datas
import br.com.fashiondog.domain.Dinheiro
import java.time.LocalDate
import java.time.LocalTime

/**
 * Campo de valor em reais. Funciona como a maquininha de cartão: cada número
 * digitado entra pela direita (5 → R$ 0,05; 50 → R$ 0,50; 5000 → R$ 50,00).
 */
@Composable
fun CampoDinheiro(
    centavos: Long,
    aoMudar: (Long) -> Unit,
    rotulo: String,
    modifier: Modifier = Modifier,
    erro: String? = null,
) {
    val texto = Dinheiro.formatar(centavos)
    OutlinedTextField(
        // O cursor fica sempre no fim, onde os números entram.
        value = TextFieldValue(texto, selection = TextRange(texto.length)),
        onValueChange = { aoMudar(Dinheiro.deDigitos(it.text)) },
        label = { Text(rotulo) },
        isError = erro != null,
        supportingText = erro?.let { { Text(it) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier.fillMaxWidth(),
    )
}

/** Campo que, ao ser tocado, abre um calendário. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampoData(
    data: LocalDate,
    aoMudar: (LocalDate) -> Unit,
    rotulo: String,
    modifier: Modifier = Modifier,
) {
    var aberto by remember { mutableStateOf(false) }
    CampoSomenteLeitura(Datas.formatarData(data), rotulo, modifier) { aberto = true }

    if (aberto) {
        val estado = rememberDatePickerState(initialSelectedDateMillis = Datas.paraSeletor(data))
        DatePickerDialog(
            onDismissRequest = { aberto = false },
            confirmButton = {
                TextButton(onClick = {
                    estado.selectedDateMillis?.let { aoMudar(Datas.deSeletor(it)) }
                    aberto = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { aberto = false }) { Text("Cancelar") } },
        ) {
            DatePicker(state = estado)
        }
    }
}

/** Campo que, ao ser tocado, abre um relógio para escolher a hora. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampoHora(
    hora: LocalTime,
    aoMudar: (LocalTime) -> Unit,
    rotulo: String,
    modifier: Modifier = Modifier,
) {
    var aberto by remember { mutableStateOf(false) }
    CampoSomenteLeitura("%02d:%02d".format(hora.hour, hora.minute), rotulo, modifier) { aberto = true }

    if (aberto) {
        val estado = rememberTimePickerState(initialHour = hora.hour, initialMinute = hora.minute, is24Hour = true)
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { aberto = false },
            confirmButton = {
                TextButton(onClick = {
                    aoMudar(LocalTime.of(estado.hour, estado.minute))
                    aberto = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { aberto = false }) { Text("Cancelar") } },
            text = { TimePicker(state = estado) },
        )
    }
}

/** Campo que parece de texto mas só abre um seletor quando tocado. */
@Composable
private fun CampoSomenteLeitura(
    valor: String,
    rotulo: String,
    modifier: Modifier,
    aoTocar: () -> Unit,
) {
    val interacao = remember { MutableInteractionSource() }
    val pressionado by interacao.collectIsPressedAsState()
    LaunchedEffect(pressionado) { if (pressionado) aoTocar() }

    OutlinedTextField(
        value = valor,
        onValueChange = {},
        readOnly = true,
        label = { Text(rotulo) },
        singleLine = true,
        interactionSource = interacao,
        modifier = modifier.fillMaxWidth(),
    )
}
