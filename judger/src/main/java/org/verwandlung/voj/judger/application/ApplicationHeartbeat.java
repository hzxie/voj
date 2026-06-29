/* Verwandlung Online Judge - A cross-platform judge online system
 * Copyright (C) 2014-2026 Haozhe Xie <root@haozhexie.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.verwandlung.voj.judger.application;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import org.verwandlung.voj.judger.mapper.UserMapper;
import org.verwandlung.voj.judger.messenger.MessageSender;
import org.verwandlung.voj.judger.model.User;

/**
 * The application heartbeat, used to send Keep-Alive messages to the web module.
 *
 * @author Haozhe Xie
 */
@Component
public class ApplicationHeartbeat implements Runnable {
  /* (non-Javadoc)
   * @see java.lang.Runnable#run()
   */
  public void run() {
    if (!isIdentityValid()) {
      LOGGER.error("Unauthorized: Please check your username and password.");
      System.exit(-1);
    }
    Calendar calendar = Calendar.getInstance();
    long currentTime = calendar.getTimeInMillis();

    Map<String, Object> mapMessage = new HashMap<>();
    mapMessage.put("event", "KeepAlive");
    mapMessage.put("username", judgerUsername);
    mapMessage.put("description", getDescription());
    mapMessage.put("heartbeatTime", currentTime);
    // Live telemetry sampled from the JVM/host so the web dashboard can show node load.
    mapMessage.put("cpuLoad", getCpuLoad());
    mapMessage.put("memoryUsage", getMemoryUsage());
    mapMessage.put("uptime", ManagementFactory.getRuntimeMXBean().getUptime());
    messageSender.sendMessage(mapMessage);
    LOGGER.info("Heartbeat sent to the web server.");
  }

  /**
   * Gets the current system CPU load as a percentage (0-100).
   *
   * @return the system CPU load percentage, or 0 when the platform cannot report it
   */
  private int getCpuLoad() {
    com.sun.management.OperatingSystemMXBean osBean =
        (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
    // The first reading after JVM start reflects class-loading/JIT warm-up rather than the
    // steady-state load, which makes a freshly started judger report ~100%. Prime the bean and
    // sample again over a short window so the reported value is an accurate recent load.
    osBean.getCpuLoad();
    try {
      Thread.sleep(CPU_SAMPLE_INTERVAL);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
    }
    double cpuLoad = osBean.getCpuLoad();
    return cpuLoad < 0 ? 0 : (int) Math.round(cpuLoad * 100);
  }

  /**
   * Gets the current physical memory usage as a percentage (0-100).
   *
   * <p>On Linux the usage is derived from {@code MemAvailable} in {@code /proc/meminfo}, which
   * accounts for reclaimable page cache and slab. Using the JVM bean's free memory instead would
   * count {@code buff/cache} as "used" and report a misleadingly high figure (close to 100%) on a
   * perfectly healthy host. The host-wide {@code /proc/meminfo} is also visible from inside a
   * container, so the reported value matches what {@code free} shows on the machine.
   *
   * @return the memory usage percentage, or 0 when the platform cannot report it
   */
  private int getMemoryUsage() {
    int usageFromMemInfo = getMemoryUsageFromProcMemInfo();
    if (usageFromMemInfo >= 0) {
      return usageFromMemInfo;
    }
    // Fall back to the JVM bean on platforms without /proc/meminfo (e.g. Windows, macOS). This
    // overstates usage on systems with a large page cache, but those platforms have no equivalent
    // of MemAvailable exposed here.
    com.sun.management.OperatingSystemMXBean osBean =
        (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
    long totalMemory = osBean.getTotalMemorySize();
    long freeMemory = osBean.getFreeMemorySize();
    if (totalMemory <= 0) {
      return 0;
    }
    return (int) Math.round((totalMemory - freeMemory) * 100.0 / totalMemory);
  }

  /**
   * Computes the memory usage percentage from {@code MemTotal} and {@code MemAvailable} in {@code
   * /proc/meminfo}.
   *
   * @return the memory usage percentage (0-100), or -1 when {@code /proc/meminfo} is unavailable or
   *     does not provide the required fields
   */
  private int getMemoryUsageFromProcMemInfo() {
    Path memInfoPath = Paths.get("/proc/meminfo");
    if (!Files.isReadable(memInfoPath)) {
      return -1;
    }
    long memTotal = -1;
    long memAvailable = -1;
    try {
      for (String line : Files.readAllLines(memInfoPath)) {
        if (line.startsWith("MemTotal:")) {
          memTotal = parseMemInfoKilobytes(line);
        } else if (line.startsWith("MemAvailable:")) {
          memAvailable = parseMemInfoKilobytes(line);
        }
        if (memTotal > 0 && memAvailable >= 0) {
          break;
        }
      }
    } catch (IOException ex) {
      LOGGER.warn("Failed to read /proc/meminfo; falling back to JVM memory reporting.", ex);
      return -1;
    }
    if (memTotal <= 0 || memAvailable < 0) {
      return -1;
    }
    long used = memTotal - memAvailable;
    return (int) Math.round(used * 100.0 / memTotal);
  }

  /**
   * Parses the kilobyte value from a {@code /proc/meminfo} line such as {@code "MemTotal:  2031024
   * kB"}.
   *
   * @param line the line to parse
   * @return the value in kilobytes, or -1 when it cannot be parsed
   */
  private long parseMemInfoKilobytes(String line) {
    String[] tokens = line.trim().split("\\s+");
    if (tokens.length < 2) {
      return -1;
    }
    try {
      return Long.parseLong(tokens[1]);
    } catch (NumberFormatException ex) {
      return -1;
    }
  }

  /**
   * Checks whether the judger's identity information is valid. Besides the periodic heartbeat, this
   * is also invoked once at startup so the judger fails fast on bad credentials before accepting any
   * judging task.
   *
   * @return whether the judger's identity information is valid
   */
  public boolean isIdentityValid() {
    User user = userMapper.getUserUsingUsername(judgerUsername);

    if (user != null
        && "judgers".equals(user.getUserGroup().getUserGroupSlug())
        && passwordEncoder.matches(judgerPassword, user.getPassword())) {
      return true;
    }
    return false;
  }

  /**
   * Gets the judger's description. TODO use JNI to obtain the computer's hardware information.
   *
   * @return the judger's description
   */
  private String getDescription() {
    return judgerDescription;
  }

  /** The judger's identity information: the judger's username. */
  @Value("${judger.username}")
  private String judgerUsername;

  /** The judger's identity information: the judger's password. */
  @Value("${judger.password}")
  private String judgerPassword;

  /** The judger's identity information: the judger's description. */
  @Value("${judger.description}")
  private String judgerDescription;

  /** The autowired MessageSender object, used to send messages to the message queue. */
  @Autowired private MessageSender messageSender;

  /** The autowired UserMapper object, used to verify the judger's identity information. */
  @Autowired private UserMapper userMapper;

  /** The autowired password encoder, used to verify the judger's password against the stored hash. */
  @Autowired private PasswordEncoder passwordEncoder;

  /** The window (in milliseconds) over which the CPU load is sampled for each heartbeat. */
  private static final long CPU_SAMPLE_INTERVAL = 500;

  /** The logger. */
  private static final Logger LOGGER = LogManager.getLogger(ApplicationHeartbeat.class);
}
