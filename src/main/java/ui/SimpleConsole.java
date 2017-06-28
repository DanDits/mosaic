package ui;

import assembling.ProgressCallback;
import data.image.AbstractBitmap;
import data.image.AbstractBitmapFactory;
import data.mosaic.MosaicTile;
import reconstruction.ReconstructionParameters;
import reconstruction.pattern.PatternReconstructor;
import reconstruction.workers.CirclePatternReconstructor;
import reconstruction.workers.LegoPatternReconstructor;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by dd on 03.06.17.
 */
public class SimpleConsole {
    private static Scanner in = new Scanner(System.in);
    private static final String COMMAND_HELP = "-h";
    private static final String COMMAND_ANALYZE = "-a";
    private static final String COMMAND_MOSAIC = "-m";


    public static void main(String[] args) {
        String command = COMMAND_HELP;
        if (args.length > 0) {
            command = args[0];
        }
        String output;
        switch (command) {
            case COMMAND_ANALYZE:
                if (args.length != 3) {
                    output = "To analyze enter path to directory and save file. E.g. '-a /home/vacation /home/analyzed'.";
                } else {
                    output = doAnalyzation(args[1], args[2]);
                }
                break;
            case COMMAND_MOSAIC:
                if (args.length < 5) {
                    output = "To generate a mosaic specify a path to the source image, path to result image, path to analyzation file(s) and a type with its parameters.";
                } else {
                    output = generateMosaic(args[1], args[2], args[3], args[4], Arrays.copyOfRange(args, 5, args.length));
                }
                break;
            default:
                /* fall through */
            case COMMAND_HELP:
                output = "Enter '-h' to show this help.\n" +
                        "'-a path1 path2' to analyze a collection of images in directory path1 and save to path2\n" +
                        "'-m path1 path2 path3 type [type_params...]' to generate a mosaic of the image at path1, saving the result at path2, getting analyzation results from path3 and using the given mosaic type (see '-t' for details)";
                break;
        }
        System.out.println(output);
    }

    private static List<File> getAnalyzationFiles(File root) {
        List<File> files = new ArrayList<>();
        if (!root.exists()) {
            return files;
        }
        if (!root.isDirectory()) {
            files.add(root);
            return files;
        }
        File[] children = root.listFiles();
        if (children == null) {
            return files;
        }
        return Arrays.stream(children).filter(file -> !file.isDirectory()).collect(Collectors.toList());
    }

    private static int parseIntegerSafe(String toParse, int fallback) {
        try {
            return Integer.parseInt(toParse);
        } catch (NumberFormatException nfe) {
            System.out.println("Not a valid integer:" + nfe);
            return fallback;
        }
    }

    private static double parseDoubleSafe(String toParse, double fallback) {
        try {
            return Double.parseDouble(toParse);
        } catch (NumberFormatException nfe) {
            System.out.println("Not a valid double:" + nfe);
            return fallback;
        }
    }

    private static String generateMosaic(String sourcePath, String targetPath, String analyzationPath, String mosaicType, String[] typeParams) {
        File sourceFile = new File(sourcePath);
        if (!sourceFile.exists() || sourceFile.isDirectory()) {
            return "Given path is not an image file.";
        }
        File targetFile = new File(targetPath);
        if (targetFile.isDirectory()) {
            return "Cannot overwrite directory as target image.";
        }
        List<File> analyzationFiles = getAnalyzationFiles(new File(analyzationPath));
        if (analyzationFiles.size() == 0) {
            return "No files found that store analyzed images at " + analyzationPath;
        }
        ProgressCallback progress = new ProgressCallback() {
            int lastPublishedProgress = -1;
            @Override
            public boolean isCancelled() {
                return false;
            }

            @Override
            public void onProgressUpdate(int progress) {
                final int showPercentageStep = 10;
                if (progress / showPercentageStep > lastPublishedProgress / showPercentageStep) {
                    lastPublishedProgress = (progress / showPercentageStep) * showPercentageStep;
                    System.out.println("Mosaic progress: " + progress + "%");
                }
            }
        };
        AbstractBitmap result = makeMosaic(mosaicType, typeParams, sourceFile, analyzationFiles, progress);
        return saveMosaic(result, targetFile);
    }

    private static AbstractBitmap makeMosaic(String mosaicType, String[] typeParams, File sourceFile, List<File> analyzationFiles, ProgressCallback progress) {
        FileMosaicMaker maker = new FileMosaicMaker(analyzationFiles);
        ReconstructionParameters parameters = null;
        PatternReconstructor.PatternParameters patternParameters = null;
        AbstractBitmap result = null;
        switch (mosaicType) {
            case "rect":
                parameters = makeMosaicRect(typeParams, sourceFile, progress, maker);
                break;
            case "multirect":
                parameters = makeMosaicMultiRect(typeParams, sourceFile, progress, maker);
                break;
            case "fixedlayer":
                parameters = makeMosaicFixedLayer(typeParams, sourceFile, progress, maker);
                break;
            case "autolayer":
                parameters = makeMosaicAutoLayer(typeParams, sourceFile, progress, maker);
                break;
            case "circle":
                patternParameters = makeMosaicCircle(typeParams, sourceFile, progress, maker);
                break;
            case "lego":
                patternParameters = makeMosaicLego(typeParams, sourceFile, progress, maker);
                break;
            case "puzzle":
                parameters = makeMosaicPuzzle(typeParams, sourceFile, progress, maker);
                break;
            case "svd":
                result = makeMosaicSVD(typeParams, sourceFile, progress, maker);
                break;
            default:
                System.out.println("Unknown mosaic type " + mosaicType);
                break;
        }
        if (parameters != null) {
            try {
                result = maker.getMaker().make(parameters, progress);
            } catch (ReconstructionParameters.IllegalParameterException e) {
                System.err.println("Error creating mosaic:" + e);
            }
        }
        if (patternParameters != null) {
            try {
                result = maker.getMaker().make(patternParameters, progress);
            } catch (ReconstructionParameters.IllegalParameterException e) {
                System.err.println("Error creating pattern mosaic:" + e);
            }
        }
        return result;
    }

    private static ReconstructionParameters makeMosaicPuzzle(String[] typeParams, File sourceFile, ProgressCallback progress, FileMosaicMaker maker) {
        int rows = 5;
        int columns = 5;
        if (typeParams.length > 0) {
            rows = parseIntegerSafe(typeParams[0], rows);
        }
        if (typeParams.length > 1) {
            columns = parseIntegerSafe(typeParams[1], columns);
        }
        AbstractBitmap source = maker.loadSourceFitByRowsColumns(sourceFile, rows, columns);
        return maker.getMaker().makePuzzleParameters(source, rows, columns, progress);
    }

    private static AbstractBitmap makeMosaicSVD(String[] typeParams, File sourceFile, ProgressCallback progress, FileMosaicMaker maker) {
        double mergeFactor = 0.7;
        if (typeParams.length > 0) {
            mergeFactor = parseDoubleSafe(typeParams[0], mergeFactor);
        }
        return maker.makeSVD(sourceFile, mergeFactor, progress);
    }

    private static ReconstructionParameters makeMosaicAutoLayer(String[] typeParams, File sourceFile, ProgressCallback progress, FileMosaicMaker maker) {
        double mergeFactor = 0.5;
        if (typeParams.length > 0) {
            mergeFactor = parseDoubleSafe(typeParams[0], mergeFactor);
        }
        AbstractBitmap source = AbstractBitmapFactory.makeInstance(sourceFile).createBitmap();
        return maker.getMaker().makeAutoLayerParameters(source, mergeFactor, progress);
    }

    private static ReconstructionParameters makeMosaicFixedLayer(String[] typeParams, File sourceFile, ProgressCallback progress, FileMosaicMaker maker) {
        int layerCount = 3;
        if (typeParams.length > 0) {
            layerCount = parseIntegerSafe(typeParams[0], layerCount);
        }
        AbstractBitmap source = AbstractBitmapFactory.makeInstance(sourceFile).createBitmap();
        return maker.getMaker().makeFixedLayerParameters(source, layerCount, progress);
    }


    private static PatternReconstructor.PatternParameters makeMosaicCircle(String[] typeParams, File sourceFile, ProgressCallback progress, FileMosaicMaker maker) {
        int rows = 5;
        int columns = 5;
        if (typeParams.length > 0) {
            rows = parseIntegerSafe(typeParams[0], rows);
        }
        if (typeParams.length > 1) {
            columns = parseIntegerSafe(typeParams[1], columns);
        }
        AbstractBitmap source = maker.loadSourceFitByRowsColumns(sourceFile, rows, columns);
        return maker.getMaker().makePatternParameters(source, CirclePatternReconstructor.NAME, rows, columns, progress);
    }

    private static PatternReconstructor.PatternParameters makeMosaicLego(String[] typeParams, File sourceFile, ProgressCallback progress, FileMosaicMaker maker) {
        int rows = 5;
        int columns = 5;
        if (typeParams.length > 0) {
            rows = parseIntegerSafe(typeParams[0], rows);
        }
        if (typeParams.length > 1) {
            columns = parseIntegerSafe(typeParams[1], columns);
        }
        AbstractBitmap source = maker.loadSourceFitByRowsColumns(sourceFile, rows, columns);
        LegoPatternReconstructor.LegoParameters params = (LegoPatternReconstructor.LegoParameters) maker.getMaker().makePatternParameters(source, LegoPatternReconstructor.NAME, rows, columns, progress);
        params.usePalettes = false;
        return params;
    }

    private static ReconstructionParameters makeMosaicRect(String[] typeParams, File sourceFile, ProgressCallback progress, FileMosaicMaker maker) {
        int rows = 5;
        int columns = 5;
        if (typeParams.length > 0) {
            rows = parseIntegerSafe(typeParams[0], rows);
        }
        if (typeParams.length > 1) {
            columns = parseIntegerSafe(typeParams[1], columns);
        }
        AbstractBitmap source = maker.loadSourceFitByRowsColumns(sourceFile, rows, columns);
        return maker.getMaker().makeRectParameters(source, rows, columns, progress);
    }

    private static ReconstructionParameters makeMosaicMultiRect(String[] typeParams, File sourceFile, ProgressCallback progress, FileMosaicMaker maker) {
        int rows = 5;
        int columns = 5;
        double mergeFactor = 0.7;
        if (typeParams.length > 0) {
            rows = parseIntegerSafe(typeParams[0], rows);
        }
        if (typeParams.length > 1) {
            columns = parseIntegerSafe(typeParams[1], columns);
        }
        if (typeParams.length > 2) {
            mergeFactor = parseDoubleSafe(typeParams[2], mergeFactor);
        }
        AbstractBitmap source = maker.loadSourceFitByRowsColumns(sourceFile, rows, columns);
        return maker.getMaker().makeMultiRectParameters(source, rows, columns, mergeFactor, progress);
    }

    private static String saveMosaic(AbstractBitmap result, File targetFile) {
        if (result != null) {
            boolean saving;
            try {
                saving = result.saveToFile(targetFile);
            } catch (IOException e) {
                return "Error saving file: " + e;
            }
            if (!saving) {
                return "Failed saving file.";
            }
            return "Successfully created mosaic at " + targetFile;
        }
        return "Failed creating mosaic!";
    }


    private static String doAnalyzation(String path, String savePath) {
        File file = new File(path);
        File saveFile = new File(savePath);
        if (file.isDirectory()) {
            Set<MosaicTile<String>> tiles = FileMosaicJSONBuilder.loadExistingTiles(saveFile);

            Set<MosaicTile<String>> allTiles = FileMosaicAnalyzer.analyze(tiles, file, saveFile);
            return "Successfully analyzed " + (allTiles.size() - tiles.size()) + " new tiles and saved "
                    + allTiles.size() + " tiles!";
        } else {
            if (file.exists()) {
                return "Enter a path to a directory to analyze!";
            } else {
                return "Directory does not exist.";
            }
        }
    }

    private static String fetchCommand() {
        System.out.print("Enter command: ");
        String input = in.next();
        System.err.println("Entered: " + input);
        return input;
    }
}
