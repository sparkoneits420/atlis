package org.atlis.common.net;

import java.nio.charset.StandardCharsets;
import org.atlis.common.security.ISAAC;

public class PacketBuilder {

    public final int opcode;
    public final ISAAC isaac;

    public byte[] buffer;
    public int index;

    public static final int DEFAULT_SIZE = 64;

    public PacketBuilder(int opcode, ISAAC isaac) {
        this.opcode = opcode;
        this.isaac = isaac;
        this.buffer = new byte[DEFAULT_SIZE];
        this.index = 0;
    }

    public void addByte(byte b) {
        ensureCapacity(1);
        buffer[index++] = b;
    }

    public void addShort(short s) {
        ensureCapacity(2);
        buffer[index++] = (byte) (s >> 8);
        buffer[index++] = (byte) s;
    }

    public void addInt(int i) {
        ensureCapacity(4);
        buffer[index++] = (byte) (i >> 24);
        buffer[index++] = (byte) (i >> 16);
        buffer[index++] = (byte) (i >> 8);
        buffer[index++] = (byte) i;
    }

    public void addLong(long l) {
        ensureCapacity(8);
        buffer[index++] = (byte) (l >> 56);
        buffer[index++] = (byte) (l >> 48);
        buffer[index++] = (byte) (l >> 40);
        buffer[index++] = (byte) (l >> 32);
        buffer[index++] = (byte) (l >> 24);
        buffer[index++] = (byte) (l >> 16);
        buffer[index++] = (byte) (l >> 8);
        buffer[index++] = (byte) l;
    }

    public void addString(String value) {
        byte[] strBytes = value.getBytes(StandardCharsets.UTF_8);
        ensureCapacity(strBytes.length);
        addShort((short) strBytes.length); // write the length first
        for (byte b : strBytes) {
            buffer[index++] = b;
        }
    }

    public void addBoolean(boolean value) {
        addByte((byte) (value ? 1 : 0));
    }

    public void ensureCapacity(int bytes) {
        if (index + bytes > buffer.length) {
            byte[] newBuf = new byte[buffer.length * 2];
            System.arraycopy(buffer, 0, newBuf, 0, buffer.length);
            buffer = newBuf;
        }
    }

    public int getOpcode() {
        return opcode;
    }

    public byte[] toPacket() {
        byte[] payload = new byte[index];
        System.arraycopy(buffer, 0, payload, 0, index);

        int encryptedOpcode = (opcode + isaac.next()) & 0xFF;
        int totalSize = 1 + 2 + payload.length;

        byte[] fullPacket = new byte[totalSize];
        int ptr = 0;

        fullPacket[ptr++] = (byte) encryptedOpcode;
        fullPacket[ptr++] = (byte) (payload.length >> 8);
        fullPacket[ptr++] = (byte) payload.length;
        System.arraycopy(payload, 0, fullPacket, ptr, payload.length);

        return fullPacket;
    }

}
