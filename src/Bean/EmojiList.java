package Bean;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by UltimateZero on 9/11/2016.
 */
public class EmojiList {
	// 表情包实体类
	private static final HashMap<String, EmojiEntry> emojiList = new HashMap<>();

	// 表情包名字匹配
	private static Pattern SHORTNAME_PATTERN;

	private static final EmojiList INSTANCE = new EmojiList();

	public static EmojiList getInstance() {
		return INSTANCE;
	}


	private static final String EMOJIONE_JSON_FILE = "res/emoji.json";
	private static final String EMOJIONE_KEY_NAME = "name";
	private static final String EMOJIONE_KEY_SHORTNAME = "shortname";
	private static final String EMOJIONE_KEY_UNICODE_ALT = "unicode_alt";
	private static final String EMOJIONE_KEY_UNICODE = "unicode";
	private static final String EMOJIONE_KEY_ALIASES = "aliases";
	private static final String EMOJIONE_KEY_ALIASES_ASCII = "aliases_ascii";
	private static final String EMOJIONE_KEY_KEYWORDS = "keywords";
	private static final String EMOJIONE_KEY_CATEGORY = "category";
	private static final String EMOJIONE_KEY_EMOJI_ORDER = "emoji_order";

	private static final String EMOJIONE_MODIFIER = "modifier";


	private EmojiList() {
		StringBuilder jsonString = new StringBuilder();
		File file = new File(EMOJIONE_JSON_FILE);
		try {
			if (file.exists()) {
				String line;
				BufferedReader reader;
				reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
				while ((line = reader.readLine()) != null)
					jsonString.append(line);
				reader.close();
			} else
				return;
		} catch (IOException e) {
			e.printStackTrace();
		}
		JSONObject jsonObject = JSON.parseObject(jsonString.toString());
		for (Map.Entry<String, Object> entry : jsonObject.entrySet()) {
			JSONObject value = (JSONObject) entry.getValue();

			//获取每个表情包的属性
			String name = value.getString(EMOJIONE_KEY_NAME);
			String shortname = value.getString(EMOJIONE_KEY_SHORTNAME);
			String unicode = value.getString(EMOJIONE_KEY_UNICODE);
			List<String> aliases = value.getJSONArray(EMOJIONE_KEY_ALIASES).toJavaList(String.class);
			List<String> aliases_ascii = value.getJSONArray(EMOJIONE_KEY_ALIASES_ASCII).toJavaList(String.class);
			List<String> keywords = value.getJSONArray(EMOJIONE_KEY_KEYWORDS).toJavaList(String.class);
			String category = value.getString(EMOJIONE_KEY_CATEGORY);
			int emojiOrder = value.getIntValue(EMOJIONE_KEY_EMOJI_ORDER);
			// 存入表情包实体
			EmojiEntry emojiEntry = new EmojiEntry();
			emojiEntry.setName(name);
			emojiEntry.setShortname(shortname);
			emojiEntry.setUnicode(unicode);
			emojiEntry.setAliases(aliases);
			emojiEntry.setKeywords(keywords);
			emojiEntry.setCategory(category);
			emojiEntry.setEmojiOrder(emojiOrder);
			// 加入表情包列表
			EmojiList.emojiList.put(shortname, emojiEntry);
		}
		// 匹配列表
		SHORTNAME_PATTERN = Pattern.compile(String.join("|", emojiList.keySet()));
	}

	// 匹配字符串中的表情符号
	public Queue<Object> toEmojiAndText(String str) {
		Queue<Object> queue = new LinkedList<>();
		Matcher matcher = SHORTNAME_PATTERN.matcher(str);
		int lastEnd = 0;
		while (matcher.find()) {
			String lastText = str.substring(lastEnd, matcher.start());
			if (!lastText.isEmpty())
				queue.add(lastText);
			String m = matcher.group();
			String shortname = emojiList.get(m).getShortname();
			if (shortname == null || shortname.isEmpty())
				queue.add(m);
			else
				queue.add(new Emoji(shortname, emojiList.get(m).getUnicode()));
			lastEnd = matcher.end();
		}
		String lastText = str.substring(lastEnd);
		if (!lastText.isEmpty())
			queue.add(lastText);
		return queue;
	}

	public List<String> getCategories() {
		return emojiList.values().stream().map(EmojiEntry::getCategory).distinct().collect(Collectors.toList());
	}

	// 获取不同肤色的表情包的键值对
	public Map<String, List<Emoji>> getCategorizedEmojis(int tone) {
		Map<String, List<Emoji>> map = new HashMap<>();
		getTonedEmojis(tone).forEach(emojiEntry -> {
			if (emojiEntry.getCategory().equals(EMOJIONE_MODIFIER)) return;
			for (int i = 1; i <= 6; i++) {
				if (i == tone) continue;
				if (emojiEntry.getShortname().endsWith("_tone" + i + ":"))
					return;
			}
			List<Emoji> list = map.computeIfAbsent(emojiEntry.getCategory(), k -> new ArrayList<>());
			Emoji emoji = new Emoji(emojiEntry.getShortname(), emojiEntry.getUnicode());
			emoji.setEmojiOrder(emojiEntry.getEmojiOrder());
			list.add(emoji);
		});

		map.values().forEach(list -> list.sort(Comparator.comparing(Emoji::getEmojiOrder)));

		return map;
	}

	// 获得不同肤色的表情包
	public List<EmojiEntry> getTonedEmojis(int tone) {
		List<EmojiEntry> allToned = new ArrayList<>();
		List<EmojiEntry> selectedTone = new ArrayList<>();
		List<EmojiEntry> defaultTone = new ArrayList<>();
		emojiList.values().forEach(emojiEntry -> {
			for(int i = 1; i <= 5; i++) {
				if(emojiEntry.getShortname().endsWith("_tone" +i+":")) {
					allToned.add(emojiEntry);
					if(emojiEntry.getShortname().endsWith(tone + ":")) {
						selectedTone.add(emojiEntry);
					}
					String withoutTone = emojiEntry.getShortname().substring(0,emojiEntry.getShortname().length()-7) + ":";
					EmojiEntry emojiEntryWithoutTone = emojiList.get(withoutTone);
					if(!defaultTone.contains(emojiEntryWithoutTone)) {
						defaultTone.add(emojiEntryWithoutTone);
					}
				}
			}
		});
		List<EmojiEntry> allEmojis = new ArrayList<>(emojiList.values());
		allEmojis.removeAll(allToned);
		allEmojis.removeAll(defaultTone);
		if(tone == 6) { //default
			allEmojis.addAll(defaultTone);
		} else {
			allEmojis.addAll(selectedTone);
		}
		return allEmojis;

	}

	// 匹配字符串中的表情符号
	private String replaceWithFunction(String input, Pattern pattern, Function<String, String> func) {
		StringBuilder builder = new StringBuilder();
		Matcher matcher = pattern.matcher(input);
		int lastEnd = 0;
		while (matcher.find()) {
			String lastText = input.substring(lastEnd, matcher.start());
			builder.append(lastText);
			builder.append(func.apply(matcher.group()));
			lastEnd = matcher.end();
		}
		builder.append(input.substring(lastEnd));
		return builder.toString();
	}

	private String convert(String unicodeStr) {
		if (unicodeStr.isEmpty()) return unicodeStr;
		String[] parts = unicodeStr.split("-");
		StringBuilder buff = new StringBuilder();
		for (String s : parts) {
			int part = Integer.parseInt(s, 16);
			if (part >= 0x10000 && part <= 0x10FFFF) {
				int hi = (int) (Math.floor((part - 0x10000) / 0x400) + 0xD800);
				int lo = ((part - 0x10000) % 0x400) + 0xDC00;
				buff.append(new String(Character.toChars(hi)) + new String(Character.toChars(lo)));
			} else {
				buff.append(new String(Character.toChars(part)));
			}
		}
		return buff.toString();
	}

	public List<Emoji> search(String text) {
		return emojiList.values().stream().filter(emojiEntry -> (emojiEntry.getShortname().contains(text)
		|| emojiEntry.getAliases().contains(text))
		|| emojiEntry.getName().contains(text)).map(emojiEntry ->
			new Emoji(emojiEntry.getShortname(), emojiEntry.getUnicode())).collect(Collectors.toList());
	}

	public Emoji getEmoji(String shortname) {
		EmojiEntry entry = emojiList.get(shortname);
		if(entry == null) return null;
		return new Emoji(entry.getShortname(), entry.getUnicode());
	}

	class EmojiEntry {
		private String name;
		private String shortname;
		private String unicode;
		private List<String> aliases;
		private List<String> keywords;
		private String category;
		private int emojiOrder;

		public EmojiEntry() {}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
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

		public List<String> getAliases() {
			return aliases;
		}

		public void setAliases(List<String> aliases) {
			this.aliases = aliases;
		}

		public List<String> getKeywords() {
			return keywords;
		}

		public void setKeywords(List<String> keywords) {
			this.keywords = keywords;
		}

		public String getCategory() {
			return category;
		}

		public void setCategory(String category) {
			this.category = category;
		}

		public int getEmojiOrder() {
			return emojiOrder;
		}

		public void setEmojiOrder(int emojiOrder) {
			this.emojiOrder = emojiOrder;
		}
	}

}
