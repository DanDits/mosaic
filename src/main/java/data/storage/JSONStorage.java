package data.storage;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.pmw.tinylog.Logger;

import java.io.*;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by dd on 03.06.17.
 */
@SuppressWarnings("unchecked") // as the JSON library does not use generics
public class JSONStorage<S> {

    public boolean saveToJSON(File file, Collection<MosaicTile<S>> tiles) {
        JSONObject root = new JSONObject();
        JSONArray all = new JSONArray();
        tiles.forEach(tile -> all.add(convertToJSON(tile)));
        root.put("tiles", all);
        return save(file, root);
    }

    public Set<MosaicTile<S>> loadFromJSON(File file, TileBuilder<S> builder) {
        JSONParser parser = new JSONParser();

        Set<MosaicTile<S>> tiles = new HashSet<>();
        try {

            Object obj = parser.parse(new FileReader(file));

            JSONObject root = (JSONObject) obj;
            JSONArray allTiles = (JSONArray) root.get("tiles");
            for (Object tileObj : allTiles) {
                MosaicTile<S> tile = readFromJson((JSONObject) tileObj, builder);
                if (tile != null) {
                    tiles.add(tile);
                }
            }
        } catch (FileNotFoundException fnf) {
            Logger.info("Creating new save file at: {}", file);
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
        return tiles;
    }

    private boolean save(File path, JSONObject obj) {
        File parent = path.getParentFile();
        if (!parent.isDirectory() && !parent.mkdirs()) {
            Logger.error("Could not create directories to save to {}", path);
            return false;
        }
        try (FileWriter file = new FileWriter(path)) {
            file.write(obj.toJSONString());
            file.flush();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private MosaicTile<S> readFromJson(JSONObject object, TileBuilder<S> builder) {
        return builder.makeTile((String) object.get("source"), ((Long) object.get("averageARGB")).intValue(),
                ((Long) object.get("width")).intValue(),  ((Long) object.get("height")).intValue());
    }

    private JSONObject convertToJSON(MosaicTile<S> tile) {
        JSONObject obj = new JSONObject();
        obj.put("source", tile.getSource());
        obj.put("averageARGB", tile.getAverageARGB());
        obj.put("width", tile.getWidth());
        obj.put("height", tile.getHeight());
        return obj;
    }
}
