/**
 * HoTT Transmitter Config Copyright (C) 2013 Oliver Treichel
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <http:></http:>//www.gnu.org/licenses/>.
 */
package de.treichels.hott.serial.jserialcommport

import com.fazecast.jSerialComm.SerialPort
import com.fazecast.jSerialComm.SerialPort.*
import de.treichels.hott.model.HoTTException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * @author Oliver Treichel &lt;oli@treichels.de&gt;
 */
class JSerialCommPort(override val portName: String) : de.treichels.hott.serial.SerialPort {
    private var port: SerialPort = SerialPort.getCommPort(portName)

    override var baudRate
        get() = port.baudRate
        set(baudRate) {
            port.setComPortParameters(baudRate, 8, ONE_STOP_BIT, NO_PARITY)
        }

    override val inputStream = object : InputStream() {
        val buffer = ByteArray(1)
        override fun read(): Int {
            if (port.readBytes(buffer, 1, 0) != 1) throw IOException("timeout")

            return buffer[0].toInt() and 0xff
        }

        override fun available(): Int {
            return port.bytesAvailable()
        }

        override fun read(bytes: ByteArray?, off: Int, len: Int): Int {
            if (len == 0) return 0

            return port.readBytes(bytes, len, off).apply {
                if (this < 1) throw IOException("timeout")
            }
        }
    }

    override val outputStream: OutputStream
        get() = port.outputStream

    override var timeout
        get() = port.readTimeout
        set(timeout) {
            val mode = if (timeout < 0) TIMEOUT_NONBLOCKING else TIMEOUT_READ_BLOCKING + TIMEOUT_WRITE_BLOCKING
            port.setComPortTimeouts(mode, timeout, timeout)
        }

    init {
        port.setFlowControl(FLOW_CONTROL_DISABLED)

        // default to blocking read and write with 1 second timeout
        timeout = 1000

        // default 115200 baud, 8 data bits, one stop bit, no parity
        baudRate = 115200
    }

    override fun readBytes(data: ByteArray, length: Int, offset: Int) = port.readBytes(data, length, offset)

    override fun writeBytes(data: ByteArray, length: Int, offset: Int) = port.writeBytes(data, length, offset)

    override val isOpen: Boolean
        get() = port.isOpen

    override fun close() {
        if (isOpen) port.closePort()
    }

    override fun open() {
        if (isOpen) throw HoTTException("HoTTSerialPort.AlreadyOpen")

        port.openPort(0)
        if (!port.isOpen) {
            // open failed, retry without configuration (e.g. for STM virtual COM ports)
            port.disablePortConfiguration()
            port.openPort(0)
        }
    }

    override fun reset() {
        close()
        open()
    }
}
