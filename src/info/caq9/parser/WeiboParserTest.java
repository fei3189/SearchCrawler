package info.caq9.parser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import org.htmlparser.util.ParserException;
import org.json.JSONException;

public class WeiboParserTest {

	public static void main(String[] args) throws IOException, ParserException,
			JSONException {
		BufferedReader br = new BufferedReader(new FileReader("exception2.txt"));
		String line;
		StringBuffer sb = new StringBuffer();
		while ((line = br.readLine()) != null) {
			sb.append(line + "\n");
		}
		List<WeiboMessage> messages = WeiboParser.parse(sb.toString(),
				System.currentTimeMillis());
		System.out.println(messages);
	}

}
