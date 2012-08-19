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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Java implementation of <a href="https://github.com/tobie/ua-parser">UA Parser</a>
 *
 * @author Steve Jiang (@sjiang) <gh at iamsteve com>
 * @author Adrian Muraru (@adimuraru)
 */
public class Parser {

  private static final int MIN_CACHE_SIZE = 1000;
  private static final int MAX_CACHE_SIZE = 150000;
  //precise set of detection rules but slower, based on regexes exclusively
  public static final String REGEX_YAML_PATH_FULL = "/ua_parser/regexes.yaml";
  //faster but less precise set of detection rules
  public static final String REGEX_YAML_PATH_MINIMAL = "/ua_parser/regexes.minimal.yaml";
  private UserAgentParser uaParser;
  private OSParser osParser;
  private DeviceParser deviceParser;
  Cache<String, Client> uaCache;
  private boolean disableCache = true;

  public Parser() throws IOException {
    this(Parser.class.getResourceAsStream(REGEX_YAML_PATH_MINIMAL), false);
  }

  public Parser(InputStream regexYaml) {
    this(regexYaml, false);
  }

  public Parser(InputStream regexYaml, boolean disableCache) {
    initialize(regexYaml, disableCache);
  }

  public Client parse(String agentString) {

    // lookup cache if enabled
    if (!disableCache) {
      Client client = uaCache.getIfPresent(agentString);
      if (client != null) {
        return client;
      }
    }
    UserAgent ua = parseUserAgent(agentString);
    OS os = parseOS(agentString);
    Device device = deviceParser.parse(agentString, (ua == null ? null : ua.family));
    Client client = new Client(ua, os, device);
    if (!disableCache) {
      uaCache.put(agentString, client);
    }
    return client;
  }

  public UserAgent parseUserAgent(String agentString) {
    return uaParser.parse(agentString);
  }

  public Device parseDevice(String agentString) {
    return deviceParser.parse(agentString);
  }

  public OS parseOS(String agentString) {
    return osParser.parse(agentString);
  }

  private void initialize(InputStream regexYaml, boolean disableCache) {
    Yaml yaml = new Yaml(new SafeConstructor());
    Map<String, List> regexConfig = (Map<String, List>) yaml.load(regexYaml);

    List<Map> uaParserConfigs = regexConfig.get("user_agent_parsers");
    if (uaParserConfigs == null) {
      throw new IllegalArgumentException("user_agent_parsers is missing from yaml");
    }
    uaParser = UserAgentParser.fromList(uaParserConfigs);

    List<Map> osParserConfigs = regexConfig.get("os_parsers");
    if (osParserConfigs == null) {
      throw new IllegalArgumentException("os_parsers is missing from yaml");
    }
    osParser = OSParser.fromList(osParserConfigs);

    List<Map> deviceParserConfigs = regexConfig.get("device_parsers");
    if (deviceParserConfigs == null) {
      throw new IllegalArgumentException("device_parsers is missing from yaml");
    }
    List<String> mobileUAFamiliesList = regexConfig.get("mobile_user_agent_families");
    List<String> mobileOSFamiliesList = regexConfig.get("mobile_os_families");
    Set<String> mobileUAFamilies =
        (mobileUAFamiliesList == null ? Collections.EMPTY_SET : new HashSet<String>(
            mobileUAFamiliesList));
    Set<String> mobileOSFamilies =
        (mobileOSFamiliesList == null ? Collections.EMPTY_SET : new HashSet<String>(
            mobileOSFamiliesList));

    deviceParser =
        DeviceParser.fromList(deviceParserConfigs, uaParser, mobileUAFamilies, mobileOSFamilies);

    this.disableCache = disableCache;
    if (!disableCache) {
      uaCache =
          CacheBuilder.newBuilder().maximumSize(MAX_CACHE_SIZE).initialCapacity(MIN_CACHE_SIZE)
              .concurrencyLevel(1).build();
    }
  }

  public Cache<String, Client> getUaCache() {
    return uaCache;
  }
}
