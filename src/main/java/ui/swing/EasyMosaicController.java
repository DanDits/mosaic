package ui.swing;

import data.image.AbstractBitmap;

import java.io.File;
import java.util.Arrays;

/**
 * Created by dd on 11.07.17.
 */
public class EasyMosaicController {
    private File[] sourcesDirectories;

    public void setNoDuplicates(boolean noDuplicates) {
        this.noDuplicates = noDuplicates;
        System.out.println("No duplicates=" + noDuplicates);
    }

    public void setMatcherPreference(MatcherOption matcherPreference) {
        this.matcherPreference = matcherPreference;
        System.out.println("Preference=" + matcherPreference);
    }

    public void chooseSourcesDirectories(File[] sourcesDirectories) {
        this.sourcesDirectories = sourcesDirectories;
        Arrays.stream(sourcesDirectories).forEach(file -> System.out.println(file.getAbsolutePath()));
    }

    public enum MatcherOption {
        RANDOM, COLOR, RESOLUTION
    }

    private enum EffectOption {
        LEGO, SIMPLE_RECTS, RECTS
    }
    private File originalFile;
    private AbstractBitmap original;

    public boolean isNoDuplicates() {
        return noDuplicates;
    }

    public MatcherOption getMatcherPreference() {
        return matcherPreference;
    }

    private boolean noDuplicates = true;
    private MatcherOption matcherPreference = MatcherOption.COLOR;
    private File tilesDirectory; // TODO system images directory
}
