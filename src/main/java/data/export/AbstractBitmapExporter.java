package data.export;

import data.image.AbstractBitmap;

/**
 * Created by dd on 16.07.17.
 */
public abstract class AbstractBitmapExporter {
    // TODO maybe implement ascii bitmap export that exports to a txt file using some ascii art of the image brightness
    public abstract void exportBitmap(AbstractBitmap toExport) throws BitmapExportException;
}
