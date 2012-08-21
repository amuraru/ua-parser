/**
 * Copyright 2012 Twitter, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ua_parser;

import java.util.Map;

/**
 * Operating System parsed data class
 *
 * @author Steve Jiang (@sjiang) <gh at iamsteve com>
 */
public class OS {
  private static final String UNKNOWN = "unknown";

  private final String family;

  private final String major;

  private final String minor;

  private final String patch;

  private final String patchMinor;

  public OS(String family, String major, String minor, String patch, String patchMinor) {
    this.family = family;
    this.major = major;
    this.minor = minor;
    this.patch = patch;
    this.patchMinor = patchMinor;
  }

  public static OS fromMap(Map<String, String> m) {
    return new OS(m.get("family"), m.get("major"), m.get("minor"), m.get("patch"), m.get("patch_minor"));
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) return true;
    if (!(other instanceof OS)) return false;

    OS o = (OS) other;
    return ((this.getFamily() != null && this.getFamily().equals(o.getFamily())) || this.getFamily() == o.getFamily()) &&
           ((this.major != null && this.major.equals(o.major)) || this.major == o.major) &&
           ((this.minor != null && this.minor.equals(o.minor)) || this.minor == o.minor) &&
           ((this.patch != null && this.patch.equals(o.patch)) || this.patch == o.patch) &&
           ((this.patchMinor != null && this.patchMinor.equals(o.patchMinor)) || this.patchMinor == o.patchMinor);
  }

  @Override
  public int hashCode() {
    int h = getFamily() == null ? 0 : getFamily().hashCode();
    h += major == null ? 0 : major.hashCode();
    h += minor == null ? 0 : minor.hashCode();
    h += patch == null ? 0 : patch.hashCode();
    h += patchMinor == null ? 0 : patchMinor.hashCode();
    return h;
  }

  @Override
  public String toString() {
    return String.format("{family: %s, major: %s, minor: %s, patch: %s, patch_minor: %s}",
                         getFamily() == null ? null : '"' + getFamily() + '"',
                         major == null ? null : '"' + major + '"',
                         minor == null ? null : '"' + minor + '"',
                         patch == null ? null : '"' + patch + '"',
                         patchMinor == null ? null : '"' + patchMinor + '"');
  }

  public String getFamily() {
    return family==null || family.length()==0 ? UNKNOWN : family;
  }

  public String getFullVersion() {
    return getVersion(true, true,true);
  }
  public String getShortVersion(){
    return getVersion(true, false, false);
  }

  public String getVersion(boolean includeMinor, boolean includePatch, boolean includePatchMinor) {
    StringBuilder sb = new StringBuilder();
    if (major != null && major.length()>0) {
      sb.append(major);
      if (includeMinor && minor != null && minor.length()>0) {
        sb.append(".").append(minor);
        if (includePatch && patch != null && patch.length()>0) {
          sb.append(".").append(patch);
          if (includePatchMinor && patchMinor != null && patchMinor.length()>0) {
            sb.append(".").append(patchMinor);
          }
        }
      }
    }
    return sb.length() > 0 ? sb.toString() : UNKNOWN;
  }
}