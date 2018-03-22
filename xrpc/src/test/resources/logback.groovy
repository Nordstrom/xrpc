import ch.qos.logback.classic.filter.ThresholdFilter

appender("CONSOLE", ConsoleAppender) {
  withJansi = true

  filter(ThresholdFilter) {
    level = DEBUG
  }
  encoder(PatternLayoutEncoder) {
    pattern = "%-4relative [%thread] %-5level %logger{30} - %msg%n"
    outputPatternAsHeader = false
  }
}

if (System.getenv("DEBUG") != null) {
  root(DEBUG, ["CONSOLE"])
} else {
  logger("com.nordstrom.xrpc.server.ResponseFactory", OFF)
  root(WARN, ["CONSOLE"])
}
