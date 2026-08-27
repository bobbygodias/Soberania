package org.soberania.app.transport.wireguard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.soberania.app.packet.OwnedTun
import org.soberania.app.transport.TransportRuntime
import org.soberania.app.transport.TransportState

class WireGuardPacketBackendTest {

    private val config = WireGuardConfig(
        interfaceName = "wg-lab",
        userspaceConfig = "private_key=00"
    )

    @Test
    fun unavailableEngineClosesTunWithoutDetaching() {
        val tun = FakeOwnedTun(fd = 41)
        val engine = FakeEngine(available = false)
        val backend = WireGuardPacketBackend(config, engine)

        val state = backend.start(tun, TransportRuntime { true })

        assertTrue(state is TransportState.Failed)
        assertTrue(tun.closed)
        assertFalse(tun.detached)
        assertEquals(0, engine.turnOnCalls)
    }

    @Test
    fun nativeStartFailureLeavesOwnershipWithEngine() {
        val tun = FakeOwnedTun(fd = 42)
        val engine = FakeEngine(
            turnOnResult = -3
        )
        val backend = WireGuardPacketBackend(config, engine)

        val state = backend.start(tun, TransportRuntime { true })

        assertTrue(state is TransportState.Failed)
        assertTrue(tun.detached)
        assertFalse(tun.closed)
        assertEquals(42, engine.receivedFd)
        assertEquals(1, engine.turnOnCalls)
        assertEquals(0, engine.turnOffCalls)
    }

    @Test
    fun negativeSocketLookupFailsClosedWithoutProtectCall() {
        val tun = FakeOwnedTun(fd = 45)
        val engine = FakeEngine(
            turnOnResult = 11,
            socketV4Fd = 120,
            socketV6Fd = WireGuardNativeEngine.SOCKET_LOOKUP_ERROR
        )
        val protected = mutableListOf<Int>()
        val backend = WireGuardPacketBackend(config, engine)

        val state = backend.start(
            tun,
            TransportRuntime { fd ->
                protected += fd
                true
            }
        )

        assertTrue(state is TransportState.Failed)
        assertTrue(protected.isEmpty())
        assertEquals(1, engine.turnOffCalls)
        assertEquals(11, engine.lastTurnedOffHandle)
    }

    @Test
    fun protectFailureStopsNativeTunnelFailClosed() {
        val tun = FakeOwnedTun(fd = 43)
        val engine = FakeEngine(
            turnOnResult = 7,
            socketV4Fd = 100,
            socketV6Fd = 101
        )
        val protected = mutableListOf<Int>()
        val backend = WireGuardPacketBackend(config, engine)

        val state = backend.start(
            tun,
            TransportRuntime { fd ->
                protected += fd
                fd != 101
            }
        )

        assertTrue(state is TransportState.Failed)
        assertEquals(listOf(100, 101), protected)
        assertEquals(1, engine.turnOffCalls)
        assertEquals(7, engine.lastTurnedOffHandle)
    }

    @Test
    fun successfulLifecycleReachesReadyThenStopsExactlyOnce() {
        val tun = FakeOwnedTun(fd = 44)
        val engine = FakeEngine(
            turnOnResult = 9,
            socketV4Fd = 110,
            socketV6Fd = 111
        )
        val protected = mutableListOf<Int>()
        val backend = WireGuardPacketBackend(config, engine)

        val state = backend.start(
            tun,
            TransportRuntime { fd ->
                protected += fd
                true
            }
        )

        assertTrue(state is TransportState.Ready)
        assertEquals(listOf(110, 111), protected)
        assertEquals(9, engine.turnOnResult)

        backend.stop()
        backend.stop()

        assertEquals(TransportState.Stopped, backend.state())
        assertEquals(1, engine.turnOffCalls)
        assertEquals(9, engine.lastTurnedOffHandle)
    }

    private class FakeOwnedTun(
        private val fd: Int
    ) : OwnedTun {
        var detached = false
        var closed = false

        override fun detachRawFd(): Int {
            check(!closed)
            check(!detached)
            detached = true
            return fd
        }

        override fun hasOwnership(): Boolean = !closed && !detached

        override fun close() {
            if (!detached) {
                closed = true
            }
        }
    }

    private class FakeEngine(
        private val available: Boolean = true,
        val turnOnResult: Int = 1,
        private val socketV4Fd: Int = WireGuardNativeEngine.SOCKET_LOOKUP_ERROR,
        private val socketV6Fd: Int = WireGuardNativeEngine.SOCKET_LOOKUP_ERROR
    ) : WireGuardNativeEngine {

        var turnOnCalls = 0
        var turnOffCalls = 0
        var receivedFd: Int? = null
        var lastTurnedOffHandle: Int? = null

        override fun isAvailable(): Boolean = available

        override fun turnOn(
            interfaceName: String,
            tunFd: Int,
            userspaceConfig: String
        ): Int {
            turnOnCalls++
            receivedFd = tunFd
            return turnOnResult
        }

        override fun turnOff(handle: Int) {
            turnOffCalls++
            lastTurnedOffHandle = handle
        }

        override fun socketV4(handle: Int): Int = socketV4Fd

        override fun socketV6(handle: Int): Int = socketV6Fd

        override fun version(): String = "fake"
    }
}
