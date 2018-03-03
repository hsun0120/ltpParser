import java.util.ArrayList;
import java.util.List;

import edu.hit.ir.ltp4j.Segmentor;

public class LTPParser {
	public static void main(String[] args) {
		Segmentor segmentor = new Segmentor();
    if(segmentor.create("ltp_data_v3.4.0/cws.model")<0){
      System.err.println("load failed");
      return;
    }

    String sent = "我是中国人";
    List<String> words = new ArrayList<String>();
    int size = segmentor.segment(sent,words);

    for(int i = 0; i<size; i++) {
      System.out.print(words.get(i));
      if(i==size-1) {
        System.out.println();
      } else{
        System.out.print("\t");
      }
    }
    segmentor.release();
  }
}