package bench;

import com.google.common.base.Functions;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Ordering;
import com.google.common.io.LineReader;

import org.junit.Test;

import ua_parser.Client;
import ua_parser.Parser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

public class BenchmarkParserTest {
  private static String TEST_FILE = "/ua_parser/test.uas";


  @Test
  public void testFullPatterns() throws NumberFormatException, IOException {
    this.testUA(Parser.REGEX_YAML_PATH_FULL);
  }

  @Test
  public void testMinialPatterns() throws NumberFormatException, IOException {
    this.testUA(Parser.REGEX_YAML_PATH_MINIMAL);
  }

  private void testUA(String patterns) throws NumberFormatException, IOException {

    Parser parser = new Parser(Parser.class.getResourceAsStream(patterns));
    InputStream testFile = this.getClass().getResourceAsStream(TEST_FILE);
    Reader reader = new InputStreamReader(testFile);
    LineReader lineReader = new LineReader(reader);

    Map<String, Integer> browser_results = new TreeMap<String, Integer>();
    Map<String, Integer> device_results = new TreeMap<String, Integer>();
    Map<String, Integer> browser_version_results = new TreeMap<String, Integer>();
    Map<String, Integer> ua_os_results = new TreeMap<String, Integer>();
    Map<String, Integer> ismobile_results = new TreeMap<String, Integer>();
    Map<String, Integer> ua_bot_results = new TreeMap<String, Integer>();


    long currentTimeMillis = System.currentTimeMillis();
    Integer count;
    Map<String, Integer> r;
    String key;
    int i = 0;
    long total = 0, t1;
    int incr = 1;
    System.out.println("start parsing");
    String line;
    while ((line = lineReader.readLine()) != null) {
      if (line.trim().length()<=0){
        continue;
      }
      String[] split = line.split("\t");
      incr = Integer.parseInt(split[0].trim());
      if (split.length < 2) {
        System.out.printf("%d\t%s\t%d\t%d\n", incr, "-", 0, incr);
        continue;
      }
      line = split[1].trim();
      boolean isNotBot = Integer.parseInt(split[2].trim()) > 0;
      boolean isBot = Integer.parseInt(split[3].trim()) > 0;


      if (++i % 10000 == 0) {
        System.out.println("current:" + i + " cache size:" + parser.getUaCache().size()
            + " total lookup time:" + total);
      }
      t1 = System.currentTimeMillis();
      Client client = parser.parse(line);

      // System.out.printf("%d\t%s\t%d\t%d\n", incr, line, !client.isSpider()?incr:0,
      // client.isSpider()?incr:0 );
      /*
       * if(client.isSpider() && isNotBot){ System.out.println(line);
       * System.out.println(client.userAgent.getShortVersion()); continue; }
       */
      total += (System.currentTimeMillis() - t1);
      if (!client.isSpider()) {
        r = browser_results;
        key = client.userAgent.getFamily();
        key += "-" + client.userAgent.getShortVersion();
        count = r.get(key);

        if (count == null) {
          r.put(key, incr);
        } else {
          r.put(key, count + incr);
        }

        r = browser_version_results;
        key = client.userAgent.getFullVersion();
        count = r.get(key);
        if (count == null) {
          r.put(key, incr);
        } else {
          r.put(key, count + incr);
        }

        r = ua_os_results;
        key = client.os.getFamily() + "-" + client.os.getShortVersion();
        count = r.get(key);
        if (count == null) {
          r.put(key, incr);
        } else {
          r.put(key, count + incr);
        }

        //if (client.device.isMobile) {
          r = device_results;
          key = client.device.getFamily();
          count = r.get(key);
          if (count == null) {
            r.put(key, incr);
          } else {
            r.put(key, count + incr);
          }
        //}

        r = ismobile_results;
        key = String.valueOf(client.device.isMobile);
        count = r.get(key);
        if (count == null) {
          r.put(key, incr);
        } else {
          r.put(key, count + incr);
        }
      }

      r = ua_bot_results;
      key = Boolean.toString(client.isSpider());
      count = r.get(key);
      if (count == null) {
        r.put(key, incr);
      } else {
        r.put(key, count + incr);
      }

    }
    System.out.println("===Totaal time:" + (System.currentTimeMillis() - currentTimeMillis));
    System.out.println("===Lookup time:" + total);
    print_map(browser_results);
    System.out.println("===versions===");
    print_map(browser_version_results);

    System.out.println("===devices===");
    print_map(device_results);
    System.out.println("===ismobile===");
    print_map(ismobile_results);

    System.out.println("===os===");
    print_map(ua_os_results);
    System.out.println("===bot===");
    print_map(ua_bot_results);

  }

  private static void print_map(Map<String, Integer> results) {
    Ordering<String> valueComparator =
        valueComparator =
            Ordering.natural().reverse().onResultOf(Functions.forMap(results))
                .compound(Ordering.natural());
    results = ImmutableSortedMap.copyOf(results, valueComparator);
    for (Entry<String, Integer> kv : results.entrySet()) {
      System.out.println(kv.getKey() + "=>" + kv.getValue());
    }
  }

}
