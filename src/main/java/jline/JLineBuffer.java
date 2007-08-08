package jline;



public interface JLineBuffer extends Appendable, CharSequence
{

	public void delete(int start, int end);

	public void insert(int offset, char c);

	public void deleteCharAt(int index);

	public void insert(int offset, CharSequence s);
	
	// Don't throw Exceptions
	public Appendable append(CharSequence s);

	public void setLength(int newLength);

	public String substring(int start);

	public String substring(int start, int end);

	public void replace(int start, int end, String str);
}
