package ar.com.satragno.osufileeditor;

import java.awt.EventQueue;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class Main {
	private JFrame frame;
	private JLabel filenameLabel;
	private JButton previewButton;
	private JTextArea preview;
	private JButton deleteButton;
	private JCheckBox deleteFoldersCheckBox;
	private Box bottomRow;

	private File folder;
	private HashSet<File> filesToSave;
	private HashSet<File> filesToDelete;
	private int filesProcessed;

	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					Main window = new Main();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	private Main() {
		initialize();
	}

	private void initialize() {
		frame = new JFrame();
		frame.setTitle("OSU file deleter");
		frame.setBounds(100, 100, 627, 551);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.Y_AXIS));

		Box mainBox = new Box(BoxLayout.Y_AXIS);
		JLabel label = new JLabel("BACKUP YOUR DATA BEFORE RUNNING THIS");
		mainBox.add(label);

		JLabel label2 = new JLabel("This will delete all data for which mode does not equal 1");
		mainBox.add(label2);

		JLabel label3 = new JLabel("FOLDER:");
		mainBox.add(label3);
		filenameLabel = new JLabel("No folder selected.");
		mainBox.add(filenameLabel);

		JButton fileButton = new JButton("Select folder...");
		fileButton.addActionListener(event -> showFileChooserDialog());
		mainBox.add(fileButton);

		previewButton = new JButton("Preview files to be deleted");
		previewButton.setVisible(false);
		previewButton.addActionListener(event -> previewDeleteFiles());
		mainBox.add(previewButton);

		preview = new JTextArea();
		preview.setVisible(false);
		JScrollPane scroll = new JScrollPane(preview);
		mainBox.add(scroll);

		frame.getContentPane().add(mainBox);

		bottomRow = new Box(BoxLayout.X_AXIS);
		bottomRow.setVisible(false);
		bottomRow.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		deleteFoldersCheckBox = new JCheckBox();
		deleteFoldersCheckBox.setText("Delete empty folders");
		bottomRow.add(deleteFoldersCheckBox);

		bottomRow.add(Box.createHorizontalGlue());

		deleteButton = new JButton("DELETE FILES!!!!1uno");
		deleteButton.addActionListener(event -> deleteFiles());
		bottomRow.add(deleteButton);

		frame.getContentPane().add(bottomRow);
	}

	private void showFileChooserDialog() {
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		if (fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
			folder = fileChooser.getSelectedFile();
			filenameLabel.setText(folder.getAbsolutePath());
		}
		previewButton.setVisible(true);
	}

	private void previewDeleteFiles() {
		preview.setVisible(true);
		filesProcessed = 0;
		new Thread(() -> {
			filesToSave = new HashSet<File>();
			filesToDelete = new HashSet<File>();
			markFiles(folder);

			filesToDelete.removeAll(filesToSave);

			ArrayList<File> ordered = new ArrayList<>(filesToDelete.size());
			filesToDelete.forEach(file -> ordered.add(file));
			ordered.sort((f1, f2) -> f1.toString().compareTo(f2.toString()));

			StringBuilder text = new StringBuilder();
			ordered.forEach(file -> text.append(file.getAbsolutePath() + "\n"));
			preview.setText(text.toString());
		}).start();
	}

	private void markFiles(File folder) {
		preview.append(++filesProcessed + " - Processing folder: " + folder + "\n");
		for (File child : folder.listFiles(file -> file.isDirectory())) {
			markFiles(child);
		}

		for (File osuFile : folder.listFiles(file -> file.getName().endsWith(".osu"))) {
			try (BufferedReader br = new BufferedReader(new FileReader(osuFile))) {
				OsuParser parser = new OsuParser(br);
				if (parser.getMode() == 1) {
					filesToSave.addAll(parser.getReferencedFiles(folder));
					filesToSave.add(osuFile);
				} else {
					filesToDelete.addAll(parser.getReferencedFiles(folder));
					filesToDelete.add(osuFile);
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		bottomRow.setVisible(true);
	}

	private void deleteEmptyFolders(File folder, StringBuilder text) {
		if (!folder.isDirectory())
			return;
		for (File child : folder.listFiles(file -> file.isDirectory())) {
			deleteEmptyFolders(child, text);
		}
		if (folder.listFiles().length == 0) {
			if (folder.delete()) {
				text.append("DELETED EMPTY FOLDER: " + folder + "\n");
			} else {
				text.append("ERROR DELETING EMPTY FOLDER: " + folder + "\n");
			}
		}
	}

	private void deleteFiles() {
		StringBuilder text = new StringBuilder();
		filesToDelete.forEach(file -> {
			if (file.delete()) {
				text.append("DELETED: " + file + "\n");
			} else {
				text.append("ERROR DELETING: " + file + "\n");
			}
		});

		if (deleteFoldersCheckBox.isSelected()) {
			deleteEmptyFolders(folder, text);
		}

		text.append("Done.\n");
		preview.setText(text.toString());
	}
}
