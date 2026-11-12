package br.com.fashiondog.ui.mais

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import br.com.fashiondog.FashionDogApplication
import br.com.fashiondog.ui.componentes.Cartao
import br.com.fashiondog.ui.componentes.Formulario
import br.com.fashiondog.ui.componentes.Tela

/**
 * Mostra se os avisos vão funcionar e leva às configurações do Android se não.
 * As permissões ficam no sistema, então a tela relê o estado sempre que volta a aparecer.
 */
@Composable
fun AvisosScreen(aoVoltar: () -> Unit) {
    val contexto = LocalContext.current
    val avisos = (contexto.applicationContext as FashionDogApplication).container.avisos
    var atualizacao by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { atualizacao++ }

    // "atualizacao" força reler ao voltar das configurações.
    val notificacoesLigadas = remember(atualizacao) { NotificationManagerCompat.from(contexto).areNotificationsEnabled() }
    val alarmeExato = remember(atualizacao) { avisos.podeAgendarExato() }

    Tela(titulo = "Avisos", aoVoltar = aoVoltar) { espacamento ->
        Formulario(espacamento) {
            Cartao {
                Text("Notificações", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (notificacoesLigadas) "Ligadas: os lembretes vão aparecer."
                    else "Desligadas: nenhum lembrete vai aparecer.",
                    color = if (notificacoesLigadas) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
                )
                OutlinedButton(onClick = {
                    contexto.startActivity(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, contexto.packageName),
                    )
                }) { Text("Abrir configurações de notificação") }
            }
            Cartao {
                Text("Horário exato", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (alarmeExato) "Liberado: o aviso toca no minuto certo."
                    else "Não liberado: o aviso pode atrasar até 10 minutos.",
                )
                if (!alarmeExato && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    OutlinedButton(onClick = {
                        contexto.startActivity(
                            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, "package:${contexto.packageName}".toUri()),
                        )
                    }) { Text("Liberar horário exato") }
                }
            }
            Text(
                "Os avisos são recriados sozinhos quando o celular reinicia. Se a bateria do celular " +
                    "tiver \"otimização agressiva\", deixe o FashionDog como \"sem restrições\".",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
