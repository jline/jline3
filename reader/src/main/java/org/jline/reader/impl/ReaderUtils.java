/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.reader.impl;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.UserPrincipal;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.jline.reader.LineReader;
import org.jline.utils.Levenshtein;

/**
 * Utility methods for LineReader implementations.
 * <p>
 * This class provides helper methods for working with LineReader variables and options.
 * It includes methods for retrieving variables of different types (string, boolean, integer,
 * long) with default values, checking if options are set, and calculating string distances
 * for completion matching.
 * <p>
 * These utilities are primarily used by the LineReader implementation classes to access
 * configuration values in a consistent way, with proper type conversion and default handling.
 *
 * @see LineReader#getVariable(String)
 * @see LineReader#isSet(LineReader.Option)
 */
public class ReaderUtils {

    private ReaderUtils() {}

    /**
     * Checks if a LineReader option is set.
     * <p>
     * This method safely handles null readers by returning false.
     *
     * @param reader the LineReader to check, may be null
     * @param option the option to check
     * @return true if the reader is not null and the option is set, false otherwise
     */
    public static boolean isSet(LineReader reader, LineReader.Option option) {
        return reader != null && reader.isSet(option);
    }

    /**
     * Gets a string variable from a LineReader with a default value.
     * <p>
     * This method safely handles null readers by returning the default value.
     *
     * @param reader the LineReader to get the variable from, may be null
     * @param name the name of the variable to get
     * @param def the default value to return if the variable is not set or the reader is null
     * @return the variable value as a string, or the default value
     */
    public static String getString(LineReader reader, String name, String def) {
        Object v = reader != null ? reader.getVariable(name) : null;
        return v != null ? v.toString() : def;
    }

    /**
     * Gets a boolean variable from a LineReader with a default value.
     * <p>
     * This method safely handles null readers by returning the default value.
     * String values are converted to boolean according to these rules:
     * <ul>
     *   <li>Empty string, "on", "1", and "true" (case-insensitive) are considered true</li>
     *   <li>All other strings are considered false</li>
     * </ul>
     *
     * @param reader the LineReader to get the variable from, may be null
     * @param name the name of the variable to get
     * @param def the default value to return if the variable is not set or the reader is null
     * @return the variable value as a boolean, or the default value
     */
    public static boolean getBoolean(LineReader reader, String name, boolean def) {
        Object v = reader != null ? reader.getVariable(name) : null;
        if (v instanceof Boolean) {
            return (Boolean) v;
        } else if (v != null) {
            String s = v.toString();
            return s.isEmpty() || s.equalsIgnoreCase("on") || s.equalsIgnoreCase("1") || s.equalsIgnoreCase("true");
        }
        return def;
    }

    /**
     * Gets an integer variable from a LineReader with a default value.
     * <p>
     * This method safely handles null readers by returning the default value.
     * String values are parsed as integers, with a fallback to 0 if parsing fails.
     *
     * @param reader the LineReader to get the variable from, may be null
     * @param name the name of the variable to get
     * @param def the default value to return if the variable is not set or the reader is null
     * @return the variable value as an integer, or the default value
     */
    public static int getInt(LineReader reader, String name, int def) {
        int nb = def;
        Object v = reader != null ? reader.getVariable(name) : null;
        if (v instanceof Number) {
            return ((Number) v).intValue();
        } else if (v != null) {
            nb = 0;
            try {
                nb = Integer.parseInt(v.toString());
            } catch (NumberFormatException e) {
                // Ignore
            }
        }
        return nb;
    }

    /**
     * Gets a long variable from a LineReader with a default value.
     * <p>
     * This method safely handles null readers by returning the default value.
     * String values are parsed as longs, with a fallback to 0 if parsing fails.
     *
     * @param reader the LineReader to get the variable from, may be null
     * @param name the name of the variable to get
     * @param def the default value to return if the variable is not set or the reader is null
     * @return the variable value as a long, or the default value
     */
    public static long getLong(LineReader reader, String name, long def) {
        long nb = def;
        Object v = reader != null ? reader.getVariable(name) : null;
        if (v instanceof Number) {
            return ((Number) v).longValue();
        } else if (v != null) {
            nb = 0;
            try {
                nb = Long.parseLong(v.toString());
            } catch (NumberFormatException e) {
                // Ignore
            }
        }
        return nb;
    }

    /**
     * Creates a temporary file with owner-only read/write permissions.
     * <p>
     * On POSIX file systems the file is created with {@code rw-------} permissions (0600).
     * The effective permissions are verified after creation; if they do not match, the file
     * is deleted and an {@link IOException} is thrown.
     * <p>
     * On non-POSIX file systems (e.g. Windows NTFS), the file is created with an ACL that
     * grants read, write, and delete access to the current user only. The ACL is applied
     * atomically at creation time, eliminating any TOCTOU race between creation and restriction.
     *
     * @param prefix the prefix for the temporary file name
     * @param suffix the suffix for the temporary file name (may be null)
     * @return the created temporary {@link File}
     * @throws IOException if the file cannot be created or if the permissions cannot be restricted
     */
    @SuppressWarnings("java:S5443") // temp file is created with explicit owner-only permissions
    public static File createSecureTempFile(String prefix, String suffix) throws IOException {
        Path tmp;
        try {
            FileAttribute<?> ownerOnly =
                    PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------"));
            tmp = Files.createTempFile(prefix, suffix, ownerOnly);
        } catch (UnsupportedOperationException e) {
            // Non-POSIX file system (e.g. Windows): create with owner-only ACL at creation time
            // to eliminate the TOCTOU race between file creation and permission restriction.
            try {
                UserPrincipal owner = FileSystems.getDefault()
                        .getUserPrincipalLookupService()
                        .lookupPrincipalByName(System.getProperty("user.name"));
                AclEntry ownerOnly = AclEntry.newBuilder()
                        .setType(AclEntryType.ALLOW)
                        .setPrincipal(owner)
                        .setPermissions(
                                AclEntryPermission.READ_DATA,
                                AclEntryPermission.WRITE_DATA,
                                AclEntryPermission.APPEND_DATA,
                                AclEntryPermission.READ_ATTRIBUTES,
                                AclEntryPermission.WRITE_ATTRIBUTES,
                                AclEntryPermission.READ_NAMED_ATTRS,
                                AclEntryPermission.WRITE_NAMED_ATTRS,
                                AclEntryPermission.READ_ACL,
                                AclEntryPermission.SYNCHRONIZE,
                                AclEntryPermission.DELETE)
                        .build();
                FileAttribute<List<AclEntry>> aclAttr = new FileAttribute<>() {
                    @Override
                    public String name() {
                        return "acl:acl";
                    }

                    @Override
                    public List<AclEntry> value() {
                        return Collections.singletonList(ownerOnly);
                    }
                };
                return Files.createTempFile(prefix, suffix, aclAttr).toFile();
            } catch (UnsupportedOperationException uoe) {
                throw new IOException(
                        "Cannot create secure temporary file: filesystem supports neither POSIX nor ACL attributes",
                        uoe);
            }
        }
        return verifyPosixPermissions(tmp);
    }

    private static File verifyPosixPermissions(Path tmp) throws IOException {
        try {
            Set<PosixFilePermission> actual = Files.getPosixFilePermissions(tmp);
            if (!actual.equals(PosixFilePermissions.fromString("rw-------"))) {
                throw new IOException("Temporary file " + tmp + " has unsafe permissions: " + actual);
            }
            return tmp.toFile();
        } catch (IOException ex) {
            Files.deleteIfExists(tmp);
            throw ex;
        }
    }

    /**
     * Calculates the edit distance between a word and a candidate string.
     * <p>
     * This method is used for fuzzy matching in completion. It uses the Levenshtein
     * distance algorithm to determine how similar two strings are, with special handling
     * for candidates that are longer than the word being matched.
     *
     * @param word the word to match against
     * @param cand the candidate string to check
     * @return the edit distance between the strings (lower values indicate closer matches)
     */
    public static int distance(String word, String cand) {
        if (word.length() < cand.length()) {
            int d1 = Levenshtein.distance(word, cand.substring(0, Math.min(cand.length(), word.length())));
            int d2 = Levenshtein.distance(word, cand);
            return Math.min(d1, d2);
        } else {
            return Levenshtein.distance(word, cand);
        }
    }
}
