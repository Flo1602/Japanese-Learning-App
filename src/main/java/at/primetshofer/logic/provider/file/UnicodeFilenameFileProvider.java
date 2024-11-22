package at.primetshofer.logic.provider.file;

import java.io.File;

public class UnicodeFilenameFileProvider implements IFileProvider {

    private final String parentAbsolutePath;
    private final char charForFilename;
    private final char prefix;
    private final int expectedFilenameLength;
    private final String extension;

    public UnicodeFilenameFileProvider(String parentAbsolutePath, char charForFilename, char prefix, int expectedFilenameLength, String extension) {
        this.parentAbsolutePath = parentAbsolutePath;
        this.charForFilename = charForFilename;
        this.prefix = prefix;
        this.expectedFilenameLength = expectedFilenameLength;
        this.extension = extension;
    }

    @Override
    public File provideFile() {
        StringBuilder fileNameBuilder = new StringBuilder(String.format("%02x", (int) this.charForFilename));

        int prefixLength = this.expectedFilenameLength - fileNameBuilder.length();
        for (int i = 0; i < prefixLength; i++) {
            fileNameBuilder.insert(0, this.prefix);
        }

        fileNameBuilder.append(this.extension);

        File file = new File(this.parentAbsolutePath, fileNameBuilder.toString());
        System.out.println(file.getAbsoluteFile());
        return file;
    }
}
