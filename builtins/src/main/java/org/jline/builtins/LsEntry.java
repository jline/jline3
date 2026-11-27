/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.builtins;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import org.jline.utils.OSUtils;

/**
 * A file or directory listed by the {@code ls} command, together with its attributes and the
 * {@code ls} options that control how it is sorted and displayed.
 *
 * @see PosixCommands#ls(PosixCommands.Context, String[])
 */
final class LsEntry implements Comparable<LsEntry> {
    private static final String[] SIZE_UNITS = {"B", "K", "M", "G", "T", "P"};
    private static final long SIZE_THRESHOLD = 1000L;
    private static final LinkOption[] EMPTY_LINK_OPTIONS = new LinkOption[0];
    private static final LinkOption[] NO_FOLLOW_OPTIONS = new LinkOption[] {LinkOption.NOFOLLOW_LINKS};
    private static final List<String> WINDOWS_EXECUTABLE_EXTENSIONS = List.of(".bat", ".exe", ".cmd");

    final Path abs;
    final Path path;
    final Map<String, Object> attributes;
    private final Options opt;
    private final Map<String, String> colors;

    /**
     * @param abs the path to list
     * @param root the directory that {@link #path} is displayed relative to
     * @param opt the parsed {@code ls} options
     * @param colors the {@code LS_COLORS} style map, empty when output is not colored
     */
    LsEntry(Path abs, Path root, Options opt, Map<String, String> colors) {
        this.abs = abs;
        this.opt = opt;
        this.colors = colors;
        try {
            this.path = Files.isSameFile(abs, root) ? Path.of(".") : abs.startsWith(root) ? root.relativize(abs) : abs;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.attributes = readAttributes(abs);
    }

    @Override
    public int compareTo(LsEntry o) {
        int c = doCompare(o);
        return opt.isSet("r") ? -c : c;
    }

    private int doCompare(LsEntry o) {
        if (opt.isSet("f")) {
            return -1;
        }
        if (opt.isSet("S")) {
            long size = attributes.get("size") != null ? ((Number) attributes.get("size")).longValue() : 0L;
            long otherSize = o.attributes.get("size") != null ? ((Number) o.attributes.get("size")).longValue() : 0L;
            return size > otherSize
                    ? -1
                    : size < otherSize ? 1 : path.toString().compareTo(o.path.toString());
        }
        if (opt.isSet("t")) {
            long modified = attributes.get("lastModifiedTime") != null
                    ? ((FileTime) attributes.get("lastModifiedTime")).toMillis()
                    : 0L;
            long otherModified = o.attributes.get("lastModifiedTime") != null
                    ? ((FileTime) o.attributes.get("lastModifiedTime")).toMillis()
                    : 0L;
            return modified > otherModified
                    ? -1
                    : modified < otherModified ? 1 : path.toString().compareTo(o.path.toString());
        }
        return path.toString().compareTo(o.path.toString());
    }

    boolean isNotDirectory() {
        return is("isRegularFile") || is("isSymbolicLink") || is("isOther");
    }

    boolean isDirectory() {
        return is("isDirectory");
    }

    private boolean is(String attr) {
        Object d = attributes.get(attr);
        return d instanceof Boolean && (Boolean) d;
    }

    String display() {
        String type;
        String suffix;
        String link = "";
        if (is("isSymbolicLink")) {
            type = "sl";
            suffix = "@";
            try {
                Path linkTarget = Files.readSymbolicLink(abs);
                link = " -> " + PosixCommands.stripControlChars(linkTarget.toString());
            } catch (IOException e) {
                // ignore
            }
        } else if (is("isDirectory")) {
            type = "dr";
            suffix = "/";
        } else if (is("isExecutable")) {
            type = "ex";
            suffix = "*";
        } else if (is("isOther")) {
            type = "ot";
            suffix = "";
        } else {
            type = "";
            suffix = "";
        }
        boolean addSuffix = opt.isSet("F");
        String name = PosixCommands.stripControlChars(path.toString());
        return PosixCommands.applyStyle(name, colors, type) + (addSuffix ? suffix : "") + link;
    }

    String longDisplay() {
        String username = Objects.toString(attributes.get("owner"), "owner");
        String group = Objects.toString(attributes.get("group"), "group");
        String size = formatSize();

        @SuppressWarnings("unchecked")
        Set<PosixFilePermission> perms = (Set<PosixFilePermission>) attributes.get("permissions");
        String permissions =
                PosixFilePermissions.toString(perms != null ? perms : EnumSet.noneOf(PosixFilePermission.class));

        String fileType = is("isDirectory") ? "d" : (is("isSymbolicLink") ? "l" : (is("isOther") ? "o" : "-"));
        String linkCount = Objects.toString(attributes.get("nlink"), "1");
        String modTime = toString((FileTime) attributes.get("lastModifiedTime"));
        String fileName = display();

        return String.format(
                "%s%s %3s %-8.8s %-8.8s %8s %s %s",
                fileType, permissions, linkCount, username, group, size, modTime, fileName);
    }

    private String formatSize() {
        Number length = (Number) attributes.get("size");
        if (length == null) {
            length = 0L;
        }
        if (opt.isSet("h")) {
            long bytes = length.longValue();
            if (bytes < SIZE_THRESHOLD) {
                return bytes + "B";
            }
            double size = bytes;
            int unitIndex = 0;
            while (size >= SIZE_THRESHOLD && unitIndex < SIZE_UNITS.length - 1) {
                size /= 1024;
                unitIndex++;
            }
            String format = (size < 10 && bytes > SIZE_THRESHOLD) ? "%.1f" : "%.0f";
            return String.format(format, size) + SIZE_UNITS[unitIndex];
        } else {
            return length.toString();
        }
    }

    private String toString(FileTime time) {
        long millis = (time != null) ? time.toMillis() : -1L;
        if (millis < 0L) {
            return "------------";
        }
        ZonedDateTime dt = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault());
        long sixMonthsInMillis = TimeUnit.DAYS.toMillis(183);
        boolean modifiedWithinSixMonths = System.currentTimeMillis() - millis < sixMonthsInMillis;
        String pattern = modifiedWithinSixMonths ? "MMM ppd HH:mm" : "MMM ppd  yyyy";
        return DateTimeFormatter.ofPattern(pattern).format(dt);
    }

    private Map<String, Object> readAttributes(Path path) {
        Map<String, Object> attrs = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (String view : path.getFileSystem().supportedFileAttributeViews()) {
            try {
                Map<String, Object> viewAttributes =
                        Files.readAttributes(path, view + ":*", getLinkOptions(opt.isSet("L")));
                viewAttributes.forEach(attrs::putIfAbsent);
            } catch (IOException e) {
                // Ignore
            }
        }
        attrs.computeIfAbsent("isExecutable", s -> Files.isExecutable(path));
        attrs.computeIfAbsent("permissions", s -> getPermissionsFromFile(path));
        return attrs;
    }

    /**
     * Get link options based on whether to follow links.
     */
    private static LinkOption[] getLinkOptions(boolean followLinks) {
        if (followLinks) {
            return EMPTY_LINK_OPTIONS;
        } else { // return a clone that modifications to the array will not affect others
            return NO_FOLLOW_OPTIONS.clone();
        }
    }

    /**
     * Check if a file name is a Windows executable.
     */
    private static boolean isWindowsExecutable(String fileName) {
        if ((fileName == null) || (fileName.length() <= 0)) {
            return false;
        }
        for (String suffix : WINDOWS_EXECUTABLE_EXTENSIONS) {
            if (fileName.endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get POSIX file permissions from a file, with Windows executable handling.
     */
    private static Set<PosixFilePermission> getPermissionsFromFile(Path f) {
        Set<PosixFilePermission> perms = new HashSet<>();
        try {
            perms = Files.getPosixFilePermissions(f);
        } catch (IOException | UnsupportedOperationException ignore) {
        }
        if (OSUtils.IS_WINDOWS && isWindowsExecutable(f.getFileName().toString())) {
            perms.add(PosixFilePermission.OWNER_EXECUTE);
            perms.add(PosixFilePermission.GROUP_EXECUTE);
            perms.add(PosixFilePermission.OTHERS_EXECUTE);
        }
        return perms;
    }
}
