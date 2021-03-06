/*
 * Copyright (C) Schweizerische Bundesbahnen SBB, 2017.
 */

package ch.sbb.perma.serializers;

import com.google.common.collect.ImmutableCollection;

/**
 * Serialize a immutable collection as value. Each item is serialized using the given item serializer.
 *
 * @author u206123 (Florian Seidl)
 * @since 1.0, 2017.
 */
public abstract class ImmutableCollectionSerializer<C extends ImmutableCollection<T>, T> implements KeyOrValueSerializer<C> {
    private final KeyOrValueSerializer<T> itemSerializer;

    public ImmutableCollectionSerializer(KeyOrValueSerializer<T> itemSerializier) {
        this.itemSerializer = itemSerializier;
    }

    @Override
    public byte[] toByteArray(C collection) {
        CompoundBinaryWriter writer = new CompoundBinaryWriter();
        writer.writeInt(collection.size());
        for (T item : collection) {
            writer.writeWithLength(itemSerializer.toByteArray(item));
        }
        return writer.toByteArray();
    }

    @SuppressWarnings("unchecked")
    public C fromByteArray(byte[] bytes) {
        CompoundBinaryReader reader = new CompoundBinaryReader(bytes);
        int collectionSize = reader.readInt();
        ImmutableCollection.Builder<T> builder = collectionBuilder();
        for (int i = 0; i < collectionSize; i++) {
            builder.add(itemSerializer.fromByteArray(reader.readWithLength()));
        }
        return (C) builder.build();
    }

    protected abstract ImmutableCollection.Builder<T> collectionBuilder();
}
