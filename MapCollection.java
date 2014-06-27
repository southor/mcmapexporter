import java.io.File;
import java.nio.file.Path;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.util.ArrayList;

// TODO: Add ability to output each loaded map as an image with a corresponding name to the number of the map

// TODO: Ability to print map numbers on the image, every map with the same center position must compete who will get its name there?

// TODO: Create a new tool where you can input the coordinates of a location and you get a list of maps that overlaps with that location.

// TODO: Read last time written date of the files, use this for secondary sorting of the maps (after zoom level).

// TODO: Add ability to read a list of maps id's in a file and only include those id's in the list

class MapCollection {
    private ArrayList<Map> maps;
    int dimension;

    class MapScaleComparator implements java.util.Comparator<Map> {
	public int compare(Map m1, Map m2) {
	    if (m1.getScale() < m2.getScale()) return 1;
	    else if (m1.getScale() > m2.getScale()) return -1;
	    else if (m1.getLastModified() > m2.getLastModified()) return 1;
	    else if (m1.getLastModified() < m2.getLastModified()) return -1;
	    else return 0;
	}
    }

    public MapCollection(Path directory, int dimension) throws java.io.IOException {
        maps = new ArrayList<Map>();
        this.dimension = dimension;
        File dir = directory.toFile();
        
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            for(File file : files) {
                if ( ! file.isFile()) continue;
                if ( ! couldBeAMap(file)) continue;
                Map.ReadResult mapDimension = new Map.ReadResult();
                Map map = new Map(file, mapDimension);
                if (map.isValid() && (mapDimension.value == dimension)) {
                    maps.add(map);
                }
            }
        }

	// Sort the maps by scale value. High scales first, low scales last.
	MapScaleComparator c = new MapScaleComparator();
	java.util.Collections.sort(maps, c);
    }

    // Tries to find a reason why a file is not a minecraft map without opening the file
    public boolean couldBeAMap(File file) {
	return file.getName().substring(0, 3).equalsIgnoreCase("map");
    }

    public int getNMaps() {
	return maps.size();
    }

    // Combines the maps into one single map
    public Map combine(int scale) {
        // check maps not null
        if (maps == null) return null;
	if (maps.size() == 0) return null;
        // iterate maps and find min and max locations
        int beginX = Integer.MAX_VALUE;
	int beginZ = Integer.MAX_VALUE;
        int endX = Integer.MIN_VALUE;
	int endZ = Integer.MIN_VALUE;
        for(Map map : maps) {
            beginX = Math.min(beginX, map.getBeginX());
            beginZ = Math.min(beginZ, map.getBeginZ());
            endX = Math.max(endX, map.getEndX());
            endZ = Math.max(endZ, map.getEndZ());
        }
        assert(beginX < Integer.MAX_VALUE);
        assert(beginZ < Integer.MAX_VALUE);
	assert(endX > Integer.MIN_VALUE);
	assert(endZ > Integer.MIN_VALUE);
        assert(endX > beginX);
        assert(endZ > beginZ);
        // create map object
        Map cMap = new Map(beginX, beginZ, endX, endZ, scale);
        // fill map with the data from maps.
        for (Map map : maps) {
            // maps should be sorted so highest scale maps is drawn first
            // either use SortedList or use Collections.sort in the beginning of this function
            map.drawToMap(cMap);
        }
        // return result
        return cMap;
    }

    public boolean exportImage(Path file, int scale) {
        Map combinedMap = combine(scale);
        if (combinedMap == null) return false;
        return combinedMap.exportImage(file.toFile(), dimension);
    }
    
};