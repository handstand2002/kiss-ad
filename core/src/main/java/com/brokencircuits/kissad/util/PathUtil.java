package com.brokencircuits.kissad.util;

import java.util.regex.Pattern;

public class PathUtil {
  final private static Pattern TRAILING_SLASH_PATTERN = Pattern.compile("[/\\\\]$");

  public static String addTrailingSlashToPath(String path) {
    if (path == null) {
      return "/";
    }
    if (!TRAILING_SLASH_PATTERN.matcher(path).find()) {
      return path + "/";
    }
    return path;
  }
}
