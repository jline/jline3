package jline;

import java.io.IOException;


public class JLineStringBuilder implements JLineBuffer
{
	StringBuilder buffer = new StringBuilder();

	public Appendable append(CharSequence s) {
		return buffer.append(s);
	}

	public Appendable append(char c) throws IOException {
		return buffer.append(c);
	}

	public Appendable append(CharSequence s, int start, int end) throws IOException {
		return buffer.append(s, start, end);
	}

	public char charAt(int index) {
		return buffer.charAt(index);
	}

	public int length() {
		return buffer.length();
	}

	public CharSequence subSequence(int start, int end) {
		return buffer.subSequence(start, end);
	}

	public void delete(int start, int end) {
		buffer.delete(start, end);
	}

	public void deleteCharAt(int index) {
		buffer.deleteCharAt(index);
	}

	public void insert(int offset, char c) {
		buffer.insert(offset, c);
	}

	public void insert(int offset, CharSequence s) {
		buffer.insert(offset, s);
	}

	public void setLength(int newLength) {
		buffer.setLength(newLength);
	}

	public String substring(int start) {
		return buffer.substring(start);
	}

	public String substring(int start, int end) {
		return buffer.substring(start, end);
	}

	public void replace(int start, int end, String str) {
		buffer.replace(start, end, str);
	}
	
	public String toString() {
		return buffer.toString();
	}
}
