package org.atlis.common.net;

import java.nio.charset.StandardCharsets;

public class Packet {

    public final byte[] buffer;
    public int index, opcode;

    public Packet(byte[] buffer, int opcode) {
        this.buffer = buffer;
        this.opcode = opcode;
    }

    public byte getByte() {
        return buffer[index++];
    }

    public short getShort() {
        int high = buffer[index++] & 0xFF;
        int low = buffer[index++] & 0xFF;
        return (short) ((high << 8) | low);
    }

    public int getInt() {
        int b1 = buffer[index++] & 0xFF;
        int b2 = buffer[index++] & 0xFF;
        int b3 = buffer[index++] & 0xFF;
        int b4 = buffer[index++] & 0xFF;
        return (b1 << 24) | (b2 << 16) | (b3 << 8) | b4;
    }

    public long getLong() {
        long value = 0;
        for (int i = 0; i < 8; i++) {
            value = (value << 8) | (buffer[index++] & 0xFFL);
        }
        return value;
    }

    public String getString() {
        int length = getShort(); // read the length first
        String result = new String(buffer, index, length, StandardCharsets.UTF_8);
        index += length;
        return result;
    }
    
    public boolean getBoolean() {
        return getByte() == 1;
    }
    
    public int getOpcode() {
        return opcode;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int remaining() {
        return buffer.length - index;
    }

}
