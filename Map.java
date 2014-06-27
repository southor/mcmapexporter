import java.io.File;
import java.nio.file.Files;
import java.nio.ByteBuffer;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.zip.GZIPInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;


class Map {

    private int centerX;
    private int centerZ;
    private int width;
    private int height;
    private int scale;
    private long lastModified;
    private byte[] pixels;

    public static final int OVERWORLD = 0;
    public static final int NETHER = -1;
    public static final int THE_END = 1;

    private static int N_BASE_COLORS = 36;

    private static boolean GZIPPED = true; // true if the files being read is gzipped.

    private static int[] baseColorTable = new int[N_BASE_COLORS];

    private static String charset = "US-ASCII";
    //private static String charset = "UNICODE";

    private static int makeRGB(int r, int g, int b) {
	int rgb = r;
	rgb = (rgb << 8) + g;
	rgb = (rgb << 8) + b;
	return rgb;
    }

    private static int getRed(int rgb) {
	return (rgb & 0xFF0000) >> 16;
    }

    private static int getGreen(int rgb) {
	return (rgb & 0x00FF00) >> 8;
    }

    private static int getBlue(int rgb) {
	return rgb & 0x0000FF;
    }

    private static boolean setBaseColor(int baseColorId, int r, int g, int b) {
	int rgb = makeRGB(r, g, b);
	boolean indexOK = baseColorId >= 0 && baseColorId < baseColorTable.length;
	if (indexOK) {
	    baseColorTable[baseColorId] = rgb;
	}
	return indexOK;
    }

    public static boolean isExplored(int colorId) {
	return colorId != 0;
    }

    public static void setBaseColors() {
	setBaseColor(0,                 127,127,127);
	setBaseColor(1, 		127,178,56);
	setBaseColor(2, 		247,233,163);
	setBaseColor(3, 		167,167,167);
	setBaseColor(4, 		255,0,0);
	setBaseColor(5, 		160,160,255);
	setBaseColor(6, 		167,167,167);
	setBaseColor(7, 		0,124,0);
	setBaseColor(8, 		255,255,255);
	setBaseColor(9, 		164,168,184);
	setBaseColor(10, 		183,106,47);
	setBaseColor(11, 		112,112,112);
	setBaseColor(12, 		64,64,255);
	setBaseColor(13, 		104,83,50);
	setBaseColor(14, 		255,252,245);
	setBaseColor(15, 		216,127,51);
	setBaseColor(16, 		178,76,216);
	setBaseColor(17, 		102,153,216);
	setBaseColor(18, 		229,229,51);
	setBaseColor(19, 		127,204,25);
	setBaseColor(20, 		242,127,165);
	setBaseColor(21, 		76,76,76);
	setBaseColor(22, 		153,153,153);
	setBaseColor(23, 		76,127,153);
	setBaseColor(24, 		127,63,178);
	setBaseColor(25, 		51,76,178);
	setBaseColor(26, 		102,76,51);
	setBaseColor(27, 		102,127,51);
	setBaseColor(28, 		153,51,51);
	setBaseColor(29, 		25,25,25);
	setBaseColor(30, 		250,238,77);
	setBaseColor(31, 		92,219,213);
	setBaseColor(32, 		74,128,255);
	setBaseColor(33, 		0,217,58);
	setBaseColor(34, 		21,20,31);
	setBaseColor(35, 		112,2,0);
    }

    public Map(int beginX, int beginZ, int endX, int endZ, int scale) {
	boolean print = false;
        this.scale = scale;
	this.lastModified = 0;
        int sizeX = endX - beginX;
        int sizeZ = endZ - beginZ;
        this.centerX = beginX + sizeX/2;
        this.centerZ = beginZ + sizeZ/2;
        this.width = worldToMap(sizeX);
        this.height = worldToMap(sizeZ);

	if (print) {
	    System.out.println("Created image width=" + width + ", height=" + height);
	    System.out.println("beginX = " + beginX);
	    System.out.println("beginZ = " + beginZ);
	    System.out.println("endX = " + endX);
	    System.out.println("endZ = " + endZ);
	}


	this.pixels = new byte[sizeX*sizeZ];

	// Set all the pixel values to the unexplored color.
	for(int x=0; x<sizeX; ++x) {
	    for(int z=0; z<sizeZ; ++z) {
		writePixel(x, z, (byte)0);
	    }
	}
    }
    
    private boolean matchInByteArr(byte[] array, int i, byte[] matchArray) {
	boolean print = false;
        if (array[i] != matchArray[0]) return false;
        byte[] subArray = java.util.Arrays.copyOfRange(array, i, i + matchArray.length);
	assert(subArray.length == matchArray.length);
	if (print) {
	    try {
		System.out.println("subArray = \"" + new String(subArray, charset) + "\" matchArray = \"" + new String(matchArray, charset) + "\"");
	    } catch(UnsupportedEncodingException e) {
		System.out.println("UnsupportedEncodingException: " + charset);
	    }
	}
	boolean res = java.util.Arrays.equals(subArray, matchArray);
	if (res && print) System.out.println("*matched*");
        return res;
    }
    
    public static class ReadResult {
        public int length;
        public int value;
    };

    private byte[] readColors(byte[] fileArray, int startI, int nBytes) {
	return java.util.Arrays.copyOfRange(fileArray, startI, startI + nBytes);
    }
    
    /**
     * @param length Number of bytes read. out parameter.
     * @param value Resulting value, out parameter.
     */
    private void readKeyAndInt(byte[] fileArray, int startI, byte[] key, ReadResult res) {
        int i = startI + key.length;
        ByteBuffer bb = ByteBuffer.wrap(fileArray);
        res.value = bb.getInt(i);
        i += 4;
        res.length = i - startI;
    }
    
    /**
     * @param length Number of bytes read. out parameter.
     * @param value Resulting value, out parameter.
     */
    private void readKeyAndShort(byte[] fileArray, int startI, byte[] key, ReadResult res) {
        int i = startI + key.length;
        ByteBuffer bb = ByteBuffer.wrap(fileArray);
        res.value = bb.getShort(i);
        i += 2;
        res.length = i - startI;
    }
    
    /**
     * @param length Number of bytes read. out parameter.
     * @param value Resulting value, out parameter.
     */
     private void readKeyAndByte(byte[] fileArray, int startI, byte[] key, ReadResult res) {
        int i = startI + key.length;
        res.value = fileArray[i];
        i += 1;
        res.length = i - startI;
     }

    private static byte[] loadFile(File file) throws IOException {
        return Files.readAllBytes(file.toPath());
    }

    private static byte[] loadGZippedFile(File file) throws IOException {
	boolean useGZip = true;

	FileInputStream fis = new FileInputStream(file);
	java.util.zip.GZIPInputStream gis = null;
	InputStream is = fis;
	if (useGZip) {
	    gis = new GZIPInputStream(fis);
	    is = gis;
	}

	ArrayList<byte[]> buffers = new ArrayList<byte[]>();
	byte[] buffer;
	int totalSize = 0;
	while(true) {
	    buffer = new byte[1024];
	    int n = is.read(buffer);

	    if (n == -1) {
		break;
	    }
	    totalSize += n;
	    if ((totalSize == Integer.MAX_VALUE) || (totalSize < 0)) {
		System.out.println("The input file is too large!");
		if (gis != null) {
		    gis.close();
		}
		is.close();
		return null;
	    }

	    if (n == 0) {
		// just try again
	    } else if (n < buffer.length) {
		byte[] tmpBuffer = buffer;
		buffer = java.util.Arrays.copyOfRange(tmpBuffer, 0, n);
		buffers.add(buffer);
		//System.out.print(new String(buffer, charset));
	    } else {
		assert(n == buffer.length);
		buffers.add(buffer);
		//System.out.print(new String(buffer, charset));
	    }
	    
	}
	//System.out.println("----------------------------");
	int testRead = is.read();
	//System.out.println("testRead = " + testRead);
	assert(testRead == -1);
	if (gis != null) {
	    gis.close();
	}
	is.close();
	if (totalSize > 0) {
	    byte[] resArray = new byte[totalSize];
	    int i = 0;
	    java.util.Iterator<byte[]> it = buffers.iterator();
	    while(it.hasNext()) {
		buffer = it.next();
		for(int j=0; j<buffer.length; ++j) {
		    if (i >= resArray.length) {
			System.out.println("buffer.length = " + buffer.length);
			System.out.println("j = " + j);
			System.out.println("Internal Error in loadGZippedFile, cannot read file.");
			assert(false);
			return null;
		    }
		    resArray[i] = buffer[j];
		    ++i;
		}
	    }
	    assert(i == resArray.length);
	    return resArray;
	} else {
	    return null;
	}
    }

    /**
     * Loads a minecraft map item
     * @param dimension An out parameter describing which dimension the loaded map represents.
     */
    public Map(File file, ReadResult dimension) throws IOException {

	//System.out.println("********************************");
	//System.out.println("Loading file: " + file);

	byte[] fileArray = GZIPPED ? loadGZippedFile(file) : loadFile(file);
	
	//java.io.FileOutputStream fileOuputStream = new java.io.FileOutputStream("/media/tornic/Voyager/Nytt/programming/mapcombiner/read_res.map"); 
	//fileOuputStream.write(fileArray);
	//fileOuputStream.close();
 

        byte[] dataStr = "data".getBytes(charset);
        byte[] scaleStr = "scale".getBytes(charset);
        byte[] dimensionStr = "dimension".getBytes(charset);
        byte[] heightStr = "height".getBytes(charset);
        byte[] widthStr = "width".getBytes(charset);
        byte[] xCenterStr = "xCenter".getBytes(charset);
        byte[] zCenterStr = "zCenter".getBytes(charset);
        byte[] colorsStr = "colors".getBytes(charset);
        
        ReadResult res = new ReadResult();

        for(int i=0; i<fileArray.length; ++i) {
            if (matchInByteArr(fileArray, i, scaleStr)) {
                readKeyAndByte(fileArray, i, scaleStr, res);
                this.scale = res.value;
                i += res.length;
            } else if (matchInByteArr(fileArray, i, dimensionStr)) {
                readKeyAndByte(fileArray, i, dimensionStr, res);
		//System.out.println("res.value = " + res.value);
		dimension.value = res.value;
                i += res.length;
            } else if (matchInByteArr(fileArray, i, heightStr)) {
                readKeyAndShort(fileArray, i, heightStr, res);
                this.height = res.value;
                i += res.length;
            } else if (matchInByteArr(fileArray, i, widthStr)) {
                readKeyAndShort(fileArray, i, widthStr, res);
                this.width = res.value;
                i += res.length;
            } else if (matchInByteArr(fileArray, i, xCenterStr)) {
                readKeyAndInt(fileArray, i, xCenterStr, res);
                this.centerX = res.value;
                i += res.length;
            } else if (matchInByteArr(fileArray, i, zCenterStr)) {
                readKeyAndInt(fileArray, i, zCenterStr, res);
                this.centerZ = res.value;
                i += res.length;
            } else if (matchInByteArr(fileArray, i, colorsStr)) {
		readKeyAndInt(fileArray, i, colorsStr, res);
		int size = res.value; // number of bytes in colors (width*height)
		i += res.length;
		this.pixels = readColors(fileArray, i, size);
            }
        }

	this.lastModified = file.lastModified();
	
	//System.out.println("loaded image width=" + width + ", height=" + height);

	//System.out.println("centerX = " + centerX);
	//System.out.println("centerZ = " + centerZ);
	//System.out.println("width = " + width);
	//System.out.println("height = " + height);
	//System.out.println("scale = " + scale);
	//System.out.println("dimension = " + dimension);
    }
    
    public boolean isValid() {
        return (width > 0) && (height > 0) && (scale >= 0) && (pixels != null);
    }

    public void setInvalid() {
        width = 0;
        height = 0;
        scale = 0;
        pixels = null;
    }

    public int getScale()       { return scale; }

    public long getLastModified() { return lastModified; }
    
    public int getSizeX()       { return mapToWorld(width);      }
    public int getSizeZ()       { return mapToWorld(height);     }

    public int getBeginX()      { return centerX - getSizeX()/2; }
    public int getBeginZ()      { return centerZ - getSizeZ()/2; }

    public int getEndX()        { return centerX + getSizeX()/2; }
    public int getEndZ()        { return centerZ + getSizeZ()/2; }

    public int worldToMap(int units) {
        for(int i=0; i<scale; ++i) {
            units = units / 2;
        }
        return units;
    }

    public int mapToWorld(int units) {
        for(int i=0; i<scale; ++i) {
            units = units * 2;
        }
        return units;
    }

    public int worldXToMapX(int x) {
        x = x - getBeginX();
        return worldToMap(x);
    }

    public int worldZToMapZ(int z) {
        z = z - getBeginZ();
        return worldToMap(z);
    }
    
    private class Rectangle {
        public int beginX;
        public int beginZ;
        public int endX;
        public int endZ;
    };
    
    public Rectangle getRectangle() {
        Rectangle r = new Rectangle();
        r.beginX = getBeginX();
        r.beginZ = getBeginZ();
        r.endX = getEndX();
        r.endZ = getEndZ();
        return r;
    }

    public static int makeColorId(int baseColorId, int shadeId) {
	return baseColorId * 4 + shadeId;
    }
    
    public static int getBaseColorId(int colorId) {
	return (colorId >> 2);
    }

    public static int getColorShadeId(int colorId) {
	return colorId & 0x03;
    }

    // Shade value, between 0 and 255
    public static int shadeValueToId(int value) {
	if (value > 235) return 2; // 255
	else if (value > 200) return 1; // 220
	else if (value > 155) return 0; // 180
	else return 3; // 135
    }

    public static int shadeIdToValue(int shadeId) {
	int val;
	switch(shadeId) {
	case 0:
	    val = 180;
	    break;
	case 1:
	    val = 220;
	    break;
	case 2:
	    val = 255;
	    break;
	case 3:
	    val = 135;
	    break;
	default:
	    assert(false);
	    val = 255;
	    break;
	};
	return val;
    }
    
    public static java.awt.Color colorIdToColor(int colorId) {
        int baseColorId = getBaseColorId(colorId);
	int shadeId = getColorShadeId(colorId);

	// apply base color
        
	int baseColor = baseColorTable[baseColorId];

	// apply shade color

	//System.out.println("shadeId = " + shadeId);
	double mult = (double)shadeIdToValue(shadeId);;
	//System.out.println("mult = " + mult);

	double r = getRed(baseColor) * mult / 255.0;
	double g = getGreen(baseColor) * mult / 255.0;
	double b = getBlue(baseColor) * mult / 255.0;

	// return color

	int ir = java.lang.Math.min((int)(r+0.5), 255);
	int ig = java.lang.Math.min((int)(g+0.5), 255);
	int ib = java.lang.Math.min((int)(b+0.5), 255);

	//System.out.println(ir);

	assert(ir >= 0);
	assert(ig >= 0);
	assert(ib >= 0);

	return new java.awt.Color(ir, ig, ib); // the final color
    }
    
    public boolean exportImage(File file, int dimension) {
	if (width <= 0 || height <= 0) {
	    System.out.println("Cannot save image to disk, image has zero size.");
	    return false;
	}
        java.awt.image.BufferedImage bi = new java.awt.image.BufferedImage(width, height, java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D sg = bi.createGraphics();
        for(int z=0; z<height; ++z) {
            for(int x=0; x<width; ++x) {
                int pixel = readPixel(x, z);
		java.awt.Color color = colorIdToColor(pixel);
                sg.setColor(color);
                sg.fill(new java.awt.Rectangle(x, z, 1, 1));
            }
        }
	try {
	    String filename = file.getName();
	    String extension = "";
	    int i = filename.lastIndexOf('.');
	    if (i >= 0) {
		extension = filename.substring(i+1);
		javax.imageio.ImageIO.write(bi, extension, file);
	    } else {
		System.out.println("Error: Where not able to determine file extension.");
		return false;
	    }
	} catch(IOException e) {
	    System.out.println("Error: IOException, cannot write image to disk.");
	    System.out.println("Message = " + e.getMessage());
	    return false;
	}
	return true;
    }

    /**
     *  @param overlap Output stored in this parameter
     */
    static private boolean calcOverlap(Map map1, Map map2, Rectangle overlap) {
        // get every begin/end value
        int map1BeginX = map1.getBeginX();
        int map1BeginZ = map1.getBeginZ();
        int map1EndX = map1.getEndX();
        int map1EndZ = map1.getEndZ();
        int map2BeginX = map1.getBeginX();
        int map2BeginZ = map1.getBeginZ();
        int map2EndX = map1.getEndX();
        int map2EndZ = map1.getEndZ();

        // Do we have an overlap?
        
        if (map1BeginX > map2EndX) return false;
        if (map2BeginX > map1EndX) return false;
        if (map1BeginZ > map2EndZ) return false;
        if (map2BeginZ > map1EndZ) return false;

        // Now we know we have an overlap
        // Get begin and end values
        overlap.beginX = Math.max(map1BeginX, map2BeginX);
        overlap.beginZ = Math.max(map1BeginZ, map2BeginZ);
        overlap.endX = Math.max(map1EndX, map2EndX);
        overlap.endZ = Math.max(map1EndZ, map2EndZ);

	return true;
    }

    byte readPixel(int x, int z) {
        return pixels[x + z*width];
    }

    void writePixel(int x, int z, byte pixel) {
        pixels[x + z*width] = pixel;
    }

    // Decomposes colorId to baseColorId and shadeId and counts baseColorId
    // When returning result shadeId is added again.
    private class ColorCounter {
        private int[] colorCounts = new int[N_BASE_COLORS];
	// TODO: Keep an array of shades too, keep one shade sum for each baseColoe
	private int shadeSum = 0;
	private int nCounts = 0;

	private int getShadeValue() {
	    return (int)((double)shadeSum / (double)nCounts);
	}

        public void count(int colorId) {
	    assert(colorId >= 0);
	    int baseColorId = getBaseColorId(colorId);
            colorCounts[baseColorId] += 1;
	    shadeSum += shadeIdToValue(getColorShadeId(colorId));
	    ++nCounts;
        }

	// @return colorId
        public int getHighestRes() {
            int n = colorCounts.length;
            int highestCount = 0;
            int highestRes = 0;
            for(int i=0; i<n; ++i) {
                if (colorCounts[i] > highestCount) {
                    highestCount = colorCounts[i];
		    assert(i >= 0 && i < 128);
		    highestRes = i;
                }
            }
	    int shadeId = shadeValueToId(getShadeValue());
            return makeColorId(highestRes, shadeId);
        }

	
    };
       
    // this map will draw itself onto 'map'. Will skip drawing the pixels with the "unexplored" color.
    public void drawToMap(Map map) {
        Map lowResMap, highResMap;
        Map readMap = this;
        Map writeMap = map;
        Rectangle overlap = new Rectangle();

	//System.out.println("readmap scale = " + readMap.scale);
	//System.out.println("writemap scale = " + writeMap.scale);
        
        if ( ! calcOverlap(readMap, writeMap, overlap)) {
            return;
        }

	int highResScale;
	int lowResScale;

        if (readMap.scale >= writeMap.scale) {
            lowResMap = readMap;
            highResMap = writeMap;
	    lowResScale = readMap.scale;
	    highResScale = writeMap.scale;
        } else {
            lowResMap = writeMap;
            highResMap = readMap;
	    lowResScale = writeMap.scale;
	    highResScale = readMap.scale;
        }

	assert(lowResScale >= highResScale);

	int scaleMult = 1;
	// TODO replace with shift operations?
	for(int i=highResScale; i<lowResScale; ++i) {
	    scaleMult *= 2;
	}
        
        int highResBeginX = highResMap.worldXToMapX(overlap.beginX);
        int highResBeginZ = highResMap.worldZToMapZ(overlap.beginZ);
        //int highResEndX = highResMap.worldXToMapX(overlap.endX);
        //int highResEndZ = highResMap.worldZToMapZ(overlap.endZ);
        int lowResBeginX = lowResMap.worldXToMapX(overlap.beginX);
        int lowResBeginZ = lowResMap.worldZToMapZ(overlap.beginZ);
        int lowResEndX = lowResMap.worldXToMapX(overlap.endX);
        int lowResEndZ = lowResMap.worldZToMapZ(overlap.endZ);

	int nXIterations = lowResEndX - lowResBeginX;
	int nZIterations = lowResEndZ - lowResBeginZ;

	for(int z=0; z<nZIterations; ++z) {
	    for(int x=0; x<nXIterations; ++x) {
		
		int lowX = lowResBeginX + x;
		int lowZ = lowResBeginZ + z;

		// if lowResMap == readMap  : copy same value to all write positions
		// if lowresMap == writeMap : Use an array of all possible colors/block types and count the occurances of each kind
		//                            The kind that has the highest kind is written to the writeMap 
            
                if (lowResMap == readMap) {
                    assert(highResMap == writeMap);
                    byte pixel = lowResMap.readPixel(lowX, lowZ);
		    // If pixel is not the explored color, then write pixels 
		    if (pixel != 0) {
			for(int subX=0; subX<scaleMult; ++subX) {
			    for(int subZ=0; subZ<scaleMult; ++subZ) {
				int highX = highResBeginX + x * scaleMult + subX;
				int highZ = highResBeginZ + z * scaleMult + subZ;
				highResMap.writePixel(highX, highZ, pixel);
			    }
			}
		    }
                } else {
                    assert(lowResMap == writeMap);
                    assert(highResMap == readMap);
		    // TODO: Reuse ColorCounter in the loop (must add reset methods).
                    ColorCounter colorCounter = new ColorCounter();
                    byte pixel;

		    for(int subX=0; subX<scaleMult; ++subX) {
			for(int subZ=0; subZ<scaleMult; ++subZ) {
			    int highX = highResBeginX + x * scaleMult + subX;
			    int highZ = highResBeginZ + z * scaleMult + subZ;
                            pixel = highResMap.readPixel(highX, highZ);
			    colorCounter.count(pixel);
                        }
                    }
                    pixel = (byte)colorCounter.getHighestRes();
		    // If pixel represents the unexplored color, then do not write the pixel.
		    if (isExplored(pixel)) {
			lowResMap.writePixel(lowX, lowZ, pixel);
		    }
                }
            }
        }
    }

};