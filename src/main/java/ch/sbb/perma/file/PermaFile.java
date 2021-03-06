/*
 * Copyright (C) Schweizerische Bundesbahnen SBB, 2018.
 */

package ch.sbb.perma.file;

import com.google.common.base.Preconditions;
import com.google.common.collect.ComparisonChain;

import java.io.*;
import java.util.Objects;

public final class PermaFile implements Comparable<PermaFile> {
    private final Compression compression;
    private final File dir;
    private final String permaName;
    private final int fullFileNumber;
    private final int deltaFileNumber;

    private PermaFile(Compression compression, File dir, String permaName, int fullFileNumber, int deltaFileNumber) {
        this.compression = compression;
        this.dir = dir;
        this.permaName = permaName;
        this.fullFileNumber = fullFileNumber;
        this.deltaFileNumber = deltaFileNumber;
    }

    public static PermaFile fullFile(Compression compression, File dir, String permaName, int fullFileNumber) {
        return new PermaFile(compression, dir, permaName, fullFileNumber, 0);
    }

    DeltaFilePattern deltaFileNamePattern() {
        return new DeltaFilePattern(this, permaName, fullFileNumber);
    }

    PermaFile nextDelta() {
        return new PermaFile(compression, dir, permaName, fullFileNumber, deltaFileNumber + 1);
    }

    PermaFile nextFull(Compression compression) {
        return fullFile(compression, dir, permaName, fullFileNumber + 1);
    }

    public PermaFile delta(int nr) {
        return new PermaFile(compression, dir, permaName, fullFileNumber, nr);
    }

    public <R> R withInputStream(IOFunction<InputStream, R> function) throws IOException {
        try(InputStream in = compression.decompress(new FileInputStream(toFile()))) {
            return function.apply(in);
        }
    }

    public <R> R withOutputStream(IOFunction<OutputStream, R> function) throws IOException {
        TempFile tempFile = new TempFile(dir, permaName);
        tempFile.deleteStaleTempFiles();
        R retval = tempFile.withOutputStream(out -> function.apply(compression.compress(out)));
        tempFile.moveTo(toFile());
        return retval;
    }

    public boolean delete() {
        return toFile().delete();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        PermaFile otherFile = (PermaFile) other;
        return fullFileNumber == otherFile.fullFileNumber &&
                deltaFileNumber == otherFile.deltaFileNumber &&
                Objects.equals(compression, otherFile.compression) &&
                Objects.equals(dir, otherFile.dir) &&
                Objects.equals(permaName, otherFile.permaName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(permaName, fullFileNumber, deltaFileNumber);
    }

    @Override
    public int compareTo(PermaFile other) {
        Preconditions.checkArgument(permaName.equals(other.permaName), "Can only compare within the same perma");
        return ComparisonChain.start()
                .compare(fullFileNumber, other.fullFileNumber)
                .compare(deltaFileNumber, other.deltaFileNumber)
                .result();
    }

    private File toFile() {
        return new File(dir, toFileName());
    }

    private String toFileName() {
        return compression.fileNameFormat().format(permaName, fullFileNumber, deltaFileNumber);
    }

    @Override
    public String toString() {
        return toFile().toString();
    }
}
