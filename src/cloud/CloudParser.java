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
	static final String URL = "api_key=%s&text=%s&pattern=all&format=json";

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
			con.setDoOutput(true); 
			con.setDoInput(true);

			DataOutputStream output = new DataOutputStream(con.getOutputStream());
			String query = String.format(URL, this.key, text);
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
		String res = ltpCloud.postRequest("我们是中国人。");
		System.out.println(res);
	}
}