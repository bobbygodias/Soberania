package org.soberania.app.transport.wireguard.lab

import android.app.Activity
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.util.Log
import org.soberania.app.packet.OwnedTunDescriptor
import org.soberania.app.transport.wireguard.JniWireGuardNativeEngine
import java.io.File

/**
 * Smoke test Android exclusivo da variante wireguardLab.
 *
 * Não cria VpnService, não altera rotas, não toca em DNS e não estabelece peer.
 * Existe para validar linker/JNI/Go e ownership real de um FD Android.
 */
class WireGuardLabSmokeActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val result = runCatching {
            runSmoke()
        }

        result.onFailure { error ->
            Log.e(
                TAG,
                "SOBERANIA_WG_SMOKE_FAIL " +
                    error.javaClass.simpleName + ": " +
                    (error.message ?: "sem mensagem")
            )
        }

        finishAndRemoveTask()
    }

    private fun runSmoke() {
        val engine = JniWireGuardNativeEngine()

        check(engine.isAvailable()) {
            "biblioteca WireGuard nativa indisponível"
        }

        val version = engine.version()
            ?: error("wireguard-go não retornou versão")

        Log.i(
            TAG,
            "SMOKE_NATIVE_AVAILABLE version=" + version
        )

        val file = File(cacheDir, "wireguard-fd-ownership-smoke.tmp").apply {
            delete()
        }

        val original = ParcelFileDescriptor.open(
            file,
            ParcelFileDescriptor.MODE_CREATE or
                ParcelFileDescriptor.MODE_READ_WRITE or
                ParcelFileDescriptor.MODE_TRUNCATE
        )

        try {
            val duplicate = ParcelFileDescriptor.dup(original.fileDescriptor)
            val owned = OwnedTunDescriptor(duplicate)
            val rawFd = owned.detachRawFd()

            val openBefore = LabNativeFdProbe.isOpen(rawFd)
            check(openBefore) {
                "duplicata já estava fechada antes da ponte nativa"
            }

            /*
             * Este FD é um arquivo comum, não uma TUN.
             * O adaptador Go deve rejeitá-lo e, por contrato, fechar somente
             * a duplicata cuja ownership recebeu.
             */
            val turnOnResult = engine.turnOn(
                interfaceName = "wg-fd-smoke",
                tunFd = rawFd,
                userspaceConfig = ""
            )

            check(turnOnResult < 0) {
                "FD não-TUN foi aceito inesperadamente: handle=" + turnOnResult
            }

            val openAfter = LabNativeFdProbe.isOpen(rawFd)
            check(!openAfter) {
                "FD transferido permaneceu aberto após falha nativa"
            }

            val originalStillAlive = runCatching {
                ParcelFileDescriptor.dup(original.fileDescriptor).use { }
                true
            }.getOrDefault(false)

            check(originalStillAlive) {
                "fechamento da duplicata atingiu o descritor original"
            }

            Log.i(
                TAG,
                "SMOKE_FD_OWNERSHIP " +
                    "before=" + openBefore +
                    " result=" + turnOnResult +
                    " after=" + openAfter +
                    " originalAlive=" + originalStillAlive
            )

            Log.i(
                TAG,
                "SOBERANIA_WG_SMOKE_OK version=" + version
            )
        } finally {
            runCatching { original.close() }
            runCatching { file.delete() }
        }
    }

    companion object {
        private const val TAG = "SoberaniaWgSmoke"
    }
}
