package secs.sml;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SmlMessageParser {

	protected SmlMessageParser() {
		/* Nothing */
	}
	
	private static class SingletonHolder {
		private static final SmlMessageParser inst = new SmlMessageParser();
	}
	
	public static SmlMessageParser getInstance() {
		return SingletonHolder.inst;
	}
	
	/**
	 * prototype
	 * 
	 * @return
	 */
	protected SmlSecs2Parser getSmlSecs2Parser() {
		return SmlSecs2Parser.getInstance();
	}
	
	protected static final String GROUP_STREAM = "STREAM";
	protected static final String GROUP_FUNCTION = "FUNCTION";
	protected static final String GROUP_WBIT = "WBIT";
	protected static final String GROUP_SECS2 = "SECS2";
	protected static final String pregMessage = "[Ss](?<" + GROUP_STREAM + ">[0-9]+)[Ff](?<" + GROUP_FUNCTION + ">[0-9]+)\\s*(?<" + GROUP_WBIT + ">[Ww]?)\\s*(?<" + GROUP_SECS2 + ">(<.+>)?)";
	
	protected static final Pattern ptnMessage = Pattern.compile("^" + pregMessage + "$");

	/**
	 * parse to SML-Message
	 * 
	 * @param SML=Format-Character
	 * @return SmlMessage
	 * @throws SmlParseException
	 */
	public SmlMessage parse(CharSequence cs) throws SmlParseException  {
		
		String s = trimPeriod(cs);
		
		Matcher m = ptnMessage.matcher(s);
		
		if ( ! m.matches() ) {
			throw new SmlParseException();
		}
		
		try {
			
			int strm = Integer.parseInt(m.group(GROUP_STREAM));
			int func = Integer.parseInt(m.group(GROUP_FUNCTION));
			boolean wbit = ! m.group(GROUP_WBIT).isEmpty();
			String secs2 = m.group(GROUP_SECS2);
			
			return new SmlMessage(strm, func, wbit
					, getSmlSecs2Parser().parse(secs2));
		}
		catch ( NumberFormatException e) {
			throw new SmlParseException("SxFy parse failed", e);
		}
	}
	
	private String trimPeriod(CharSequence cs) {
		String s = cs.toString().trim();
		if ( s.endsWith(".") ) {
			s = s.substring(0, s.length() - 1);
		}
		return s;
	}

}
