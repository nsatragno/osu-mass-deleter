package ar.com.satragno.osufileeditor;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;

public class OsuParser {

	private class Token {
		final String title;
		final String value;

		Token(String line) {
			int colon = line.indexOf(':');
			if (colon == -1) {
				throw new RuntimeException("Line " + line + " has no colon.");
			}
			title = line.substring(0, colon).trim();
			value = line.substring(colon + 1).trim();
		}
	}

	private int mode;
	private final HashSet<String> files;

	private final String EXTRA_FILE_PATTERN = ".*[^:|]*:[^:|]*:[^:|]*:[^:|]*:[^:]*\\.[^:]*$";

	public OsuParser(BufferedReader br) throws IOException {
		files = new HashSet<>();
		String currentSection = "";
		String currentLine;
		while ((currentLine = br.readLine()) != null) {
			currentLine = currentLine.trim();
			
			if (currentLine.isEmpty() || currentLine.startsWith("//"))
				continue;
			
			if (currentLine.startsWith("osu file format")) {
				int format = Integer.parseInt(currentLine.substring(currentLine.indexOf('v') + 1));
				System.out.println("Format: " + format);
				if (format < 12) {
					System.err.println("WARNING: Format old. This might break");
				}
				continue;
			}
			
			if (currentLine.startsWith("[") && currentLine.endsWith("]")) {
				currentSection = currentLine;
				continue;
			}
			
			switch (currentSection) {
				case "[General]":
					Token token = new Token(currentLine);
					if (token.title.equals("Mode")) {
						mode = Integer.parseInt(token.value);
						System.out.println("Parsing Mode type " + mode);
						continue;
					}
					if (token.title.equals("AudioFilename")) {
						addFile(token.value);
						System.out.println("Parsing Audio Filename " + token.value);
						continue;
					}
					break;
				case "[Events]":
					if (currentLine.startsWith("0,")) {
						String filename = currentLine.split(",")[2];
						System.out.println("Parsing event with file " + filename);
						addFile(filename);
						continue;
					}
					if (currentLine.startsWith("4,") || 
						currentLine.startsWith("5,") ||
						currentLine.startsWith("6,") || 
						currentLine.startsWith("Sprite,") ||
						currentLine.startsWith("Animation,")) {
						String filename = currentLine.split(",")[3];
						System.out.println("Parsing storyboard with file " + filename);
						addFile(filename);
						continue;
					}
					break;
				case "[HitObjects]":
					if (currentLine.matches(EXTRA_FILE_PATTERN)) {
						String[] parts = currentLine.split(":");
						String filename = parts[parts.length - 1];
						System.out.println("Parsing hit object with name " + filename);
						addFile(filename);
						continue;
					}
					break;
			}
			if (currentLine.contains(".wav") ||
				currentLine.contains(".mp3") ||
				currentLine.contains(".ogg") ||
				currentLine.contains(".png") ||
				currentLine.contains(".bmp") ||
				currentLine.contains(".flag")) {
				System.err.println("WARNING: Possibly did not find this file!");
				System.err.println(currentLine);
			}
		}
	}

	public int getMode() {
		return mode;
	}

	public HashSet<File> getReferencedFiles(File baseFolder) {
		HashSet<File> references = new HashSet<File>();
		for (String filename : files) {
			references.add(new File(baseFolder, filename));
		}
		return references;
	}

	private void addFile(String filename) {
		if (filename.startsWith("\"") && filename.endsWith("\"")) {
			filename = filename.substring(1, filename.length() - 1);
		}
		files.add(filename);
	}
}
