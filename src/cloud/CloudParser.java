package cloud;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Scanner;

import javax.net.ssl.HttpsURLConnection;

import com.google.common.util.concurrent.RateLimiter;

/**
 * A class that utilize ltp-cloud REST API to get syntactic parsing output.
 * @author Haoran Sun
 * @since 03-05-2018
 */
public class CloudParser {
	static final String PARAM = "api_key=%s&text=%s&pattern=all&format=json";
	static final int MAX_LENGTH = 5000;

	private String key = null;
	private RateLimiter rl = null;

	/**
	 * Import api key from file.
	 * @param path - path to the file containing api key
	 */
	public void importKey(String path) {
		try(Scanner sc = new Scanner(new FileReader(path))) {
			this.key = sc.nextLine().trim();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		this.rl = RateLimiter.create(3.0); //180 times per minute
	}

	/**
	 * Post request to ltp cloud server and get json output.
	 * @param text - text to annotate
	 * @return a list of annotated output in json format
	 */
	public ArrayList<String> postRequest(String text) {
		if(this.key == null || text == null || text.length() == 0)
			return null;

		ArrayList<String> split = this.preprocess(text);
		ArrayList<String> parsed = new ArrayList<>();
		for(String splitText: split) {
			try {
				URL base = new URL("https://api.ltp-cloud.com/analysis/");
				this.rl.acquire();
				HttpsURLConnection con = (HttpsURLConnection)base.openConnection();
				con.setRequestMethod("POST");
				con.setDoOutput(true); 
				con.setDoInput(true);

				DataOutputStream output = new DataOutputStream(con.getOutputStream());
				String query = String.format(PARAM, this.key, 
						URLEncoder.encode(splitText, "utf-8"));
				output.write(query.getBytes(StandardCharsets.UTF_8));
				BufferedReader reader = new  BufferedReader(new InputStreamReader
						(con.getInputStream(), StandardCharsets.UTF_8)); 
				StringBuilder sb = new StringBuilder();
				String line = null;
				while((line = reader.readLine()) != null)
					sb.append(line);
				reader.close();
				parsed.add(sb.toString());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return parsed;
	}
	
	private ArrayList<String> preprocess(String text) {
		ArrayList<String> split = new ArrayList<>();
		if(text.length() <= MAX_LENGTH) {
			split.add(text);
			return split;
		}
		
		String[] sentences = text.split("(?<=��)|(?<=��)|(?<=��)");
		StringBuilder buf = new StringBuilder();
		int count = 0;
		for(int i = 0; i < sentences.length; i++) {
			if(count + sentences[i].length() <= MAX_LENGTH) { //Check length
				count += sentences.length;
				buf.append(sentences[i]);
			} else {
				/* Add split sentences */
				split.add(buf.toString());
				count = sentences[i].length();
				buf = new StringBuilder(); //Clear buffer
				buf.append(sentences[i]);
			}
		}
		return split;
	}

	public static void main(String[] args) {
		CloudParser ltpCloud = new CloudParser();
		ltpCloud.importKey("api.key");
		ArrayList<String> res = ltpCloud.postRequest("巴希尔强调，政府坚决主张通过和平和政治途径结束目前的武装冲突，在全国实现和平。"
				+ "他强烈呼吁以约翰·加朗为首的反政府武装力量回到国家的怀抱。在谈到周边关系时，巴希尔说，苏丹政府将采取行动改善与周边国家的关系。");
		System.out.println(res);
	}
}