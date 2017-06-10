package ui;

import data.image.AbstractBitmap;
import data.mosaic.MosaicMaker;
import data.mosaic.MosaicTile;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by dd on 03.06.17.
 */
public class SimpleConsole {
    private static Scanner in = new Scanner(System.in);;
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
        return Arrays.stream(root.listFiles()).filter(file -> !file.isDirectory()).collect(Collectors.toList());
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
        MosaicMaker.ProgressCallback progress = new MosaicMaker.ProgressCallback() {
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

    private static AbstractBitmap makeMosaic(String mosaicType, String[] typeParams, File sourceFile, List<File> analyzationFiles, MosaicMaker.ProgressCallback progress) {
        FileMosaicMaker maker = new FileMosaicMaker(analyzationFiles);
        AbstractBitmap result = null;
        switch (mosaicType) {
            case "rect":
                result = makeMosaicRect(typeParams, sourceFile, progress, maker);
                break;
            case "multirect":
                result = makeMosaicMultiRect(typeParams, sourceFile, progress, maker);
                break;
            case "fixedlayer":
                result = makeMosaicFixedLayer(typeParams, sourceFile, progress, maker);
                break;
            case "autolayer":
                result = makeMosaicAutoLayer(typeParams, sourceFile, progress, maker);
                break;
            case "circle":
                result = makeMosaicCircle(typeParams, sourceFile, progress, maker);
                break;
            case "lego":
                result = makeMosaicLego(typeParams, sourceFile, progress, maker);
                break;
            case "svd":
                result = makeMosaicSVD(typeParams, sourceFile, progress, maker);
                break;
            default:
                System.out.println("Unknown mosaic type " + mosaicType);
                break;
        }
        return result;
    }

    private static AbstractBitmap makeMosaicSVD(String[] typeParams, File sourceFile, MosaicMaker.ProgressCallback progress, FileMosaicMaker maker) {
        double mergeFactor = 0.7;
        if (typeParams.length > 0) {
            mergeFactor = parseDoubleSafe(typeParams[0], mergeFactor);
        }
        return maker.makeSVD(sourceFile, mergeFactor, progress);
    }

    private static AbstractBitmap makeMosaicAutoLayer(String[] typeParams, File sourceFile, MosaicMaker.ProgressCallback progress, FileMosaicMaker maker) {
        double mergeFactor = 0.5;
        if (typeParams.length > 0) {
            mergeFactor = parseDoubleSafe(typeParams[0], mergeFactor);
        }
        return maker.makeAutoLayer(sourceFile, mergeFactor, progress);
    }

    private static AbstractBitmap makeMosaicFixedLayer(String[] typeParams, File sourceFile, MosaicMaker.ProgressCallback progress, FileMosaicMaker maker) {
        int layerCount = 3;
        if (typeParams.length > 0) {
            layerCount = parseIntegerSafe(typeParams[0], layerCount);
        }
        return maker.makeFixedLayer(sourceFile, layerCount, progress);
    }


    private static AbstractBitmap makeMosaicCircle(String[] typeParams, File sourceFile, MosaicMaker.ProgressCallback progress, FileMosaicMaker maker) {
        int rows = 5;
        int columns = 5;
        if (typeParams.length > 0) {
            rows = parseIntegerSafe(typeParams[0], rows);
        }
        if (typeParams.length > 1) {
            columns = parseIntegerSafe(typeParams[1], columns);
        }
        return maker.makeCircle(sourceFile, rows, columns, progress);
    }

    private static AbstractBitmap makeMosaicLego(String[] typeParams, File sourceFile, MosaicMaker.ProgressCallback progress, FileMosaicMaker maker) {
        int rows = 5;
        int columns = 5;
        if (typeParams.length > 0) {
            rows = parseIntegerSafe(typeParams[0], rows);
        }
        if (typeParams.length > 1) {
            columns = parseIntegerSafe(typeParams[1], columns);
        }
        return maker.makeLego(sourceFile, rows, columns, progress);
    }

    private static AbstractBitmap makeMosaicRect(String[] typeParams, File sourceFile, MosaicMaker.ProgressCallback progress, FileMosaicMaker maker) {
        int rows = 5;
        int columns = 5;
        if (typeParams.length > 0) {
            rows = parseIntegerSafe(typeParams[0], rows);
        }
        if (typeParams.length > 1) {
            columns = parseIntegerSafe(typeParams[1], columns);
        }
        return maker.makeRect(sourceFile, rows, columns, progress);
    }

    private static AbstractBitmap makeMosaicMultiRect(String[] typeParams, File sourceFile, MosaicMaker.ProgressCallback progress, FileMosaicMaker maker) {
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
        return maker.makeMultiRect(sourceFile, rows, columns, mergeFactor, progress);
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
