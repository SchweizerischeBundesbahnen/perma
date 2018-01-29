/*
 * Copyright (C) Schweizerische Bundesbahnen SBB, 2017.
 */

package ch.sbb.perma.serializers;

/**
 * Do not use. Use String Serializer instead.
 *
 * @author u206123 (Florian Seidl)
 * @since 3.1, 2017.
 */
@Deprecated
public class ShortStringSerializer implements KeyOrValueSerializer<String> {
    @Override
    public byte[] toByteArray(String string) {
        return string.getBytes();
    }

    @Override
    public String fromByteArray(byte[] bytes) {
        return new String(bytes);
    }
}