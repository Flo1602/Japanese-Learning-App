package at.primetshofer.logic.provider.file;

import java.io.File;

public class UnicodeFilenameFileProvider implements IFileProvider {

    private final String parentAbsolutePath;
    private final char prefix;
    private final int expectedFilenameLength;
    private final String extension;
    private char charForFilename;

    public UnicodeFilenameFileProvider(String parentAbsolutePath, char prefix, int expectedFilenameLength, String extension) {
        this.parentAbsolutePath = parentAbsolutePath;
        this.prefix = prefix;
        this.expectedFilenameLength = expectedFilenameLength;
        this.extension = extension;
    }

    public void setCharForFilename(char charForFilename) {
        this.charForFilename = charForFilename;
    }

    @Override
    public File provideFile() {
        StringBuilder fileNameBuilder = new StringBuilder(String.format("%02x", (int) this.charForFilename));

        int prefixLength = this.expectedFilenameLength - fileNameBuilder.length();
        for (int i = 0; i < prefixLength; i++) {
            fileNameBuilder.insert(0, this.prefix);
        }

        fileNameBuilder.append(this.extension);

        return new File(this.parentAbsolutePath, fileNameBuilder.toString());
    }
}
