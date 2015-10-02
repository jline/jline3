/*
 * Copyright (c) 2002-2015, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.reader;

/**
 * List of all operations.
 *
 * @author <a href="mailto:gnodet@gmail.com">Guillaume Nodet</a>
 * @since 2.6
 */
public class Operation {

    public static final String ABORT = "abort";
    public static final String ACCEPT_LINE = "accept-line";
    public static final String BACKWARD_BYTE = "backward-byte";
    public static final String BACKWARD_CHAR = "backward-char";
    public static final String BACKWARD_DELETE_CHAR = "backward-delete-char";
    public static final String BACKWARD_KILL_LINE = "backward-kill-line";
    public static final String BACKWARD_KILL_WORD = "backward-kill-word";
    public static final String BACKWARD_WORD = "backward-word";
    public static final String BEGINNING_OF_HISTORY = "beginning-of-history";
    public static final String BEGINNING_OF_LINE = "beginning-of-line";
    public static final String CALL_LAST_KBD_MACRO = "call-last-kbd-macro";
    public static final String CAPITALIZE_WORD = "capitalize-word";
    public static final String CHARACTER_SEARCH = "character-search";
    public static final String CHARACTER_SEARCH_BACKWARD = "character-search-backward";
    public static final String CLEAR_SCREEN = "clear-screen";
    public static final String COMPLETE_PREFIX = "complete-prefix";
    public static final String COMPLETE_WORD = "complete-word";
    public static final String COPY_BACKWARD_WORD = "copy-backward-word";
    public static final String COPY_FORWARD_WORD = "copy-forward-word";
    public static final String COPY_REGION_AS_KILL = "copy-region-as-kill";
    public static final String DELETE_CHAR = "delete-char";
    public static final String DELETE_CHAR_OR_LIST = "delete-char-or-list";
    public static final String DELETE_HORIZONTAL_SPACE = "delete-horizontal-space";
    public static final String DIGIT_ARGUMENT = "digit-argument";
    public static final String DO_LOWERCASE_VERSION = "do-lowercase-version";
    public static final String DOWN_LINE_OR_HISTORY = "down-line-or-history";
    public static final String DOWNCASE_WORD = "downcase-word";
    public static final String EMACS_EDITING_MODE = "emacs-editing-mode";
    public static final String END_KBD_MACRO = "end-kbd-macro";
    public static final String END_OF_HISTORY = "end-of-history";
    public static final String END_OF_LINE = "end-of-line";
    public static final String EXCHANGE_POINT_AND_MARK = "exchange-point-and-mark";
    public static final String EXIT_OR_DELETE_CHAR = "exit-or-delete-char";
    public static final String FORWARD_CHAR = "forward-char";
    public static final String FORWARD_SEARCH_HISTORY = "forward-search-history";
    public static final String FORWARD_WORD = "forward-word";
    public static final String HISTORY_SEARCH_BACKWARD = "history-search-backward";
    public static final String HISTORY_SEARCH_FORWARD = "history-search-forward";
    public static final String INSERT_CLOSE_CURLY = "insert-close-curly";
    public static final String INSERT_CLOSE_PAREN = "insert-close-paren";
    public static final String INSERT_CLOSE_SQUARE = "insert-close-square";
    public static final String INSERT_COMMENT = "insert-comment";
    public static final String INSERT_COMPLETIONS = "insert-completions";
    public static final String INTERRUPT = "interrupt";
    public static final String KILL_WHOLE_LINE = "kill-whole-line";
    public static final String KILL_LINE = "kill-line";
    public static final String KILL_REGION = "kill-region";
    public static final String KILL_WORD = "kill-word";
    public static final String MENU_COMPLETE = "menu-complete";
    public static final String NEXT_HISTORY = "next-history";
    public static final String NON_INCREMENTAL_FORWARD_SEARCH_HISTORY = "non-incremental-forward-search-history";
    public static final String NON_INCREMENTAL_REVERSE_SEARCH_HISTORY = "non-incremental-reverse-search-history";
    public static final String NON_INCREMENTAL_FORWARD_SEARCH_HISTORY_AGAIN = "non-incremental-forward-search-history-again";
    public static final String NON_INCREMENTAL_REVERSE_SEARCH_HISTORY_AGAIN = "non-incremental-reverse-search-history-again";
    public static final String OLD_MENU_COMPLETE = "old-menu-complete";
    public static final String OVERWRITE_MODE = "overwrite-mode";
    public static final String PASTE_FROM_CLIPBOARD = "paste-from-clipboard";
    public static final String POSSIBLE_COMPLETIONS = "possible-completions";
    public static final String PREVIOUS_HISTORY = "previous-history";
    public static final String QUOTED_INSERT = "quoted-insert";
    public static final String QUIT = "quit";
    public static final String REVERSE_MENU_COMPLETE = "reverse-menu-complete";
    public static final String REVERSE_SEARCH_HISTORY = "reverse-search-history";
    public static final String REVERT_LINE = "revert-line";
    public static final String SELF_INSERT = "self-insert";
    public static final String SELF_INSERT_UNMETA = "self-insert-unmeta";
    public static final String SET_MARK = "set-mark";
    public static final String START_KBD_MACRO = "start-kbd-macro";
    public static final String TAB_INSERT = "tab-insert";
    public static final String TILDE_EXPAND = "tilde-expand";
    public static final String TRANSPOSE_CHARS = "transpose-chars";
    public static final String TRANSPOSE_WORDS = "transpose-words";
    public static final String TTY_STATUS = "tty-status";
    public static final String UNDO = "undo";
    public static final String UNIVERSAL_ARGUMENT = "universal-argument";
    public static final String UNIX_FILENAME_RUBOUT = "unix-filename-rubout";
    public static final String UNIX_LINE_DISCARD = "unix-line-discard";
    public static final String UNIX_WORD_RUBOUT = "unix-word-rubout";
    public static final String UP_LINE_OR_HISTORY = "up-line-or-history";
    public static final String UPCASE_WORD = "upcase-word";
    public static final String YANK = "yank";
    public static final String YANK_LAST_ARG = "yank-last-arg";
    public static final String YANK_NTH_ARG = "yank-nth-arg";
    public static final String YANK_POP = "yank-pop";
    public static final String VI_ADD_EOL = "vi-add-eol";
    public static final String VI_ADD_NEXT = "vi-add-next";
    public static final String VI_ARG_DIGIT = "vi-arg-digit";
    public static final String VI_BACK_TO_INDENT = "vi-back-to-indent";
    public static final String VI_BACKWARD_BIGWORD = "vi-backward-bigword";
    public static final String VI_BACKWARD_WORD = "vi-backward-word";
    public static final String VI_BWORD = "vi-bword";
    public static final String VI_CHANGE_CASE = "vi-change-case";
    public static final String VI_CHANGE_CHAR = "vi-change-char";
    public static final String VI_CHANGE_TO = "vi-change-to";
    public static final String VI_CHANGE_TO_EOL = "vi-change-to-eol";
    public static final String VI_CHAR_SEARCH = "vi-char-search";
    public static final String VI_COLUMN = "vi-column";
    public static final String VI_COMPLETE = "vi-complete";
    public static final String VI_DELETE = "vi-delete";
    public static final String VI_DELETE_TO = "vi-delete-to";
    public static final String VI_DELETE_TO_EOL = "vi-delete-to-eol";
    public static final String VI_END_BIGWORD = "vi-end-bigword";
    public static final String VI_END_WORD = "vi-end-word";
    public static final String VI_EOF_MAYBE = "vi-eof-maybe";
    public static final String VI_EWORD = "vi-eword";
    public static final String VI_FWORD = "vi-fword";
    public static final String VI_FETCH_HISTORY = "vi-fetch-history";
    public static final String VI_FIRST_PRINT = "vi-first-print";
    public static final String VI_FORWARD_BIGWORD = "vi-forward-bigword";
    public static final String VI_FORWARD_WORD = "vi-forward-word";
    public static final String VI_GOTO_MARK = "vi-goto-mark";
    public static final String VI_INSERT_BOL = "vi-insert-bol";
    public static final String VI_INSERT = "vi-insert";
    public static final String VI_KILL_WHOLE_LINE = "vi-kill-whole-line";
    public static final String VI_MATCH = "vi-match";
    public static final String VI_CMD_MODE = "vi-cmd-mode";
    public static final String VI_NEXT_WORD = "vi-next-word";
    public static final String VI_OVERSTRIKE = "vi-overstrike";
    public static final String VI_OVERSTRIKE_DELETE = "vi-overstrike-delete";
    public static final String VI_PREV_WORD = "vi-prev-word";
    public static final String VI_PUT = "vi-put";
    public static final String VI_REDO = "vi-redo";
    public static final String VI_REPLACE = "vi-replace";
    public static final String VI_RUBOUT = "vi-rubout";
    public static final String VI_SEARCH = "vi-search";
    public static final String VI_SEARCH_AGAIN = "vi-search-again";
    public static final String VI_SET_MARK = "vi-set-mark";
    public static final String VI_SUBST = "vi-subst";
    public static final String VI_TILDE_EXPAND = "vi-tilde-expand";
    public static final String VI_YANK_ARG = "vi-yank-arg";
    public static final String VI_YANK_TO = "vi-yank-to";
    public static final String VI_MOVE_ACCEPT_LINE = "vi-move-accept-line";
    public static final String VI_NEXT_HISTORY = "vi-next-history";
    public static final String VI_PREVIOUS_HISTORY = "vi-previous-history";
    public static final String VI_INSERT_COMMENT = "vi-insert-comment";
    public static final String VI_BEGINNING_OF_LINE_OR_ARG_DIGIT = "vi-beginning-of-line-or-arg-digit";

}
