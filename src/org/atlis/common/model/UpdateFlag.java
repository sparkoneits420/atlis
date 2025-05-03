package org.atlis.common.model;
 
import java.util.HashMap;
import java.util.Map;

public enum UpdateFlag {
    
    APPEARANCE(0x01),
    REGION(0x02),
    ANIMATION(0x03),
    WALKING(0x04),
    CHAT(0x05);
    
    private static final Map<Integer, UpdateFlag> BY_OPCODE = new HashMap<>();
    private final int bit;
    
    UpdateFlag(int bit) {
        this.bit = bit;
    }

    public int getBit() {
        return bit;
    } 
    
    public static UpdateFlag getFlag(int bit) {
        return BY_OPCODE.get(bit);
    }
    
    static {
        for (UpdateFlag flag : values()) {
            BY_OPCODE.put(flag.getBit(), flag);
        }
    }
}