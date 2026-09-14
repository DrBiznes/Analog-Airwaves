package me.jamino.analogairwaves.block;

import net.minecraft.util.StringRepresentable;

public enum TransmitterStatus implements StringRepresentable {
    IDLE("idle"),
    BROADCASTING("broadcasting"),
    INTERFERENCE("interference");

    private final String serializedName;

    TransmitterStatus(String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
