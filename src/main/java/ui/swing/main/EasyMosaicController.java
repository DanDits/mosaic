package ui.swing.main;

/**
 * Created by dd on 11.07.17.
 */
public class EasyMosaicController {
    private boolean noDuplicates = true;
    private MatcherOption matcherPreference = MatcherOption.COLOR;

    public void setNoDuplicates(boolean noDuplicates) {
        this.noDuplicates = noDuplicates;
        System.out.println("No duplicates=" + noDuplicates);
    }

    public void setMatcherPreference(MatcherOption matcherPreference) {
        this.matcherPreference = matcherPreference;
        System.out.println("Preference=" + matcherPreference);
    }
    public enum MatcherOption {
        RANDOM, COLOR, RESOLUTION

    }
    private enum EffectOption {
        LEGO, SIMPLE_RECTS, RECTS
    }

    public boolean isNoDuplicates() {
        return noDuplicates;
    }

    public MatcherOption getMatcherPreference() {
        return matcherPreference;
    }
}
