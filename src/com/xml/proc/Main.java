package com.xml.proc;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
	private static Charset JAVA_ENCODING = StandardCharsets.UTF_8;
	
	private static String LINE_DELIMITER = "\n";
	private static String TAG_NAME_START = "name=\"";
	private static String TAG_NAME_END = "\"";
	private static String TAG_FORMATED_FALSE = "formatted=\"false\"";
	private static String TAG_PLURAL_ELEMENT = "<item quantity=\"";

	// just looking only in strings.xml files
	private static String EXTERNAL_XML = "\\strings.xml";

	// exclude Java keywords
	private static String JAVA_KEYWORDS = " false synchronized int abstract float private char boolean static null if const "
			+ "for true while long strictfp finally protected import native final void "
			+ "enum else break transient catch instanceof byte super volatile case assert short "
			+ "package default double public try this switch continue throws protected public private ";

	// phrase app often using %{name} for formatted text, but android using %s or %d
	private static String PHRASE_FLOAT = "  ";
	private static String PHRASE_DECIMAL = " duration from to countdown count amount max_stay min_stay number nights "
			+ "extra_n_guests contacts_ignored_count ignored_emails_count ";
	private static String PHRASE_STRING = " location checkin checkout user link count name travel period resource kind "
			+ "reason user_name host_name international_number us_number rejected_at start_at terms_and_conditions username "
			+ "price_extra room_kind location country price description room_title search_terms user_first_name current_credit "
			+ "guest_price coupon_amount host_price godchild_coupon_amount to title total_credit ";

	// replace it with own replace symbol
	private static String NAME_REPLACER = "_";

	// Warning ! Hardcoded XML processing #)

	public static void main(String[] args) {
		if (args.length == 1) {
			File file = new File(args[0]);
			if (file.isDirectory()) {
				List<String> files = new ArrayList<String>();
				files = getFileNames(files, Paths.get(file.getAbsolutePath()), EXTERNAL_XML);
				for (String name : files) {
					processFile(name, name);
				}
			} else {
				processFile(args[0], args[0]);
			}
		} else if (args.length == 2) {
			processFile(args[0], args[1]);
		} else {
			System.err.println("Error arguments - enter folder name with \\ or xml file or two xml files");
		}

	}

	private static void processFile(String fileIn, String fileOut) {
		System.out.println(String.format("File %s processing to %s", fileIn, fileOut));
		String loadData = null;
		try {
			loadData = loadData(fileIn);
		} catch (IOException e) {
			System.err.println(String.format("File %s not found", fileIn));
			return;
		}
		String result = processData(loadData);
		try {
			saveData(fileOut, result);
		} catch (IOException e) {
			System.err.println(String.format("File %s can't be writing", loadData));
			return;
		}
	}

	private static String loadData(String filename) throws IOException {

		int length;
		char[] buffer = new char[4096];
		final StringBuffer string = new StringBuffer();

		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename), JAVA_ENCODING));

		try {
			while ((length = reader.read(buffer)) > 0) {
				string.append(buffer, 0, length);
			}
		} finally {
			reader.close();
		}

		return string.toString();
	}

	private static void saveData(String filename, String data) throws IOException {
		Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename), JAVA_ENCODING));
		try {
			out.write(data);
		} finally {
			out.close();
		}
		return;
	}

	private static String processData(String data) {
		StringBuffer buffer = new StringBuffer();

		Scanner scanner = new Scanner(data);
		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			line = processLineNames(line);
			line = processLineFormat(line);
			line = processLineMultipleFormat(line);
			line = processLineMultiplePlurals(line);
			buffer.append(line);
			buffer.append(LINE_DELIMITER);
		}
		scanner.close();

		return buffer.toString();
	}

	private static String processLineNames(String data) {
		StringBuffer buffer = new StringBuffer();
		try {
			int startString = data.indexOf(TAG_NAME_START);
			int endString = data.indexOf(TAG_NAME_END, startString + TAG_NAME_START.length());

			if (startString >= 0 && endString >= 0) {
				startString = startString + TAG_NAME_START.length();
				endString = endString + TAG_NAME_END.length() - 1;
				String result = data.substring(startString, endString);

				buffer.append(data.substring(0, startString));
				// Java keywords
				if (JAVA_KEYWORDS.contains(" ".concat(result.trim()).concat(" "))) {
					result = NAME_REPLACER.concat(result);
				}
				// Spaces
				result = result.replace(" ", NAME_REPLACER);
				// Minus sign
				result = result.replace("-", NAME_REPLACER);
				// Dots
				result = result.replace(".", NAME_REPLACER);
				//
				buffer.append(result);
				buffer.append(data.substring(endString));
				return buffer.toString();
			} else {
				return data;
			}
		} catch (Exception e) {
			System.err.println("Line processing error processLineNames");
		}
		return data;
	}

	private static String processLineFormat(String data) {
		String buffer = data;
		try {
			Pattern pattern = Pattern.compile("\\{(.*?)\\}");
			Matcher matcher = pattern.matcher(data);
			while (matcher.find()) {
				String token = matcher.group(0);
				String search = " ".concat(token.substring(1, token.length() - 1)).concat(" ");

				if (PHRASE_FLOAT.contains(search)) {
					buffer = buffer.replace(token, "f");
				}

				if (PHRASE_DECIMAL.contains(search)) {
					buffer = buffer.replace(token, "d");
				}

				if (PHRASE_STRING.contains(search)) {
					buffer = buffer.replace(token, "s");
				}
			}
		} catch (Exception e) {
			System.err.println("Line processing error processLineFormat");
		}
		return buffer;
	}

	private static String processLineMultipleFormat(String data) {
		String buffer = data;
		try {
			if (!data.contains(TAG_FORMATED_FALSE)) {
				if (getCharCount(buffer, "%") >= 2) {
					buffer = buffer.replace("<string ", "<string formatted=\"false\" ");
				}
			}
		} catch (Exception e) {
			System.err.println("Line processing error processLineMultipleFormat");
		}
		return buffer;
	}

	private static String processLineMultiplePlurals(String data) {
		StringBuffer buffer = new StringBuffer();
		try {
			if (data.contains(TAG_PLURAL_ELEMENT)) {
				if (getCharCount(data, "%") >= 2) {
					String[] split = data.split("%");

					int lastDigit = 1;
					buffer.append(split[0]);
					for (int i = 1; i < split.length; i++) {	
						try{
							int digit = isDigit(split[i].substring(0, 1));	
							if(digit>0){
								lastDigit = digit;
								buffer.append("%");
								buffer.append(split[i]);
							}else{
								buffer.append("%");
								buffer.append(Integer.toString(lastDigit));
								buffer.append("$");
								buffer.append(split[i]);
								lastDigit++;
							}
						}catch(Exception e){
							buffer.append("%");
							buffer.append(split[i]);
						}
					}
				}else{
					return data;
				}
			}else{
				return data;
			}
		} catch (Exception e) {
			System.err.println("Line processing error processLineMultipleFormat");
		}
		return buffer.toString();
	}

	private static List<String> getFileNames(List<String> fileNames, Path dir, String filter) {
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
			for (Path path : stream) {
				if (path.toFile().isDirectory()) {
					getFileNames(fileNames, path, filter);
				} else {
					String fullPath = path.toAbsolutePath().toString();
					if (fullPath.endsWith(filter)) {
						fileNames.add(fullPath);
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return fileNames;
	}

	private static int getCharCount(String data, String symbol) {
		return data.length() - data.replace(symbol, "").length();
	}

	// -1 is not digit
	private static int isDigit(String data) {
		try {
			int digit = Integer.parseInt(data);
			return digit;
		} catch (Exception e) {
			return -1;
		}
	}

}
