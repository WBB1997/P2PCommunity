package Bean;

/**
 * Created by UltimateZero on 9/11/2016.
 */
public class Emoji {
	private String shortname;
	private String unicode;
	private int emojiOrder;

	public Emoji(String shortname, String unicode) {
		this.shortname = shortname;
		this.unicode = unicode;
	}

	public String getShortname() {
		return shortname;
	}

	public void setShortname(String shortname) {
		this.shortname = shortname;
	}

	public String getUnicode() {
		return unicode;
	}

	public void setUnicode(String unicode) {
		this.unicode = unicode;
	}

	/**
	 * This is the filename (without extension) of the image
	 * @return Hex representation of the unicode
	 */

	public int getEmojiOrder() {
		return emojiOrder;
	}

	public void setEmojiOrder(int emojiOrder) {
		this.emojiOrder = emojiOrder;
	}

	@Override
	public String toString() {
		return "Emoji: [shortname: " +  shortname + ", unicode: " + unicode + "]";
	}


}
