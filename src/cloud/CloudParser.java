package cloud;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import javax.net.ssl.HttpsURLConnection;

/**
 * A class that utilize ltp-cloud REST API to get syntactic parsing output.
 * @author Haoran Sun
 * @since 03-05-2018
 */
public class CloudParser {
	static final String PARAM = "api_key=%s&text=%s&pattern=all&format=json";

	private String key = null;

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
	}

	/**
	 * Post request to ltp cloud server and get json output.
	 * @param text - text to annotate
	 * @return annotated output in json format
	 */
	public String postRequest(String text) {
		if(this.key == null)
			return null;

		try {
			URL base = new URL("https://api.ltp-cloud.com/analysis/");
			HttpsURLConnection con = (HttpsURLConnection)base.openConnection();
			con.setRequestMethod("POST");
			con.setDoOutput(true); 
			con.setDoInput(true);

			DataOutputStream output = new DataOutputStream(con.getOutputStream());
			String query = String.format(PARAM, this.key, text);
			output.write(query.getBytes(StandardCharsets.UTF_8));
			BufferedReader reader = new  BufferedReader(new InputStreamReader
					(con.getInputStream(), StandardCharsets.UTF_8)); 
			StringBuilder sb = new StringBuilder();
			String line = null;
			while((line = reader.readLine()) != null)
				sb.append(line);
			reader.close();
			return sb.toString();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static void main(String[] args) {
		CloudParser ltpCloud = new CloudParser();
		ltpCloud.importKey("api.key");
		String res = ltpCloud.postRequest("巴希尔强调，政府坚决主张通过和平和政治途径结束目前的武装冲突，在全国实现和平。"
				+ "他强烈呼吁以约翰·加朗为首的反政府武装力量回到国家的怀抱。在谈到周边关系时，巴希尔说，苏丹政府将采取行动改善与周边国家的关系。");
		System.out.println(res);
	}
}