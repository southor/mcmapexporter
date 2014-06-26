import java.nio.file.Path;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;


class Combiner
{

    static void combineToImage(Path mapDirectory, String dimension, int scale, Path outFile) {
	Map.setBaseColors();
	int dimensionInt;
	if (dimension.equalsIgnoreCase("Overworld")) {
	    dimensionInt = Map.OVERWORLD;
	} else if (dimension.equalsIgnoreCase("Nether")) {
	    dimensionInt = Map.NETHER;	    
	} else if (dimension.equalsIgnoreCase("End")) {
    	    dimensionInt = Map.THE_END;
	} else {
	    System.out.println("Incorrect dimension, use Overworld, Nether or End");
	    return;
	}
	System.out.println("dimension = " + dimensionInt);
	MapCollection maps;
	try {
	    maps = new MapCollection(mapDirectory, dimensionInt);
	} catch(java.io.UnsupportedEncodingException e) {
	    System.out.println("Required character encoding not supported on this sytem!");
	    return;
	} catch (java.io.IOException e) {
	    System.out.println("Failed reading files from the directory.");
	    System.out.println("Exception message: " + e.getMessage());
	    e.printStackTrace();
	    return;
	}
	System.out.println("Number of maps loaded: " + maps.getNMaps());
	boolean res = maps.exportImage(outFile, scale);
	if (res) {
	    System.out.println("Image created: " + outFile.toString() + " with scale = " + scale);
	} else {
	    System.out.println("No image was created.");
	}
    }

    public static void main(String[] args) {
        if (args.length < 4) System.out.println("Error: Not enough arguments, see source code.");
        FileSystem fs = FileSystems.getDefault();
        Path mapDirectory = fs.getPath(args[0]);
        String dimension = args[1];
        int scale = Integer.parseInt(args[2]);
        Path outFile = fs.getPath(args[3]);
	System.out.println("map directory = " + mapDirectory);
	System.out.println("dimension = " + dimension);
        combineToImage(mapDirectory, dimension, scale, outFile);
    }

};