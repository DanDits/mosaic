package data.export;

import data.image.AbstractBitmap;
import org.pmw.tinylog.Logger;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

/**
 * Created by dd on 16.07.17.
 */
public class FileBitmapExporter extends AbstractBitmapExporter {
    private final File targetFile;

    public FileBitmapExporter(File targetFile) {
        Objects.requireNonNull(targetFile);
        this.targetFile = targetFile;
    }

    @Override
    public void exportBitmap(AbstractBitmap toExport) throws BitmapExportException {
        try {
            toExport.saveToFile(targetFile);
        } catch (IOException e) {
            Logger.error("Failed exporting bitmap to file: {}", targetFile);
            throw new BitmapExportException(e);
        }
    }
}
