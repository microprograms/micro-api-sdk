package com.github.microprograms.micro_api_sdk.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

public class Fn {

	public static List<Field> minusFieldByName(List<Field> fields, List<Field> fieldsToMinus) {
		List<Field> list = new ArrayList<>();
		for (Field x : fields) {
			if (!containsFieldByName(fieldsToMinus, x)) {
				list.add(x);
			}
		}
		return list;
	}

	public static boolean containsFieldByName(List<Field> fields, Field field) {
		for (Field x : fields) {
			if (x.getName().equals(field.getName())) {
				return true;
			}
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	public static <T> List<T> deepCopy(List<T> src) throws IOException, ClassNotFoundException {
		ByteArrayOutputStream byteOut = null;
		ObjectOutputStream objOut = null;
		ByteArrayInputStream byteIn = null;
		ObjectInputStream in = null;
		try {
			byteOut = new ByteArrayOutputStream();
			objOut = new ObjectOutputStream(byteOut);
			objOut.writeObject(src);
			byteIn = new ByteArrayInputStream(byteOut.toByteArray());
			in = new ObjectInputStream(byteIn);
			return (List<T>) in.readObject();
		} catch (IOException e) {
			throw e;
		} finally {
			IOUtils.closeQuietly(byteOut);
			IOUtils.closeQuietly(objOut);
			IOUtils.closeQuietly(byteIn);
			IOUtils.closeQuietly(in);
		}
	}

	public static String readFile(String file, Charset encoding) throws IOException {
		FileInputStream input = null;
		try {
			input = new FileInputStream(file);
			List<String> lines = IOUtils.readLines(input, encoding);
			return StringUtils.join(lines, "");
		} catch (IOException e) {
			throw e;
		} finally {
			IOUtils.closeQuietly(input);
		}
	}
}
