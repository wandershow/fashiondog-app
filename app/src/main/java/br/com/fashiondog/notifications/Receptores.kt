package br.com.fashiondog.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import br.com.fashiondog.FashionDogApplication
import br.com.fashiondog.MainActivity
import br.com.fashiondog.R
import kotlinx.coroutines.launch

/** Acordado pelo alarme: mostra a notificação com o texto preparado em [Avisos]. */
class ReceptorDeAvisos : BroadcastReceiver() {

    override fun onReceive(contexto: Context, intent: Intent) {
        // Sem permissão de notificação (Android 13+), não há o que mostrar.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(contexto, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return

        val codigo = intent.getIntExtra(EXTRA_CODIGO, 0)
        val abrirApp = PendingIntent.getActivity(
            contexto,
            codigo,
            Intent(contexto, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val texto = intent.getStringExtra(EXTRA_TEXTO).orEmpty()
        val notificacao = NotificationCompat.Builder(contexto, intent.getStringExtra(EXTRA_CANAL) ?: Avisos.CANAL_AGENDA)
            .setSmallIcon(R.drawable.ic_aba_agenda)
            .setContentTitle(intent.getStringExtra(EXTRA_TITULO))
            .setContentText(texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(abrirApp)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(contexto).notify(codigo, notificacao)
    }

    companion object {
        const val EXTRA_CODIGO = "codigo"
        const val EXTRA_CANAL = "canal"
        const val EXTRA_TITULO = "titulo"
        const val EXTRA_TEXTO = "texto"
    }
}

/**
 * O Android apaga todos os alarmes quando o celular reinicia (e quando o app é
 * atualizado ou o relógio muda). Este receptor recria todos os avisos pendentes.
 */
class ReceptorDeInicializacao : BroadcastReceiver() {

    override fun onReceive(contexto: Context, intent: Intent) {
        val app = contexto.applicationContext as FashionDogApplication
        // goAsync: dá alguns segundos para terminar a consulta ao banco em segundo plano.
        val pendente = goAsync()
        app.container.escopo.launch {
            try {
                app.container.reagendarAvisos()
            } finally {
                pendente.finish()
            }
        }
    }
}
