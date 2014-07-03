package ts.mcmapexporter;

import javax.swing.*;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.FileSystems;
import java.nio.file.FileSystem;

//TODO: Use java.util.Properties to store the settings set by user for map generating in a file.
//      Some properties don't have to be settable from the Gui.
//      If output directory is settable from Gui then maybe filenames should not be settable.
//      OverworldMap.png, NetherMap.png, TheEndMap.png is good default-names.
//      See this example on java Properties: http://crunchify.com/java-properties-file-how-to-read-config-properties-values-in-java/

//TODO: Use dialoge to select output folder.
//      In this folder the map files will be stored (names of files specified in .properties file).
//      Another possibility is to have one save box for each map dimension (3 dimensions)
//      This does not work well if you have multiple maps for each dimension which you can have if
//      you have made a list of map numbers to include in a limited map.

public class Gui extends JFrame implements ActionListener {

	private Dimension WINDOW_START_SIZE = new Dimension(500, 300);
	private Dimension TEXT_FIELD_START_SIZE = new Dimension(100, 20);

	// Components in inputPanel
	JTextField inputFolderBox;
	JButton inputBrowseButton;

	// Components in dimensionPanel
	JPanel dimensionRadioPanel;
	ButtonGroup dimensionButtonGroup;
	JRadioButton dimensionRadios[];

	// Components in scalePanel
	JComboBox<Integer> scaleSelector;

	// // Components in outputPanel
	// JTextField outputFolderBox;
	// JButton outputBrowseButton;

	// Components in startPanel
	JButton startButton;

	int MAX_SCALE = 4;
	
	private String getDefaultInputFolder() {
		// TODO: Test/Fix for windows
		// TODO: Add also for Mac and Linux
		String osName = System.getProperty("os.name");
		if (osName.equalsIgnoreCase("Windows")) {
			return "%appdata%\.minecraft";
		} else if (osName.equalsIgnoreCase("Mac")) {
			return "~/Library/Application Support/minecraft";
		} else if (osName.equalsIgnoreCase("Linux")) {
			return "~/.minecraft";
		} else {
			return "";
		}
	}

	Gui() {

		JPanel windowPanel;

		this.setPreferredSize(WINDOW_START_SIZE);
		windowPanel = new JPanel();
		windowPanel.setLayout(new BoxLayout(windowPanel, BoxLayout.Y_AXIS));
		this.setContentPane(windowPanel);
		this.setTitle("Gui");

		JPanel inputPanel;
		JPanel dimensionPanel;
		JPanel scalePanel;
		JPanel outputPanel;
		JPanel startPanel;

		// create the other panels
		inputPanel = new JPanel();
		dimensionPanel = new JPanel();
		scalePanel = new JPanel();
		// outputPanel = new JPanel();
		startPanel = new JPanel();

		// Components in inputPanel
		inputFolderBox = new JTextField();
		inputFolderBox.setPreferredSize(TEXT_FIELD_START_SIZE);
		inputBrowseButton = new JButton("Browse");
		inputBrowseButton.addActionListener(this);

		// Components in dimensionPanel
		dimensionRadioPanel = new JPanel();
		dimensionRadioPanel.setLayout(new BoxLayout(dimensionRadioPanel,
				BoxLayout.Y_AXIS));
		dimensionButtonGroup = new ButtonGroup();
		dimensionRadios = new JRadioButton[3];
		dimensionRadios[0] = new JRadioButton("Nether");
		dimensionRadios[1] = new JRadioButton("Overworld");
		dimensionRadios[2] = new JRadioButton("The End");

		// Components in scalePanel
		scaleSelector = new JComboBox<Integer>();
		for (int i = 0; i <= MAX_SCALE; ++i) {
			scaleSelector.addItem(i);
		}
		scaleSelector.setSelectedIndex(2);

		// // Components in outputPanel
		// outputLabel = new JLabel("Output file: ");
		// outputFolderBox = new JTextField();
		// outputFolderBox.setPreferredSize(TEXT_FIELD_START_SIZE);
		// outputBrowseButton = new JButton("Browse");
		// outputBrowseButton.addActionListener(this);

		// components in startPanel
		startButton = new JButton("Create Map");
		startButton.addActionListener(this);

		// Do all the adding

		windowPanel.add(inputPanel);
		windowPanel.add(dimensionPanel);
		windowPanel.add(scalePanel);
		// windowPanel.add(outputPanel);
		windowPanel.add(startPanel);

		inputPanel.add(new JLabel("Input Map Folder: "));
		inputPanel.add(inputFolderBox);
		inputPanel.add(inputBrowseButton);

		dimensionPanel.add(new JLabel("dimension: "));
		dimensionPanel.add(dimensionRadioPanel);
		for (int i = 0; i < dimensionRadios.length; ++i) {
			dimensionButtonGroup.add(dimensionRadios[i]);
			dimensionRadioPanel.add(dimensionRadios[i]);
		}

		scalePanel.add(new JLabel("Scale: "));
		scalePanel.add(scaleSelector);

		// outputPanel.add(outputLabel);
		// outputPanel.add(outputFolderBox);
		// outputPanel.add(outputBrowseButton);

		startPanel.add(startButton);

		inputFolderBox.setText(getDefaultInputFolder());
		dimensionRadios[1].setSelected(true);
		// outputFolderBox.setText("output");

	}

	public static void main(String[] args) {
		Gui window = new Gui();
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		window.pack();
		window.setVisible(true);
	}

	public void actionPerformed(ActionEvent e) {
		Object o = e.getSource();
		FileSystem fs = FileSystems.getDefault();

		if (o == inputBrowseButton) {
			Path p = performSelectFolder(false);
			if (p != null) {
				inputFolderBox.setText(p.toString());
			}
			// } else if (o == outputBrowseButton) {
			// Path p = performSelectFolder();
			// if (p != null) {
			// outputFolderBox.setText(p.toString());
			// }
		} else if (o == startButton) {

			String dimension = "";
			for (int i = 0; i < dimensionRadios.length; ++i) {
				if (dimensionRadios[i].isSelected()) {
					dimension = dimensionRadios[i].getText();
					break;
				}
			}
			if (dimension == "") {
				JOptionPane.showMessageDialog(null,
						"No dimension has been selected.");
				return;
			}

			String inputFolder = inputFolderBox.getText();
			if (inputFolder.isEmpty()) {
				JOptionPane.showMessageDialog(null,
						"Please set the input folder path.");
				return;
			}

			String outputFile = "CombinedMap.png";
			// String outputFolder = outputFolderBox.getText();
			// if (outputFolder.isEmpty()) {
			// JOptionPane.showMessageDialog(null,
			// "Please set an output file path.");
			// return;
			// }

			int scale = (Integer) (scaleSelector.getSelectedItem());

			// TODO: combineToImage should return true/false to indicate error
			// or not. Print error message to a field in the Combiner object.
			Combiner.combineToImage(fs.getPath(inputFolder), dimension, scale,
					fs.getPath(outputFile));
			JOptionPane.showMessageDialog(null, "Map stored to " + outputFile);
		}
	}

	public Path performSelectFolder(boolean save) {
		JFileChooser fc = new JFileChooser(inputFolderBox.getText());
		fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		if (save) {
			fc.showSaveDialog(this);
		} else {
			fc.showOpenDialog(this);
		}
		File res = fc.getSelectedFile();
		return res==null?null:res.toPath();
	}
}