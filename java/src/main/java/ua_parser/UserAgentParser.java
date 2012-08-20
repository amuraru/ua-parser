/**
 * Copyright 2012 Twitter, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package ua_parser;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User Agent parser using ua-parser regexes
 *
 * @author Steve Jiang (@sjiang) <gh at iamsteve com>
 * @author Adrian Muraru (@adimuraru) <amuraru at adobe com>
 */
public class UserAgentParser {
  static final String SPIDER = "spider";

  private final List<UAMatcher> matchers;

  public UserAgentParser(List<UAMatcher> patterns) {
    this.matchers = patterns;
  }

  public static UserAgentParser fromList(List<Map> configList) {
    List<UAMatcher> configPatterns = new ArrayList<UAMatcher>();

    for (Map<String, String> configMap : configList) {
      configPatterns.add(UserAgentParser.patternFromMap(configMap));
    }
    return new UserAgentParser(configPatterns);
  }

  public UserAgent parse(String agentString) {
    UserAgent agent;
    for (UAMatcher p : matchers) {
      if ((agent = p.match(agentString)) != null) {
        return agent;
      }
    }
    return new UserAgent("Other", null, null, null);
  }

  protected static UAMatcher patternFromMap(Map<String, String> configMap) {
    String regex = configMap.get("regex");
    if (regex != null) {
      return new UAPatternMatcher(Pattern.compile(regex), configMap.get("family_replacement"),
          configMap.get("v1_replacement"), configMap.get("v2_replacement"));
    }

    String names = configMap.get("name");
    if (names != null) {
      return new UAStringMatcher(names, configMap.get("require"), configMap.get("exclude"),
          configMap.get("family_replacement"), configMap.get("version_sep"));
    }

    throw new IllegalArgumentException("User agent is missing regex or names" + configMap);


  }

  protected static interface UAMatcher {
    public UserAgent match(String agentString);
  }

  protected static class UAStringMatcher implements UAMatcher {
    /**
     * List of strings to look for, at least one needs to match the full UA string The strings in
     * this list are also used to find the browser version : i.e. name/VERSION
     */
    private List<String> names;
    /**
     * additional list of strings that needs to be checked in order to complete the match
     */
    private List<String> require;
    /**
     * optional exclude
     */
    private List<String> exclude;


    private final String familyReplacement;
    private String versionSeparator = "/";

    public UAStringMatcher(String name, String require, String exclude, String familyReplacement,
        String versionSeparator) {

      if (name != null) {
        names = Lists.newLinkedList(Splitter.on("|").trimResults().split(name.toLowerCase()));
      }

      if (require != null) {
        this.require = Lists.newLinkedList(Splitter.on(",").split(require.toLowerCase()));
      }
      if (exclude != null) {
        this.exclude = Lists.newLinkedList(Splitter.on(",").split(exclude.toLowerCase()));
      }
      this.familyReplacement = familyReplacement;
      if (versionSeparator != null)
        this.versionSeparator = versionSeparator;
    }

    @Override
    public UserAgent match(String agentString) {
      if (agentString == null || agentString.length() == 0) {
        return new UserAgent(SPIDER, null, null, null);
      }
      if (this.names == null){
        return null;
      }

      int pos, posStart, posEnd;
      String version;
      String[] versionComponents;

      agentString = agentString.toLowerCase();

      for (String name : names) {
        if (agentString.contains(name)) {
          // main keyword match, check additional names
          if (require != null) {
            for (String req : require) {
              if (!agentString.contains(req)) return null;
            }
          }
          if (exclude != null) {
            // else, excludes?
            for (String exc : exclude) {
              if (agentString.contains(exc)) {
                return null;
              }
            }
          }
          // matched! extract version if required
          if (familyReplacement.equalsIgnoreCase(SPIDER)){
            //special case for SPIDER, version is the bot name
            return new UserAgent(familyReplacement, name, null, null);
          }
          //else, not bot
          if (this.versionSeparator.length()>0){
            versionComponents = new String[3];
            StringBuilder sb = new StringBuilder(name);
            pos = agentString.indexOf(sb.append(this.versionSeparator).toString());
            if (pos >= 1) {
              posStart = pos + name.length() + 1;

              posEnd = CharMatcher.anyOf(" ;/,)").indexIn(agentString, posStart);
              if (posEnd != -1) {
                version = agentString.substring(posStart, posEnd);
              } else {
                version = agentString.substring(posStart);
              }
              if (version != null) {
                int i = 0;
                for (String component : Splitter.on(".").limit(3).trimResults().split(version)) {
                  versionComponents[i++] = component;
                }
              }
              return new UserAgent(familyReplacement, versionComponents[0], versionComponents[1],
                  versionComponents[2]);
            }
          }
          //version detection disabled
          return new UserAgent(familyReplacement, null, null, null);
        }
      }
      // no match
      return null;
    }
  }

  protected static class UAPatternMatcher implements UAMatcher {
    private Pattern pattern;

    private final String familyReplacement, v1Replacement, v2Replacement;

    public UAPatternMatcher(Pattern pattern, String familyReplacement, String v1Replacement,
        String v2Replacement) {
      this.pattern = pattern;
      this.familyReplacement = familyReplacement;
      this.v1Replacement = v1Replacement;
      this.v2Replacement = v2Replacement;
    }

    @Override
    public UserAgent match(String agentString) {

      String family = null, v1 = null, v2 = null, v3 = null;
      Matcher matcher = pattern.matcher(agentString);

      if (!matcher.find()) {
        return null;
      }

      int groupCount = matcher.groupCount();

      if (familyReplacement != null) {
        if (familyReplacement.contains("$1") && groupCount >= 1 && matcher.group(1) != null) {
          family =
              familyReplacement.replaceFirst("\\$1", Matcher.quoteReplacement(matcher.group(1)));
        } else {
          family = familyReplacement;
        }
      } else if (groupCount >= 1) {
        family = matcher.group(1);
      }

      if (v1Replacement != null) {
        v1 = v1Replacement;
      } else if (groupCount >= 2) {
        v1 = matcher.group(2);
      }

      if (v2Replacement != null) {
        v2 = v2Replacement;
      } else if (groupCount >= 3) {
        v2 = matcher.group(3);
        if (groupCount >= 4) {
          v3 = matcher.group(4);
        }
      }
      return family == null ? null : new UserAgent(family, v1, v2, v3);
    }
  }
}
